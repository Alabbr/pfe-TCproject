package org.example.gestionrh.tcproject.Services;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.response.DocumentInboxResponse;
import org.example.gestionrh.tcproject.Dtos.response.DocumentResponse;
import org.example.gestionrh.tcproject.Dtos.response.DocumentStatsResponse;
import org.example.gestionrh.tcproject.Dtos.response.UserResponse;
import org.example.gestionrh.tcproject.Entities.Document;
import org.example.gestionrh.tcproject.Entities.DocumentRecipient;
import org.example.gestionrh.tcproject.Entities.RoleName;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Entities.ValidationStatus;
import org.example.gestionrh.tcproject.Entities.InterimDelegation;
import org.example.gestionrh.tcproject.Repositories.DocumentRecipientRepository;
import org.example.gestionrh.tcproject.Repositories.DocumentRepository;
import org.example.gestionrh.tcproject.Repositories.UserRepository;
import org.example.gestionrh.tcproject.Repositories.InterimDelegationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentRecipientRepository documentRecipientRepository;
    private final UserRepository userRepository;
    private final InterimDelegationRepository interimDelegationRepository;
    private final FileStorageService fileStorageService;
    private final org.example.gestionrh.tcproject.Repositories.DocumentRequestRepository documentRequestRepository;

    // Upload un document : sauvegarde le fichier, crée l'entité Document et les destinataires avec leur statut de validation
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String title, String description, boolean isPublic, List<Long> recipientIds, String uploaderEmail, Long targetDepartmentId, boolean skipValidation, boolean needsValidation) {
        User uploader = userRepository.findByEmail(uploaderEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String fileName = fileStorageService.storeFile(file);

        Document document = Document.builder()
                .title(title)
                .description(description)
                .fileUrl(fileName)
                .originalFileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .isPublic(isPublic)
                .uploader(uploader)
                .build();
                
        if (targetDepartmentId != null) {
            document.setTargetDepartment(org.example.gestionrh.tcproject.Entities.Department.builder().id(targetDepartmentId).build());
            document.setDepartmentPublic(true);
        }

        document = documentRepository.save(document);

        if (!isPublic && recipientIds != null && !recipientIds.isEmpty()) {
            List<User> recipients = userRepository.findAllById(recipientIds);
            for (User recipient : recipients) {
                ValidationStatus status = ValidationStatus.NOT_REQUIRED;
                
                // Si l'employé envoie à un autre département ou à un chef, ça passe en PENDING.
                // NOUVEAU: Si l'expéditeur a explicitement coché "needsValidation", on force PENDING.
                if (needsValidation) {
                    status = ValidationStatus.PENDING;
                }

                DocumentRecipient docRecipient = DocumentRecipient.builder()
                        .document(document)
                        .recipient(recipient)
                        .isRead(false)
                        .validationStatus(status)
                        .build();
                documentRecipientRepository.save(docRecipient);
            }
        }

        return mapToResponse(document);
    }

    // Retourne tous les documents publics, triés par date de téléchargement décroissante
    public List<DocumentResponse> getPublicDocuments() {
        return documentRepository.findByIsPublicTrueOrderByUploadDateDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Retourne les documents publics ciblant un département spécifique
    public List<DocumentResponse> getDepartmentPublicDocs(Long departmentId) {
        return documentRepository.findByTargetDepartmentIdAndIsDepartmentPublicTrueOrderByUploadDateDesc(departmentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Retourne la boîte de réception de l'utilisateur (documents reçus validés ou sans validation requise)
    public List<DocumentInboxResponse> getMyInbox(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return documentRecipientRepository.findByRecipientIdAndValidationStatusInOrderByDocumentUploadDateDesc(
                        user.getId(), List.of(ValidationStatus.NOT_REQUIRED, ValidationStatus.VALIDATED))
                .stream()
                .map(this::mapToInboxResponse)
                .collect(Collectors.toList());
    }

    // Retourne les documents en attente de validation pour le chef de département ou son intérimaire
    public List<DocumentInboxResponse> getPendingValidations(String chefEmail) {
        User user = userRepository.findByEmail(chefEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isChef = (user.getRole() == RoleName.RESPONSABLE || user.getRole() == RoleName.DIRECTEUR || user.getRole() == RoleName.DIRECTEUR_GENERAL);
        
        List<InterimDelegation> interims = interimDelegationRepository.findActiveDelegationsForInterim(user.getId(), java.time.LocalDate.now());
        boolean isInterim = !interims.isEmpty();

        if (!isChef && !isInterim) {
            throw new RuntimeException("Only Chefs or designated Interims can access pending validations.");
        }

        Long deptId = null;
        if (isChef) {
            deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        } else if (isInterim) {
            deptId = interims.get(0).getDelegator().getDepartment() != null ? interims.get(0).getDelegator().getDepartment().getId() : null;
        }

        if (deptId == null) {
            return List.of();
        }

        return documentRecipientRepository.findByDocumentUploaderDepartmentIdAndValidationStatusOrderByDocumentUploadDateDesc(
                        deptId, ValidationStatus.PENDING)
                .stream()
                .map(this::mapToInboxResponse)
                .collect(Collectors.toList());
    }

    // Valide un document en attente (change le statut à VALIDATED et enregistre le validateur)
    @Transactional
    public void validateDocument(Long recipientId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DocumentRecipient rec = documentRecipientRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient record not found"));

        rec.setValidationStatus(ValidationStatus.VALIDATED);
        rec.setValidator(user);
        rec.setValidatedAt(LocalDateTime.now());
        documentRecipientRepository.save(rec);
    }

    // Rejette un document en attente (change le statut à REJECTED et enregistre le validateur)
    @Transactional
    public void rejectDocument(Long recipientId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DocumentRecipient rec = documentRecipientRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient record not found"));

        rec.setValidationStatus(ValidationStatus.REJECTED);
        rec.setValidator(user);
        rec.setValidatedAt(LocalDateTime.now());
        documentRecipientRepository.save(rec);
    }

    // Retourne tous les documents envoyés par l'utilisateur avec leur statut de validation
    public List<DocumentInboxResponse> getMySentDocs(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return documentRecipientRepository.findByDocumentUploaderIdOrderByDocumentUploadDateDesc(user.getId())
                .stream()
                .map(this::mapToInboxResponse)
                .collect(Collectors.toList());
    }

    // Retourne l'historique global des envois (Admin voit tout, Chef voit son département)
    public List<DocumentInboxResponse> getUploadHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<DocumentRecipient> recipients;
        
        if (user.getRole() == RoleName.SUPER_ADMIN || user.getRole() == RoleName.DIRECTEUR_GENERAL) {
            // Admin ou DG voient tout l'historique
            recipients = documentRecipientRepository.findAll();
        } else if (user.getRole() == RoleName.DIRECTEUR || user.getRole() == RoleName.RESPONSABLE) {
            // Le Chef voit uniquement l'historique de son propre département
            if (user.getDepartment() == null) {
                return List.of();
            }
                
            // Fetch all and filter by uploader's department id
            recipients = documentRecipientRepository.findAll().stream()
                    .filter(r -> r.getDocument().getUploader().getDepartment() != null &&
                            r.getDocument().getUploader().getDepartment().getId().equals(user.getDepartment().getId()))
                    .collect(Collectors.toList());
        } else {
            throw new RuntimeException("Unauthorized to view upload history");
        }
        
        // Sort by date descending
        recipients.sort((a, b) -> b.getDocument().getUploadDate().compareTo(a.getDocument().getUploadDate()));
        
        return recipients.stream().map(this::mapToInboxResponse).collect(Collectors.toList());
    }

    // Marque un document comme lu par le destinataire (vérifie les droits)
    @Transactional
    public void markAsRead(Long documentRecipientId, String userEmail) {
        DocumentRecipient rec = documentRecipientRepository.findById(documentRecipientId)
                .orElseThrow(() -> new RuntimeException("Recipient record not found"));
        
        if (!rec.getRecipient().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized");
        }

        if (!rec.isRead()) {
            rec.setRead(true);
            rec.setReadAt(LocalDateTime.now());
            documentRecipientRepository.save(rec);
        }
    }

    // Calcule les statistiques du tableau de bord pour un utilisateur (envoyés, reçus, non lus, en attente)
    public DocumentStatsResponse getStats(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        long totalSent = documentRepository.countByUploaderId(user.getId());
        long totalReceived = documentRecipientRepository.findByRecipientIdAndValidationStatusInOrderByDocumentUploadDateDesc(
                user.getId(), List.of(ValidationStatus.NOT_REQUIRED, ValidationStatus.VALIDATED)).size();
        
        long unreadCount = documentRecipientRepository.countByRecipientIdAndIsReadFalseAndValidationStatusIn(
                user.getId(), List.of(ValidationStatus.NOT_REQUIRED, ValidationStatus.VALIDATED));
        long publicDocsCount = documentRepository.countPublicDocuments();

        long pendingCount = 0;
        long pendingRequestsCount = 0;
        long assignedTasksCount = 0;

        if (user.getRole() == RoleName.RESPONSABLE || user.getRole() == RoleName.DIRECTEUR || user.getRole() == RoleName.DIRECTEUR_GENERAL) {
            if (user.getDepartment() != null) {
                pendingCount = documentRecipientRepository.findByDocumentUploaderDepartmentIdAndValidationStatusOrderByDocumentUploadDateDesc(
                        user.getDepartment().getId(), ValidationStatus.PENDING).size();
                pendingRequestsCount = documentRequestRepository.findByTargetDepartmentIdOrderByCreatedAtDesc(user.getDepartment().getId())
                        .stream().filter(r -> r.getStatus().equals("PENDING")).count();
            }
        }

        if (user.getRole() == RoleName.EMPLOYE) {
            assignedTasksCount = documentRequestRepository.findAll().stream()
                    .filter(r -> (r.getAssignedTo() != null && r.getAssignedTo().getId().equals(user.getId()) && r.getStatus().equals("ASSIGNED"))
                              || (r.getDelegatedTo() != null && r.getDelegatedTo().getId().equals(user.getId()) && r.getStatus().equals("DELEGATED"))).count();
        }

        return DocumentStatsResponse.builder()
                .totalSent(totalSent)
                .totalReceived(totalReceived)
                .unreadCount(unreadCount)
                .publicDocsCount(publicDocsCount)
                .pendingCount(pendingCount)
                .pendingRequestsCount(pendingRequestsCount)
                .assignedTasksCount(assignedTasksCount)
                .build();
    }

    // Retourne le nombre de documents en attente de validation pour le badge du chef
    public long getPendingValidationCount(String chefEmail) {
        User user = userRepository.findByEmail(chefEmail).orElse(null);
        if (user == null) return 0;
        
        boolean isChef = (user.getRole() == RoleName.RESPONSABLE || user.getRole() == RoleName.DIRECTEUR || user.getRole() == RoleName.DIRECTEUR_GENERAL);
        List<InterimDelegation> interims = interimDelegationRepository.findActiveDelegationsForInterim(user.getId(), java.time.LocalDate.now());
        boolean isInterim = !interims.isEmpty();

        Long deptId = null;
        if (isChef) {
            deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
        } else if (isInterim) {
            deptId = interims.get(0).getDelegator().getDepartment() != null ? interims.get(0).getDelegator().getDepartment().getId() : null;
        }

        if (deptId != null) {
            return documentRecipientRepository.findByDocumentUploaderDepartmentIdAndValidationStatusOrderByDocumentUploadDateDesc(
                    deptId, ValidationStatus.PENDING).size();
        }
        return 0;
    }

    // Récupère le chemin du fichier physique d'un document par son ID
    public Path getDocumentFile(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return fileStorageService.getFilePath(document.getFileUrl());
    }

    // Récupère l'entité Document complète par son ID
    public Document getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    // Convertit un DocumentRecipient en DTO pour la réponse inbox
    private DocumentInboxResponse mapToInboxResponse(DocumentRecipient rec) {
        return DocumentInboxResponse.builder()
                .id(rec.getId())
                .document(mapToResponse(rec.getDocument()))
                .isRead(rec.isRead())
                .readAt(rec.getReadAt())
                .validationStatus(rec.getValidationStatus())
                .recipient(mapUserToResponse(rec.getRecipient()))
                .build();
    }

    // Convertit un Document en DTO pour la réponse API
    private DocumentResponse mapToResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .fileUrl("/api/documents/download/" + document.getId())
                .fileType(document.getFileType())
                .originalFileName(document.getOriginalFileName())
                .fileSize(document.getFileSize())
                .isPublic(document.isPublic())
                .uploadDate(document.getUploadDate())
                .uploader(mapUserToResponse(document.getUploader()))
                .build();
    }

    // Convertit un User en DTO simplifié pour l'intégrer dans les réponses
    private UserResponse mapUserToResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .jobPositionName(user.getJobPosition() != null ? user.getJobPosition().getName() : null)
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }
}
