package org.example.gestionrh.tcproject.Controllers;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.DepartmentDto;
import org.example.gestionrh.tcproject.Services.DepartmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    // Retourne la liste de tous les départements actifs
    @GetMapping
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    // Retourne la liste de tous les départements archivés (inactifs)
    @GetMapping("/archived")
    public ResponseEntity<List<DepartmentDto>> getArchivedDepartments() {
        return ResponseEntity.ok(departmentService.getArchivedDepartments());
    }

    // Retourne les détails d'un département en fonction de son ID
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDto> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    // Crée un nouveau département
    @PostMapping
    public ResponseEntity<DepartmentDto> createDepartment(@RequestBody DepartmentDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(dto));
    }

    // Met à jour les informations d'un département existant
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDto> updateDepartment(@PathVariable Long id, @RequestBody DepartmentDto dto) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, dto));
    }

    // Supprime un département de manière logique (mise en corbeille / archivage)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    // Restaure un département précédemment archivé
    @PutMapping("/{id}/restore")
    public ResponseEntity<Void> restoreDepartment(@PathVariable Long id) {
        departmentService.restoreDepartment(id);
        return ResponseEntity.noContent().build();
    }

    // Supprime définitivement un département archivé (non réversible)
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentDeleteDepartment(@PathVariable Long id) {
        departmentService.permanentDeleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
