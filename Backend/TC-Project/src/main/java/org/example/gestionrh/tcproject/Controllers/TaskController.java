package org.example.gestionrh.tcproject.Controllers;

import org.example.gestionrh.tcproject.Entities.Task;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Services.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Map<String, Object> data, @AuthenticationPrincipal User currentUser) {
        try {
            Task task = taskService.createTask(data, currentUser);
            return ResponseEntity.ok(Map.of("message", "Tâche créée", "taskId", task.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getProjectTasks(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<?> getMyTasks(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(taskService.getMyTasks(currentUser));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id, @RequestBody Map<String, Object> data, @AuthenticationPrincipal User currentUser) {
        try {
            String newStatus = (String) data.get("status");
            String comment = (String) data.get("comment");
            taskService.changeTaskStatus(id, newStatus, currentUser, comment);
            return ResponseEntity.ok(Map.of("message", "Statut mis à jour"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getTaskHistory(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskHistory(id));
    }
}
