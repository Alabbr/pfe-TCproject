package org.example.gestionrh.tcproject.Services;

import org.example.gestionrh.tcproject.Entities.Permission;
import org.example.gestionrh.tcproject.Entities.Project;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Repositories.ProjectRepository;
import org.example.gestionrh.tcproject.Repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.gestionrh.tcproject.Entities.Task;
import org.example.gestionrh.tcproject.Entities.TaskStatus;
import org.example.gestionrh.tcproject.Entities.TaskPriority;
import org.example.gestionrh.tcproject.Repositories.TaskRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WebSocketNotificationService notificationService;
    private final GeminiService geminiService;
    private final TaskRepository taskRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository, 
                          WebSocketNotificationService notificationService, GeminiService geminiService, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.geminiService = geminiService;
        this.taskRepository = taskRepository;
    }

    public Project createProject(Map<String, Object> data, User creator) {
        boolean isAdmin = creator.getRole().name().equals("SUPER_ADMIN") || creator.getRole().name().equals("DIRECTEUR_GENERAL");
        boolean canManage = isAdmin || creator.getRole().name().equals("DIRECTEUR") ||
                (creator.getPermissions() != null && creator.getPermissions().contains(Permission.MANAGE_PROJECTS));
        if (!canManage) {
            throw new RuntimeException("Non autorisé à créer un projet");
        }

        Project project = new Project();
        project.setTitle((String) data.get("title"));
        project.setDescription((String) data.get("description"));
        if (data.get("deadline") != null) {
            project.setDeadline(LocalDate.parse(data.get("deadline").toString()));
        }
        if (data.get("startDate") != null) {
            project.setStartDate(LocalDate.parse(data.get("startDate").toString()));
        }
        project.setCreatedBy(creator);
        
        List<?> rawMemberIds = (List<?>) data.get("memberIds");
        if (rawMemberIds != null && !rawMemberIds.isEmpty()) {
            List<Long> memberIds = rawMemberIds.stream()
                .map(id -> Long.parseLong(id.toString()))
                .collect(Collectors.toList());
            List<User> members = userRepository.findAllById(memberIds);
            project.setMembers(members);
        }
        
        Project saved = projectRepository.save(project);
        
        // Notify members
        if (saved.getMembers() != null) {
            for (User member : saved.getMembers()) {
                if (!member.getId().equals(creator.getId())) {
                    notificationService.sendToUser(
                        member,
                        "Nouveau Projet",
                        creator.getFirstName() + " vous a ajouté au projet: " + saved.getTitle(),
                        "PROJECT_CREATED",
                        "/projects/" + saved.getId()
                    );
                }
            }
        }
        
        // Global notification (optional) - let's keep it to members to avoid spam
        notificationService.sendGlobalNotification(
            "Nouveau Projet", 
            "Le projet '" + saved.getTitle() + "' a été créé par " + creator.getFirstName(), 
            "PROJECT_CREATED", 
            "/projects"
        );
        
        return saved;
    }

    public List<Map<String, Object>> getAllProjectsForUser(User user) {
        List<Project> allProjects;
        boolean isAdmin = user.getRole().name().equals("SUPER_ADMIN") || user.getRole().name().equals("DIRECTEUR_GENERAL");
        boolean canManage = isAdmin || (user.getPermissions() != null && user.getPermissions().contains(Permission.MANAGE_PROJECTS));
        
        if (canManage) {
            allProjects = projectRepository.findAll();
        } else {
            List<Project> created = projectRepository.findByCreatedBy(user);
            List<Project> memberOf = projectRepository.findByMembersContaining(user);
            created.removeAll(memberOf);
            created.addAll(memberOf);
            allProjects = created;
        }

        return allProjects.stream().map(this::mapToDTO).collect(Collectors.toList());
    }
    
    public Map<String, Object> getProjectById(Long id) {
        Project p = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Projet introuvable"));
        return mapToDTO(p);
    }

    public Project updateProject(Long id, Map<String, Object> data, User user) {
        Project project = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Projet introuvable"));
        
        boolean isCreator = project.getCreatedBy().getId().equals(user.getId());
        boolean isAdmin = user.getRole().name().equals("SUPER_ADMIN") || user.getRole().name().equals("DIRECTEUR_GENERAL");
        boolean canManage = isAdmin || (user.getPermissions() != null && user.getPermissions().contains(Permission.MANAGE_PROJECTS));
        
        if (!isCreator && !canManage) {
            throw new RuntimeException("Non autorisé à modifier ce projet");
        }

        if (data.containsKey("title")) project.setTitle((String) data.get("title"));
        if (data.containsKey("description")) project.setDescription((String) data.get("description"));
        if (data.get("deadline") != null) project.setDeadline(LocalDate.parse(data.get("deadline").toString()));
        if (data.get("startDate") != null) project.setStartDate(LocalDate.parse(data.get("startDate").toString()));
        
        List<?> rawMemberIds = (List<?>) data.get("memberIds");
        if (rawMemberIds != null) {
            List<Long> memberIds = rawMemberIds.stream()
                .map(idStr -> Long.parseLong(idStr.toString()))
                .collect(Collectors.toList());
            List<User> members = userRepository.findAllById(memberIds);
            project.setMembers(members);
        }
        
        return projectRepository.save(project);
    }

    public void deleteProject(Long id, User user) {
        Project project = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Projet introuvable"));
        
        boolean isCreator = project.getCreatedBy().getId().equals(user.getId());
        boolean isAdmin = user.getRole().name().equals("SUPER_ADMIN") || user.getRole().name().equals("DIRECTEUR_GENERAL");
        boolean canManage = isAdmin || (user.getPermissions() != null && user.getPermissions().contains(Permission.MANAGE_PROJECTS));
        
        if (!isCreator && !canManage) {
            throw new RuntimeException("Non autorisé à supprimer ce projet");
        }
        
        projectRepository.delete(project);
    }

    public List<Map<String, Object>> generateTasksFromAI(Long projectId, String prompt, User user) throws Exception {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Projet introuvable"));
        
        boolean isCreator = project.getCreatedBy().getId().equals(user.getId());
        boolean isAdmin = user.getRole().name().equals("SUPER_ADMIN") || user.getRole().name().equals("DIRECTEUR_GENERAL");
        boolean canManage = isAdmin || (user.getPermissions() != null && user.getPermissions().contains(Permission.MANAGE_PROJECTS));
        
        if (!isCreator && !canManage) {
            throw new RuntimeException("Non autorisé à générer des tâches pour ce projet");
        }

        String systemPrompt = "Tu es un chef de projet expert. Ton but est de générer une liste de tâches pour un projet basé sur la description de l'utilisateur. " +
            "Tu dois renvoyer UNIQUEMENT un tableau JSON (pas de markdown, pas de texte avant ou après). " +
            "Chaque objet du tableau doit avoir les champs suivants: 'title' (string, max 100 chars) et 'description' (string). " +
            "Génère entre 4 et 8 tâches pertinentes.";

        String aiResponse = geminiService.askRaw(systemPrompt, prompt);

        // Nettoyer la réponse pour en extraire uniquement le JSON si le modèle a mis du markdown
        if (aiResponse.contains("```json")) {
            aiResponse = aiResponse.substring(aiResponse.indexOf("```json") + 7);
        }
        if (aiResponse.contains("```")) {
            aiResponse = aiResponse.substring(0, aiResponse.lastIndexOf("```"));
        }
        aiResponse = aiResponse.trim();

        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, String>>>(){}.getType();
        List<Map<String, String>> tasksData = gson.fromJson(aiResponse, listType);

        List<Task> savedTasks = new ArrayList<>();
        for (Map<String, String> tData : tasksData) {
            Task task = new Task();
            task.setProject(project);
            task.setTitle(tData.get("title"));
            task.setDescription(tData.get("description"));
            task.setStatus(TaskStatus.A_FAIRE);
            task.setPriority(TaskPriority.NORMALE);
            task.setReporter(user);
            savedTasks.add(taskRepository.save(task));
        }

        return savedTasks.stream().map(t -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", t.getId());
            dto.put("title", t.getTitle());
            dto.put("description", t.getDescription());
            dto.put("status", t.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> mapToDTO(Project p) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", p.getId());
        dto.put("title", p.getTitle());
        dto.put("description", p.getDescription());
        dto.put("status", p.getStatus());
        dto.put("startDate", p.getStartDate());
        dto.put("deadline", p.getDeadline());
        
        Map<String, Object> creator = new HashMap<>();
        creator.put("id", p.getCreatedBy().getId());
        creator.put("firstName", p.getCreatedBy().getFirstName());
        creator.put("lastName", p.getCreatedBy().getLastName());
        dto.put("createdBy", creator);
        
        List<Map<String, Object>> members = new ArrayList<>();
        if (p.getMembers() != null) {
            for (User u : p.getMembers()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", u.getId());
                m.put("firstName", u.getFirstName());
                m.put("lastName", u.getLastName());
                m.put("email", u.getEmail());
                members.add(m);
            }
        }
        dto.put("members", members);
        
        return dto;
    }
}
