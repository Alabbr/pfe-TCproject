package org.example.gestionrh.tcproject.Controllers;

import org.example.gestionrh.tcproject.Entities.DocumentRequest;
import org.example.gestionrh.tcproject.Entities.Department;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Repositories.DocumentRequestRepository;
import org.example.gestionrh.tcproject.Repositories.DepartmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents/requests")
public class DocumentRequestController {

    private final DocumentRequestRepository requestRepo;
    private final DepartmentRepository deptRepo;
    private final org.example.gestionrh.tcproject.Repositories.UserRepository userRepo;

    public DocumentRequestController(DocumentRequestRepository requestRepo, DepartmentRepository deptRepo, org.example.gestionrh.tcproject.Repositories.UserRepository userRepo) {
        this.requestRepo = requestRepo;
        this.deptRepo = deptRepo;
        this.userRepo = userRepo;
    }

    // Crée une nouvelle demande de document (ex: attestation) adressée à un département spécifique
    @PostMapping("/create")
    public ResponseEntity<?> createRequest(@AuthenticationPrincipal User currentUser,
                                           @RequestParam Long departmentId,
                                           @RequestParam String description,
                                           @RequestParam(required = false) String documentType) {
        try {
            Department targetDept = deptRepo.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            
            DocumentRequest req = DocumentRequest.builder()
                    .requester(currentUser)
                    .targetDepartment(targetDept)
                    .description(description)
                    .documentType(documentType)
                    .status("PENDING")
                    .build();
            requestRepo.save(req);
            
            // Return a simple DTO to avoid lazy-loading serialization issues
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", req.getId());
            response.put("description", req.getDescription());
            response.put("status", req.getStatus());
            response.put("departmentName", targetDept.getName());
            response.put("message", "Demande envoyée avec succès");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating request: " + e.getMessage());
        }
    }

    // Retourne toutes les demandes adressées à un département donné
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<?> getDepartmentRequests(@PathVariable Long departmentId) {
        List<DocumentRequest> reqs = requestRepo.findByTargetDepartmentIdOrderByCreatedAtDesc(departmentId);
        
        List<java.util.Map<String, Object>> result = reqs.stream().map(req -> {
            java.util.Map<String, Object> dto = new java.util.HashMap<>();
            dto.put("id", req.getId());
            dto.put("description", req.getDescription());
            dto.put("documentType", req.getDocumentType());
            dto.put("status", req.getStatus());
            dto.put("createdAt", req.getCreatedAt().toString());
            if (req.getAssignedTo() != null) {
                dto.put("assignedToName", req.getAssignedTo().getFirstName() + " " + req.getAssignedTo().getLastName());
            }
            
            java.util.Map<String, String> requester = new java.util.HashMap<>();
            requester.put("firstName", req.getRequester().getFirstName());
            requester.put("lastName", req.getRequester().getLastName());
            requester.put("email", req.getRequester().getEmail());
            dto.put("requester", requester);
            
            return dto;
        }).collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    // Marque une demande de document comme traitée (complétée)
    @PostMapping("/{requestId}/fulfill")
    public ResponseEntity<?> fulfillRequest(@PathVariable Long requestId) {
        DocumentRequest req = requestRepo.findById(requestId).orElseThrow();
        req.setStatus("FULFILLED");
        requestRepo.save(req);
        return ResponseEntity.ok(java.util.Map.of("message", "Request marked as fulfilled."));
    }

    // Affecte une demande de document à un employé spécifique pour traitement
    @PutMapping("/{requestId}/assign")
    public ResponseEntity<?> assignRequest(@PathVariable Long requestId, @RequestParam Long assigneeId) {
        DocumentRequest req = requestRepo.findById(requestId).orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        User assignee = userRepo.findById(assigneeId).orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        req.setAssignedTo(assignee);
        req.setStatus("ASSIGNED");
        requestRepo.save(req);
        return ResponseEntity.ok(java.util.Map.of("message", "Demande affectée avec succès"));
    }

    // Délègue une demande de document à un autre employé (par l'employé assigné)
    @PutMapping("/{requestId}/delegate")
    public ResponseEntity<?> delegateRequest(@PathVariable Long requestId, @RequestParam Long delegatedToId) {
        DocumentRequest req = requestRepo.findById(requestId).orElseThrow(() -> new RuntimeException("Demande non trouvée"));
        User delegatedTo = userRepo.findById(delegatedToId).orElseThrow(() -> new RuntimeException("Employé non trouvé"));
        req.setDelegatedTo(delegatedTo);
        req.setStatus("DELEGATED");
        requestRepo.save(req);
        return ResponseEntity.ok(java.util.Map.of("message", "Demande déléguée avec succès"));
    }

    // Retourne la liste des demandes qui sont assignées ou déléguées à l'utilisateur connecté
    @GetMapping("/assigned")
    public ResponseEntity<?> getAssignedRequests(@AuthenticationPrincipal User currentUser) {
        List<DocumentRequest> reqs = requestRepo.findAll().stream()
                .filter(r -> (r.getAssignedTo() != null && r.getAssignedTo().getId().equals(currentUser.getId()) && r.getStatus().equals("ASSIGNED"))
                          || (r.getDelegatedTo() != null && r.getDelegatedTo().getId().equals(currentUser.getId()) && r.getStatus().equals("DELEGATED")))
                .collect(java.util.stream.Collectors.toList());

        List<java.util.Map<String, Object>> result = reqs.stream().map(req -> {
            java.util.Map<String, Object> dto = new java.util.HashMap<>();
            dto.put("id", req.getId());
            dto.put("description", req.getDescription());
            dto.put("documentType", req.getDocumentType());
            dto.put("status", req.getStatus());
            dto.put("createdAt", req.getCreatedAt().toString());

            java.util.Map<String, Object> requester = new java.util.HashMap<>();
            requester.put("id", req.getRequester().getId());
            requester.put("firstName", req.getRequester().getFirstName());
            requester.put("lastName", req.getRequester().getLastName());
            requester.put("email", req.getRequester().getEmail());
            // Added fields for template rendering
            requester.put("role", req.getRequester().getRole().toString());
            if (req.getRequester().getDepartment() != null) {
                requester.put("department", req.getRequester().getDepartment().getName());
            }
            if (req.getRequester().getJobPosition() != null) {
                requester.put("jobPosition", req.getRequester().getJobPosition().getName());
            }
            dto.put("requester", requester);
            return dto;
        }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
