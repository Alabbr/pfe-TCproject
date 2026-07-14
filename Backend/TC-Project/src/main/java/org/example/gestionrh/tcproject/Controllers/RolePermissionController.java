package org.example.gestionrh.tcproject.Controllers;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Entities.Permission;
import org.example.gestionrh.tcproject.Entities.RoleName;
import org.example.gestionrh.tcproject.Entities.RolePermission;
import org.example.gestionrh.tcproject.Services.RolePermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    // Retourne la liste complète de toutes les configurations de permissions par rôle
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_DIRECTEUR_GENERAL') or hasAuthority('ROLE_DIRECTEUR') or hasAuthority('ROLE_RESPONSABLE')")
    public ResponseEntity<List<RolePermission>> getAllRolePermissions() {
        return ResponseEntity.ok(rolePermissionService.getAllRolePermissions());
    }

    // Retourne les permissions spécifiques configurées pour un rôle donné
    @GetMapping("/{roleName}/permissions")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_DIRECTEUR_GENERAL') or hasAuthority('ROLE_DIRECTEUR') or hasAuthority('ROLE_RESPONSABLE')")
    public ResponseEntity<RolePermission> getRolePermissions(@PathVariable RoleName roleName) {
        return ResponseEntity.ok(rolePermissionService.getRolePermissions(roleName));
    }

    // Met à jour les permissions pour un rôle spécifique (restreint aux Super Admin et DG)
    @PutMapping("/{roleName}/permissions")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN') or hasAuthority('ROLE_DIRECTEUR_GENERAL')")
    public ResponseEntity<RolePermission> updateRolePermissions(
            @PathVariable RoleName roleName,
            @RequestBody Set<Permission> newPermissions) {
        return ResponseEntity.ok(rolePermissionService.updateRolePermissions(roleName, newPermissions));
    }
}
