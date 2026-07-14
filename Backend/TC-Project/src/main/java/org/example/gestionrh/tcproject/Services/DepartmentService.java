package org.example.gestionrh.tcproject.Services;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.DepartmentDto;
import org.example.gestionrh.tcproject.Entities.Department;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Repositories.DepartmentRepository;
import org.example.gestionrh.tcproject.Repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    // Convertit une entité Department en DTO (sans récursion sur les sous-départements)
    public DepartmentDto toDto(Department dept) {
        return DepartmentDto.builder()
                .id(dept.getId())
                .name(dept.getName())
                .description(dept.getDescription())
                .parentId(dept.getParent() != null ? dept.getParent().getId() : null)
                .parentName(dept.getParent() != null ? dept.getParent().getName() : null)
                .isActive(dept.isActive())
                .build();
    }

    // Retourne la liste de tous les départements actifs
    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .filter(Department::isActive)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Retourne la liste des départements archivés (inactifs)
    public List<DepartmentDto> getArchivedDepartments() {
        return departmentRepository.findAll()
                .stream()
                .filter(dept -> !dept.isActive())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Retourne un département par son ID
    public DepartmentDto getDepartmentById(Long id) {
        return toDto(findById(id));
    }

    // Crée un nouveau département (avec parent optionnel)
    @Transactional
    public DepartmentDto createDepartment(DepartmentDto dto) {
        Department dept = Department.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .isActive(true)
                .build();
        if (dto.getParentId() != null) {
            dept.setParent(findById(dto.getParentId()));
        }
        return toDto(departmentRepository.save(dept));
    }

    // Met à jour le nom, la description et le parent d'un département existant
    @Transactional
    public DepartmentDto updateDepartment(Long id, DepartmentDto dto) {
        Department existing = findById(id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        if (dto.getParentId() != null) {
            existing.setParent(findById(dto.getParentId()));
        } else {
            existing.setParent(null);
        }
        return toDto(departmentRepository.save(existing));
    }

    // Supprime un département (soft delete : le met en inactif)
    @Transactional
    public void deleteDepartment(Long id) {
        Department dept = findById(id);
        dept.setActive(false);
        departmentRepository.save(dept);
    }

    // Restaure un département archivé (le remet actif)
    @Transactional
    public void restoreDepartment(Long id) {
        Department dept = findById(id);
        dept.setActive(true);
        departmentRepository.save(dept);
    }

    // Supprime définitivement un département (uniquement s'il est déjà archivé)
    @Transactional
    public void permanentDeleteDepartment(Long id) {
        Department dept = findById(id);
        if (dept.isActive()) {
            throw new RuntimeException("Seuls les départements archivés peuvent être supprimés définitivement.");
        }
        departmentRepository.deleteById(id);
    }

    // Helper interne : cherche un département par ID ou lance une exception
    private Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Département introuvable: " + id));
    }
}
