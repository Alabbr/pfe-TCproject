package org.example.gestionrh.tcproject.Controllers;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.response.DocumentInboxResponse;
import org.example.gestionrh.tcproject.Dtos.response.DocumentResponse;
import org.example.gestionrh.tcproject.Dtos.response.DocumentStatsResponse;
import org.example.gestionrh.tcproject.Entities.Document;
import org.example.gestionrh.tcproject.Services.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // Reçoit un fichier avec ses métadonnées, l'upload et crée un Document avec ses destinataires
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("isPublic") boolean isPublic,
            @RequestParam(value = "recipientIds", required = false) List<Long> recipientIds,
            @RequestParam(value = "targetDepartmentId", required = false) Long targetDepartmentId,
            @RequestParam(value = "skipValidation", defaultValue = "false") boolean skipValidation,
            @RequestParam(value = "needsValidation", defaultValue = "false") boolean needsValidation,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        DocumentResponse response = documentService.uploadDocument(file, title, description, isPublic, recipientIds, userEmail, targetDepartmentId, skipValidation, needsValidation);
        return ResponseEntity.ok(response);
    }

    // Retourne la liste de tous les documents publics
    @GetMapping("/public")
    public ResponseEntity<List<DocumentResponse>> getPublicDocuments() {
        return ResponseEntity.ok(documentService.getPublicDocuments());
    }

    // Retourne la liste des documents publics ciblant un département spécifique
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<DocumentResponse>> getDepartmentPublicDocs(@PathVariable Long departmentId) {
        return ResponseEntity.ok(documentService.getDepartmentPublicDocs(departmentId));
    }

    // Retourne la boîte de réception (documents reçus validés) de l'utilisateur connecté
    @GetMapping("/private/me")
    public ResponseEntity<List<DocumentInboxResponse>> getMyInbox(Authentication authentication) {
        return ResponseEntity.ok(documentService.getMyInbox(authentication.getName()));
    }

    // Marque un document reçu comme lu par l'utilisateur
    @PutMapping("/private/{documentRecipientId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long documentRecipientId, Authentication authentication) {
        documentService.markAsRead(documentRecipientId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    // Retourne les statistiques d'utilisation des documents pour le tableau de bord de l'utilisateur
    @GetMapping("/stats")
    public ResponseEntity<DocumentStatsResponse> getStats(Authentication authentication) {
        return ResponseEntity.ok(documentService.getStats(authentication.getName()));
    }

    // Retourne le nombre de documents en attente de validation pour le badge du chef
    @GetMapping("/stats/pending-count")
    public ResponseEntity<Long> getPendingValidationCount(Authentication authentication) {
        return ResponseEntity.ok(documentService.getPendingValidationCount(authentication.getName()));
    }

    // Retourne les documents en attente de validation par le chef ou son intérimaire
    @GetMapping("/pending-validation")
    public ResponseEntity<List<DocumentInboxResponse>> getPendingValidations(Authentication authentication) {
        return ResponseEntity.ok(documentService.getPendingValidations(authentication.getName()));
    }

    // Valide un document en attente
    @PutMapping("/validate/{recipientId}")
    public ResponseEntity<Void> validateDocument(@PathVariable Long recipientId, Authentication authentication) {
        documentService.validateDocument(recipientId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    // Rejette un document en attente
    @PutMapping("/reject/{recipientId}")
    public ResponseEntity<Void> rejectDocument(@PathVariable Long recipientId, Authentication authentication) {
        documentService.rejectDocument(recipientId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    // Retourne la liste de tous les documents envoyés par l'utilisateur avec leur statut
    @GetMapping("/my-sent-docs")
    public ResponseEntity<List<DocumentInboxResponse>> getMySentDocs(Authentication authentication) {
        return ResponseEntity.ok(documentService.getMySentDocs(authentication.getName()));
    }

    // Retourne l'historique complet des uploads (pour admin ou chef)
    @GetMapping("/upload-history")
    public ResponseEntity<List<DocumentInboxResponse>> getUploadHistory(Authentication authentication) {
        return ResponseEntity.ok(documentService.getUploadHistory(authentication.getName()));
    }

    // Télécharge le fichier physique d'un document
    @GetMapping("/download/{documentId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        try {
            Path filePath = documentService.getDocumentFile(documentId);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() || resource.isReadable()) {
                Document document = documentService.getDocument(documentId);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(document.getFileType() != null ? document.getFileType() : "application/octet-stream"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getOriginalFileName() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Could not read file");
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Error: " + ex.getMessage());
        }
    }
}
