package org.example.gestionrh.tcproject.Services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.example.gestionrh.tcproject.Dtos.response.ChatMessageDto;
import org.example.gestionrh.tcproject.Entities.Project;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Repositories.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AgentService {

    private final GeminiService geminiService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final ChatMessageService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    public AgentService(GeminiService geminiService, ProjectService projectService, TaskService taskService, ChatMessageService chatService, SimpMessagingTemplate messagingTemplate, UserRepository userRepository) {
        this.geminiService = geminiService;
        this.projectService = projectService;
        this.taskService = taskService;
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    public Map<String, Object> executeCommand(String command, User currentUser) throws Exception {
        // Build the list of users for the AI to know who exists
        List<User> allUsers = userRepository.findByIsActiveTrue();
        StringBuilder userList = new StringBuilder();
        for (User u : allUsers) {
            userList.append("- ID: ").append(u.getId())
                    .append(", Nom: ").append(u.getFirstName()).append(" ").append(u.getLastName())
                    .append(", Rôle: ").append(u.getRole())
                    .append("\n");
        }

        String systemPrompt = "Tu es l'Agent d'Action IA de Tunisie Clearing. L'utilisateur va te donner un ordre en langage naturel. " +
            "Ton but est de comprendre l'intention et d'extraire les paramètres. " +
            "Tu dois renvoyer UNIQUEMENT un objet JSON valide, sans aucun texte autour.\n" +
            "Actions supportées :\n" +
            "1. 'CREATE_PROJECT' : Créer un projet. Params attendus: 'title' (string), 'description' (string).\n" +
            "2. 'CREATE_TASK' : Créer une tâche. Params attendus: 'title' (string), 'description' (string), 'projectId' (number, si mentionné, sinon null), 'assigneeId' (number, l'ID de la personne à qui assigner la tâche, si mentionné, sinon null).\n" +
            "3. 'SEND_MESSAGE' : Envoyer un message dans le chat général (channel). Params attendus: 'content' (string).\n" +
            "4. 'SEND_DM' : Envoyer un message privé (direct message) à un utilisateur spécifique. Params attendus: 'recipientId' (number, l'ID de l'utilisateur), 'content' (string, le message à envoyer).\n" +
            "5. 'CHAT' : Si l'utilisateur dit juste bonjour, pose une question simple, ou discute sans demander une des actions ci-dessus. Params attendus: aucun.\n" +
            "\nVoici la liste des utilisateurs actifs du système :\n" + userList +
            "\nQuand l'utilisateur mentionne un nom de personne, cherche dans cette liste et utilise son ID pour 'recipientId'.\n" +
            "Format attendu : {\"action\": \"NOM_ACTION\", \"params\": {\"cle\": \"valeur\"}, \"reply\": \"Message court de confirmation amical, ou la réponse à la question si l'action est CHAT\"}";

        String aiResponse = geminiService.askRaw(systemPrompt, command);

        if (aiResponse.contains("```json")) {
            aiResponse = aiResponse.substring(aiResponse.indexOf("```json") + 7);
        }
        if (aiResponse.contains("```")) {
            aiResponse = aiResponse.substring(0, aiResponse.lastIndexOf("```"));
        }
        aiResponse = aiResponse.trim();

        JsonObject rootNode = JsonParser.parseString(aiResponse).getAsJsonObject();

        if (!rootNode.has("action")) {
            throw new RuntimeException("Je n'ai pas compris l'action demandée.");
        }

        String action = rootNode.get("action").getAsString();
        JsonObject params = rootNode.has("params") && !rootNode.get("params").isJsonNull() ? rootNode.getAsJsonObject("params") : new JsonObject();
        String reply = rootNode.has("reply") && !rootNode.get("reply").isJsonNull() ? rootNode.get("reply").getAsString() : "Action exécutée.";

        Map<String, Object> result = new HashMap<>();
        result.put("reply", reply);
        result.put("action", action);

        try {
            switch (action) {
                case "CREATE_PROJECT":
                    Map<String, Object> projData = new HashMap<>();
                    projData.put("title", params.has("title") ? params.get("title").getAsString() : "Nouveau Projet IA");
                    projData.put("description", params.has("description") ? params.get("description").getAsString() : "Projet créé par l'Agent IA");
                    Project p = projectService.createProject(projData, currentUser);
                    result.put("projectId", p.getId());
                    break;
                case "CREATE_TASK":
                    Long pId = null;
                    if (params.has("projectId") && !params.get("projectId").isJsonNull()) {
                        pId = params.get("projectId").getAsLong();
                    } else {
                        List<Map<String, Object>> userProjects = projectService.getAllProjectsForUser(currentUser);
                        if (!userProjects.isEmpty()) {
                            pId = ((Number) userProjects.get(0).get("id")).longValue();
                        } else {
                            throw new RuntimeException("Vous n'avez aucun projet pour assigner cette tâche.");
                        }
                    }
                    Map<String, Object> taskData = new HashMap<>();
                    taskData.put("projectId", pId);
                    taskData.put("title", params.has("title") ? params.get("title").getAsString() : "Nouvelle Tâche IA");
                    taskData.put("description", params.has("description") ? params.get("description").getAsString() : "");
                    taskData.put("priority", "NORMALE");
                    if (params.has("assigneeId") && !params.get("assigneeId").isJsonNull()) {
                        taskData.put("assigneeId", params.get("assigneeId").getAsLong());
                    }
                    taskService.createTask(taskData, currentUser);
                    break;
                case "SEND_MESSAGE":
                    String content = params.has("content") ? params.get("content").getAsString() : "Bonjour de l'IA !";
                    ChatMessageDto chatMessage = new ChatMessageDto();
                    chatMessage.setContent(content);
                    chatMessage.setChannelId("general");
                    chatMessage.setSenderId(currentUser.getId());
                    ChatMessageDto savedMessage = chatService.saveMessage(chatMessage);
                    messagingTemplate.convertAndSend("/topic/general", savedMessage);
                    break;
                case "SEND_DM":
                    String dmContent = params.has("content") ? params.get("content").getAsString() : "Bonjour !";
                    Long recipientId = params.has("recipientId") ? params.get("recipientId").getAsLong() : null;
                    if (recipientId == null) {
                        throw new RuntimeException("Aucun destinataire spécifié pour le message privé.");
                    }
                    Optional<User> recipientOpt = userRepository.findById(recipientId);
                    if (recipientOpt.isEmpty()) {
                        throw new RuntimeException("Utilisateur introuvable avec l'ID " + recipientId);
                    }
                    User recipient = recipientOpt.get();
                    ChatMessageDto dmMessage = new ChatMessageDto();
                    dmMessage.setContent(dmContent);
                    dmMessage.setSenderId(currentUser.getId());
                    dmMessage.setReceiverId(recipientId);
                    ChatMessageDto savedDM = chatService.saveMessage(dmMessage);
                    // Send to recipient via their personal queue
                    messagingTemplate.convertAndSendToUser(
                        recipient.getEmail(), "/queue/messages", savedDM
                    );
                    // Send back to sender
                    messagingTemplate.convertAndSendToUser(
                        currentUser.getEmail(), "/queue/messages", savedDM
                    );
                    break;
                case "CHAT":
                    // Ne rien faire côté système, la réponse de l'IA (reply) sera affichée directement dans l'interface Co-Pilot
                    break;
                default:
                    throw new RuntimeException("Action non supportée : " + action);
            }
            result.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("reply", "Désolé, j'ai compris la commande mais une erreur est survenue : " + e.getMessage());
        }

        return result;
    }
}
