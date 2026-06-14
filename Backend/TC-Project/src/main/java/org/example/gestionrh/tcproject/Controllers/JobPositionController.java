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

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<Map<String, Object>>> getJobPositionsByDepartment(@PathVariable Long departmentId) {
        List<JobPosition> positions = jobPositionRepository.findByDepartment_Id(departmentId);
        List<Map<String, Object>> result = positions.stream()
                .map(p -> Map.<String, Object>of("id", p.getId(), "name", p.getName(), "departmentId", departmentId))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
