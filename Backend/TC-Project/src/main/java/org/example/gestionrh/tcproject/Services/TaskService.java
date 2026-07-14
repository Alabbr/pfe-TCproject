package org.example.gestionrh.tcproject.Services;

import org.example.gestionrh.tcproject.Entities.*;
import org.example.gestionrh.tcproject.Repositories.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final EmailNotificationService emailService;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, 
                       UserRepository userRepository, TaskHistoryRepository taskHistoryRepository, 
                       EmailNotificationService emailService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskHistoryRepository = taskHistoryRepository;
        this.emailService = emailService;
    }

    public List<Map<String, Object>> getTasksByProject(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new RuntimeException("Project not found"));
        return taskRepository.findByProject(project).stream().map(this::mapToDTO).collect(Collectors.toList());
    }
    
    public List<Map<String, Object>> getMyTasks(User user) {
        return taskRepository.findByAssignee(user).stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public Task createTask(Map<String, Object> data, User reporter) {
        Project project = projectRepository.findById(Long.parseLong(data.get("projectId").toString()))
            .orElseThrow(() -> new RuntimeException("Project not found"));

        Task task = new Task();
        task.setTitle((String) data.get("title"));
        task.setDescription((String) data.get("description"));
        task.setProject(project);
        task.setReporter(reporter);
        
        if (data.get("deadline") != null) {
            task.setDeadline(LocalDate.parse(data.get("deadline").toString()));
        }
        
        if (data.get("priority") != null) {
            task.setPriority(TaskPriority.valueOf(data.get("priority").toString()));
        }

        if (data.get("assigneeId") != null) {
            User assignee = userRepository.findById(Long.parseLong(data.get("assigneeId").toString()))
                .orElse(null);
            task.setAssignee(assignee);
        }

        Task savedTask = taskRepository.save(task);
        
        // Save History
        TaskHistory history = new TaskHistory();
        history.setTask(savedTask);
        history.setChangedBy(reporter);
        history.setNewStatus(savedTask.getStatus().name());
        history.setComment("Tâche créée.");
        taskHistoryRepository.save(history);

        // Send Email
        if (savedTask.getAssignee() != null) {
            emailService.sendTaskAssignmentEmail(savedTask);
        }

        return savedTask;
    }

    public void changeTaskStatus(Long taskId, String newStatus, User user, String comment) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        String oldStatus = task.getStatus().name();
        
        TaskStatus nextStatus = TaskStatus.valueOf(newStatus);
        task.setStatus(nextStatus);
        taskRepository.save(task);

        TaskHistory history = new TaskHistory();
        history.setTask(task);
        history.setChangedBy(user);
        history.setOldStatus(oldStatus);
        history.setNewStatus(nextStatus.name());
        history.setComment(comment);
        taskHistoryRepository.save(history);

        if (nextStatus == TaskStatus.EN_VALIDATION) {
            emailService.sendTaskValidationRequestEmail(task);
        }
    }
    
    public List<Map<String, Object>> getTaskHistory(Long taskId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        return taskHistoryRepository.findByTaskOrderByChangedAtDesc(task).stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("oldStatus", h.getOldStatus());
            map.put("newStatus", h.getNewStatus());
            map.put("comment", h.getComment());
            map.put("changedAt", h.getChangedAt());
            
            Map<String, Object> changer = new HashMap<>();
            changer.put("id", h.getChangedBy().getId());
            changer.put("firstName", h.getChangedBy().getFirstName());
            changer.put("lastName", h.getChangedBy().getLastName());
            map.put("changedBy", changer);
            return map;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> mapToDTO(Task t) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", t.getId());
        dto.put("title", t.getTitle());
        dto.put("description", t.getDescription());
        dto.put("status", t.getStatus());
        dto.put("priority", t.getPriority());
        dto.put("deadline", t.getDeadline());
        dto.put("projectId", t.getProject().getId());
        
        if (t.getAssignee() != null) {
            Map<String, Object> a = new HashMap<>();
            a.put("id", t.getAssignee().getId());
            a.put("firstName", t.getAssignee().getFirstName());
            a.put("lastName", t.getAssignee().getLastName());
            dto.put("assignee", a);
        }
        
        Map<String, Object> r = new HashMap<>();
        r.put("id", t.getReporter().getId());
        r.put("firstName", t.getReporter().getFirstName());
        r.put("lastName", t.getReporter().getLastName());
        dto.put("reporter", r);

        return dto;
    }
}
