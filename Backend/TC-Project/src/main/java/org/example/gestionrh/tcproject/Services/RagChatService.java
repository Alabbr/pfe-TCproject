package org.example.gestionrh.tcproject.Services;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.example.gestionrh.tcproject.Entities.RagConversation;
import org.example.gestionrh.tcproject.Entities.RagMessage;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Repositories.RagConversationRepository;
import org.example.gestionrh.tcproject.Repositories.RagMessageRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RagChatService {

    private final GeminiService geminiService;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final RagConversationRepository conversationRepository;
    private final RagMessageRepository messageRepository;

    // Constructeur : injecte le service LLM (Gemini/Groq), le modèle d'embedding, le vector store et les repositories
    public RagChatService(GeminiService geminiService,
                          EmbeddingModel embeddingModel,
                          EmbeddingStore<TextSegment> embeddingStore,
                          RagConversationRepository conversationRepository,
                          RagMessageRepository messageRepository) {
        this.geminiService = geminiService;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    // Surcharge pour compatibilité ancienne (sans historique)
    public Map<String, Object> askQuestion(String question) {
        return askQuestion(question, null, null);
    }

    // Répond à une question avec persistance de la conversation
    public Map<String, Object> askQuestion(String question, Long conversationId, User user) {
        try {
            RagConversation conversation;
            if (conversationId != null) {
                conversation = conversationRepository.findById(conversationId)
                        .orElseThrow(() -> new RuntimeException("Conversation not found"));
                conversation.setUpdatedAt(java.time.LocalDateTime.now());
                conversation = conversationRepository.save(conversation);
            } else {
                conversation = new RagConversation();
                conversation.setUser(user);
                // Utiliser les 30 premiers caractères pour le titre
                conversation.setTitle(question.length() > 30 ? question.substring(0, 30) + "..." : question);
                conversation = conversationRepository.save(conversation);
            }

            // Sauvegarder le message de l'utilisateur
            RagMessage userMsg = new RagMessage();
            userMsg.setConversation(conversation);
            userMsg.setRole("user");
            userMsg.setContent(question);
            messageRepository.save(userMsg);

            // Charger l'historique récent (les 6 derniers messages)
            List<RagMessage> dbMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            List<Map<String, String>> history = dbMessages.stream().map(m -> {
                Map<String, String> map = new HashMap<>();
                map.put("role", m.getRole());
                map.put("content", m.getContent());
                return map;
            }).collect(Collectors.toList());

            // 1. Convert the question to an embedding vector
            Embedding questionEmbedding = embeddingModel.embed(question).content();

            // 2. Search the vector store for the top 10 most relevant chunks
            // minScore à 0.0 pour ne pas filtrer trop agressivement (différences linguistiques)
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(questionEmbedding, 10, 0.0);

            // 3. Build the context string from matched chunks
            String context = matches.stream()
                    .map(match -> {
                        String source = match.embedded().metadata().getString("source");
                        String text = match.embedded().text();
                        return (source != null ? "[Source: " + source + "]\n" : "") + text;
                    })
                    .collect(Collectors.joining("\n\n---\n\n"));

            // 4. Call LLM with the context + question + history
            String answer = geminiService.askWithContext(question, context, history);

            // Sauvegarder le message de l'IA
            RagMessage aiMsg = new RagMessage();
            aiMsg.setConversation(conversation);
            aiMsg.setRole("assistant");
            aiMsg.setContent(answer);
            messageRepository.save(aiMsg);

            Map<String, Object> response = new HashMap<>();
            response.put("answer", answer);
            response.put("conversationId", conversation.getId());
            return response;

        } catch (Throwable e) {
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("answer", "Désolé, une erreur est survenue lors du traitement de votre question : " + e.getMessage());
            return err;
        }
    }
}
