package org.example.gestionrh.tcproject.Services;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Service to call the Groq API (Llama 3.3 70B) via REST.
 * Uses OpenAI-compatible API format.
 * Class name kept as GeminiService to avoid breaking RagChatService references.
 */
@Service
public class GeminiService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model.name:llama-3.3-70b-versatile}")
    private String modelName;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private static final String RAG_SYSTEM_PROMPT =
            "Tu es l'assistant IA officiel de Tunisie Clearing (TC Hub). " +
            "Voici tes règles:\n" +
            "1. Si l'utilisateur te salue (bonjour, hi, salut, etc.), réponds poliment et propose ton aide.\n" +
            "2. Pour les questions, utilise le contexte fourni ci-dessous en priorité.\n" +
            "3. Cite les sources quand c'est possible.\n" +
            "4. Si l'information n'est pas explicite dans le contexte, essaie de donner une réponse utile et logique à partir de tes connaissances, tout en précisant que l'information n'a pas été trouvée dans les documents internes.\n" +
            "5. Réponds en français, de manière professionnelle et claire.\n" +
            "6. Si le contexte contient des tableaux, formate-les proprement dans ta réponse.";

    public String askWithContext(String question, String context) throws IOException {
        return askWithContext(question, context, null);
    }

    // Envoie la question + le contexte RAG au LLM Groq/Llama 3.3 avec l'historique
    public String askWithContext(String question, String context, java.util.List<java.util.Map<String, String>> history) throws IOException {
        String userMessage;
        if (context != null && !context.isBlank()) {
            userMessage = "--- Contexte des documents ---\n" + context + "\n\n--- Question ---\n" + question;
        } else {
            userMessage = "--- Question ---\n" + question;
        }

        StringBuilder messagesJson = new StringBuilder();
        String escapedSystem = escapeJson(RAG_SYSTEM_PROMPT);
        messagesJson.append("{\"role\":\"system\",\"content\":\"").append(escapedSystem).append("\"},");

        if (history != null && !history.isEmpty()) {
            // Keep up to 6 last messages (3 turns) for context to avoid huge payloads
            int startIndex = Math.max(0, history.size() - 6);
            for (int i = startIndex; i < history.size(); i++) {
                java.util.Map<String, String> msg = history.get(i);
                String role = msg.get("role");
                String content = msg.get("content");
                if (role != null && content != null) {
                    messagesJson.append("{\"role\":\"").append(role).append("\",\"content\":\"").append(escapeJson(content)).append("\"},");
                }
            }
        }
        
        messagesJson.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(userMessage)).append("\"}");

        String jsonBody = "{" +
                "\"model\":\"" + modelName + "\"," +
                "\"messages\":[" + messagesJson.toString() + "]," +
                "\"temperature\":0.3," +
                "\"max_tokens\":4096" +
                "}";

        // Retry logic for rate limit errors (up to 3 retries with backoff)
        int maxRetries = 3;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            Request request = new Request.Builder()
                    .url(GROQ_API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.code() == 429 && attempt < maxRetries) {
                    long waitSeconds = (long) Math.pow(2, attempt + 1) * 5; // 10s, 20s, 40s
                    System.out.println("Groq 429 rate limit hit. Waiting " + waitSeconds + "s before retry " + (attempt + 1) + "/" + maxRetries + "...");
                    try { Thread.sleep(waitSeconds * 1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "unknown error";
                    throw new IOException("Groq API error " + response.code() + ": " + errBody);
                }
                String responseBody = response.body().string();
                return extractContentFromResponse(responseBody);
            }
        }
        throw new IOException("Groq API: rate limit exceeded after " + maxRetries + " retries. Veuillez réessayer dans quelques minutes.");
    }

    /**
     * Escapes a string for safe JSON embedding.
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Extracts the assistant's response content from OpenAI-compatible JSON.
     * Looks for: "content":"..." inside the choices array.
     */
    private String extractContentFromResponse(String json) {
        // Find "content":" pattern (the assistant's message)
        // We need to find the content inside choices[0].message.content
        String key = "\"content\":";
        
        // Skip the first "content" occurrence (which is the system/user message echo)
        // and find the one inside "message" object of choices
        int choicesIdx = json.indexOf("\"choices\"");
        if (choicesIdx == -1) {
            return "Désolé, je n'ai pas pu générer de réponse.";
        }

        int index = json.indexOf(key, choicesIdx);
        if (index == -1) {
            return "Désolé, je n'ai pas pu générer de réponse.";
        }

        // Find the opening quote of the value
        int startQuote = json.indexOf("\"", index + key.length());
        if (startQuote == -1) {
            return "Désolé, je n'ai pas pu générer de réponse.";
        }

        // Parse the JSON string value handling escape sequences
        StringBuilder extracted = new StringBuilder();
        boolean escaped = false;
        for (int i = startQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n': extracted.append('\n'); break;
                    case 'r': extracted.append('\r'); break;
                    case 't': extracted.append('\t'); break;
                    case '"': extracted.append('"'); break;
                    case '\\': extracted.append('\\'); break;
                    default: extracted.append('\\').append(c); break;
                }
                escaped = false;
            } else {
                if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break;
                } else {
                    extracted.append(c);
                }
            }
        }
        return extracted.toString();
    }
}
