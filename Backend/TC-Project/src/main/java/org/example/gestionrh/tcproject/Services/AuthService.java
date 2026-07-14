package org.example.gestionrh.tcproject.Services;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Dtos.request.LoginRequest;
import org.example.gestionrh.tcproject.Dtos.response.AuthResponse;
import org.example.gestionrh.tcproject.Repositories.UserRepository;
import org.example.gestionrh.tcproject.Repositories.RolePermissionRepository;
import org.example.gestionrh.tcproject.Entities.RolePermission;
import org.example.gestionrh.tcproject.Entities.Permission;
import org.example.gestionrh.tcproject.Entities.RoleName;
import org.example.gestionrh.tcproject.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    // Authentifie l'utilisateur avec email/mot de passe, met à jour lastLogin et génère un token JWT
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);

        return buildAuthResponse(user, token);
    }

    /**
     * Called by GET /api/auth/me to return fresh user data without re-authentication.
     * The existing JWT token is kept — only permissions/role are refreshed from DB.
     */
    public AuthResponse refreshCurrentUser(User user, String existingToken) {
        return buildAuthResponse(user, existingToken);
    }

    /**
     * Compute the effective permissions for a user.
     * Priority: role_permissions_config > user_permissions (fallback).
     * Super Admin and DG always get ALL permissions.
     */
    public Set<Permission> getEffectivePermissions(User user) {
        Set<Permission> effective = new HashSet<>();

        // Super Admin and Directeur General always get ALL permissions
        if (user.getRole() == RoleName.SUPER_ADMIN || user.getRole() == RoleName.DIRECTEUR_GENERAL) {
            for (Permission p : Permission.values()) {
                effective.add(p);
            }
            return effective;
        }

        // Per-user permissions (set by Chef or Admin) take priority
        if (user.getPermissions() != null && !user.getPermissions().isEmpty()) {
            effective.addAll(user.getPermissions());
            return effective;
        }

        // Fallback: role-level permissions from role_permissions_config
        RolePermission rolePerm = rolePermissionRepository.findById(user.getRole()).orElse(null);
        if (rolePerm != null && rolePerm.getPermissions() != null) {
            effective.addAll(rolePerm.getPermissions());
        }

        return effective;
    }

    // Construit l'objet AuthResponse avec toutes les infos de l'utilisateur + ses permissions effectives
    private AuthResponse buildAuthResponse(User user, String token) {
        Set<Permission> effectivePermissions = getEffectivePermissions(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .permissions(effectivePermissions)
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();
    }
}
