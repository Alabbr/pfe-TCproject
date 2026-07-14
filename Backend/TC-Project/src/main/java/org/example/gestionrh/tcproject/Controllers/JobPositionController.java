package org.example.gestionrh.tcproject.Controllers;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Entities.JobPosition;
import org.example.gestionrh.tcproject.Repositories.JobPositionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/job-positions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class JobPositionController {

    private final JobPositionRepository jobPositionRepository;
    private final org.example.gestionrh.tcproject.Repositories.DepartmentRepository departmentRepository;

    // Retourne la liste des postes de travail pour un département donné
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<Map<String, Object>>> getJobPositionsByDepartment(@PathVariable Long departmentId) {
        List<JobPosition> positions = jobPositionRepository.findByDepartment_Id(departmentId);
        List<Map<String, Object>> result = positions.stream()
                .map(p -> Map.<String, Object>of("id", p.getId(), "name", p.getName(), "departmentId", departmentId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // Crée un nouveau poste de travail dans un département spécifique
    @PostMapping("/department/{departmentId}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DIRECTEUR_GENERAL') or hasRole('DIRECTEUR')")
    public ResponseEntity<Map<String, Object>> createJobPosition(
            @PathVariable Long departmentId,
            @RequestBody Map<String, String> request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.example.gestionrh.tcproject.Entities.User user) {
        
        String name = request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Restrict DIRECTEUR to their own department
        if (user.getRole() == org.example.gestionrh.tcproject.Entities.RoleName.DIRECTEUR) {
            if (user.getDepartment() == null || !user.getDepartment().getId().equals(departmentId)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
            }
        }

        org.example.gestionrh.tcproject.Entities.Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Département introuvable"));

        JobPosition jobPosition = JobPosition.builder()
                .name(name.trim())
                .department(dept)
                .build();
        
        jobPosition = jobPositionRepository.save(jobPosition);
        
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(Map.<String, Object>of("id", jobPosition.getId(), "name", jobPosition.getName(), "departmentId", departmentId));
    }
}
