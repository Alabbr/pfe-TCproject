package org.example.gestionrh.tcproject.Controllers;

import org.example.gestionrh.tcproject.Entities.ProjectDocument;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Services.ProjectDocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/projects/documents")
public class ProjectDocumentController {

    private final ProjectDocumentService documentService;

    public ProjectDocumentController(ProjectDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/{projectId}")
    public ResponseEntity<?> uploadDocument(@PathVariable Long projectId, @RequestParam("file") MultipartFile file, @AuthenticationPrincipal User currentUser) {
        try {
            documentService.uploadDocument(projectId, file, currentUser);
            return ResponseEntity.ok(Map.of("message", "Document téléchargé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getProjectDocuments(@PathVariable Long projectId) {
        return ResponseEntity.ok(documentService.getProjectDocuments(projectId));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        ProjectDocument doc = documentService.getDocument(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(new ByteArrayResource(doc.getData()));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(Map.of("message", "Document supprimé"));
    }
}
