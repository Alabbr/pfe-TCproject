package org.example.gestionrh.tcproject.Services;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Entities.Permission;
import org.example.gestionrh.tcproject.Entities.RoleName;
import org.example.gestionrh.tcproject.Entities.RolePermission;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Repositories.RolePermissionRepository;
import org.example.gestionrh.tcproject.Repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;

    // Retourne toutes les configurations de permissions par rôle
    public List<RolePermission> getAllRolePermissions() {
        return rolePermissionRepository.findAll();
    }

    // Retourne les permissions configurées pour un rôle spécifique
    public RolePermission getRolePermissions(RoleName roleName) {
        return rolePermissionRepository.findById(roleName).orElse(null);
    }

    // Met à jour les permissions d'un rôle et propage les changements à tous les utilisateurs ayant ce rôle
    @Transactional
    public RolePermission updateRolePermissions(RoleName roleName, Set<Permission> newPermissions) {
        RolePermission rolePermission = rolePermissionRepository.findById(roleName)
                .orElse(RolePermission.builder().roleName(roleName).build());

        rolePermission.setPermissions(newPermissions);
        rolePermissionRepository.save(rolePermission);

        // Update all existing users with this role
        List<User> usersWithRole = userRepository.findByRole(roleName);

        for (User user : usersWithRole) {
            user.setPermissions(new java.util.HashSet<>(newPermissions));
            userRepository.save(user);
        }

        return rolePermission;
    }
}
