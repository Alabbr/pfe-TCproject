package org.example.gestionrh.tcproject.Controllers;

import org.example.gestionrh.tcproject.Entities.RagConversation;
import org.example.gestionrh.tcproject.Entities.RagMessage;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Repositories.RagConversationRepository;
import org.example.gestionrh.tcproject.Repositories.RagMessageRepository;
import org.example.gestionrh.tcproject.Services.DocumentIngestionService;
import org.example.gestionrh.tcproject.Services.RagChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagChatService ragChatService;
    private final DocumentIngestionService ingestionService;
    private final RagConversationRepository conversationRepository;
    private final RagMessageRepository messageRepository;

    // Constructeur : injecte les services RAG (chat IA) et ingestion (indexation de documents)
    public RagController(RagChatService ragChatService, 
                         DocumentIngestionService ingestionService,
                         RagConversationRepository conversationRepository,
                         RagMessageRepository messageRepository) {
        this.ragChatService = ragChatService;
        this.ingestionService = ingestionService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    // Reçoit une question de l'utilisateur et retourne la réponse IA basée sur les documents indexés (RAG)
    @PostMapping("/chat")
    public ResponseEntity<?> askQuestion(@AuthenticationPrincipal User currentUser, @RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question cannot be empty"));
        }
        
        Long conversationId = null;
        if (request.containsKey("conversationId") && request.get("conversationId") != null) {
            try {
                conversationId = Long.parseLong(request.get("conversationId").toString());
            } catch (Exception e) {
                // Ignore parsing error
            }
        }
        
        try {
            Map<String, Object> result = ragChatService.askQuestion(question, conversationId, currentUser);
            return ResponseEntity.ok(result);
        } catch (Throwable e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get answer from AI: " + e.getMessage()));
        }
    }

    // Récupère la liste des conversations de l'utilisateur connecté
    @GetMapping("/conversations")
    public ResponseEntity<List<RagConversation>> getConversations(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(conversationRepository.findByUserIdOrderByUpdatedAtDesc(currentUser.getId()));
    }

    // Récupère les messages d'une conversation spécifique
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        RagConversation conversation = conversationRepository.findById(id).orElse(null);
        if (conversation == null || !conversation.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Conversation not found or unauthorized"));
        }
        return ResponseEntity.ok(messageRepository.findByConversationIdOrderByCreatedAtAsc(id));
    }

    // Supprime une conversation
    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<?> deleteConversation(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        RagConversation conversation = conversationRepository.findById(id).orElse(null);
        if (conversation == null || !conversation.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Conversation not found or unauthorized"));
        }
        conversationRepository.delete(conversation);
        return ResponseEntity.ok(Map.of("message", "Conversation deleted successfully"));
    }

    // Reçoit un fichier PDF, le sauvegarde temporairement, l'indexe dans le vector store puis le supprime
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        try {
            // Save file temporarily
            File tempFile = File.createTempFile("upload-", "-" + file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }

            // Ingest document into Vector DB
            ingestionService.ingestPdf(tempFile);

            // Clean up
            tempFile.delete();

            return ResponseEntity.ok(Map.of("message", "Document successfully ingested into the Knowledge Base!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to ingest document: " + e.getMessage()));
        }
    }

    // Importe en masse tous les PDFs du dossier "RAG pdf" (parcourt récursivement les sous-dossiers)
    @PostMapping("/bulk-import")
    public ResponseEntity<?> bulkImport() {
        String folderPath = "C:\\Users\\sreou\\OneDrive\\Bureau\\pfe-project\\RAG pdf";
        File folder = new File(folderPath);
        
        if (!folder.exists() || !folder.isDirectory()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Folder not found: " + folderPath));
        }

        try {
            int count = processFolder(folder);
            return ResponseEntity.ok(Map.of("message", "Successfully ingested " + count + " documents!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed during bulk import: " + e.getMessage()));
        }
    }

    // Helper récursif : parcourt un dossier et ingère chaque fichier PDF trouvé
    private int processFolder(File folder) {
        int count = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    count += processFolder(file);
                } else if (file.getName().toLowerCase().endsWith(".pdf")) {
                    ingestionService.ingestPdf(file);
                    count++;
                }
            }
        }
        return count;
    }
}
