package org.example.gestionrh.tcproject.Controllers;

import org.example.gestionrh.tcproject.Entities.Project;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Services.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<?> createProject(@RequestBody Map<String, Object> data, @AuthenticationPrincipal User currentUser) {
        try {
            Project project = projectService.createProject(data, currentUser);
            return ResponseEntity.ok(Map.of("message", "Projet créé avec succès", "projectId", project.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyProjects(@AuthenticationPrincipal User currentUser) {
        List<Map<String, Object>> projects = projectService.getAllProjectsForUser(currentUser);
        return ResponseEntity.ok(projects);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(projectService.getProjectById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject (@PathVariable Long id, @RequestBody Map<String, Object> data, @AuthenticationPrincipal User currentUser) {
        try {
            Project project = projectService.updateProject(id, data, currentUser);
            return ResponseEntity.ok(Map.of("message", "Projet mis à jour avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        try {
            projectService.deleteProject(id, currentUser);
            return ResponseEntity.ok(Map.of("message", "Projet supprimé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
