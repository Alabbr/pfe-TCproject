package org.example.gestionrh.tcproject.Controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Dtos.request.CreateUserRequest;
import org.example.gestionrh.tcproject.Dtos.request.UpdateUserRequest;
import org.example.gestionrh.tcproject.Dtos.response.UserResponse;
import org.example.gestionrh.tcproject.Services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.example.gestionrh.tcproject.Services.CloudinaryService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    // Retourne la liste de tous les utilisateurs actifs
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Retourne la liste de tous les utilisateurs archivés
    @GetMapping("/archived")
    public ResponseEntity<List<UserResponse>> getArchivedUsers() {
        return ResponseEntity.ok(userService.getArchivedUsers());
    }

    // Retourne les informations d'un utilisateur par son ID
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // Crée un nouvel utilisateur (avec envoi d'email de bienvenue)
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DIRECTEUR_GENERAL') or (hasRole('DIRECTEUR') and principal.department != null and principal.department.name == 'Direction Générale')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    // Met à jour les informations d'un utilisateur existant
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DIRECTEUR_GENERAL') or (hasRole('DIRECTEUR') and principal.department != null and principal.department.name == 'Direction Générale')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // Supprime un utilisateur de manière logique (archivage)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DIRECTEUR_GENERAL') or (hasRole('DIRECTEUR') and principal.department != null and principal.department.name == 'Direction Générale')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Restaure un utilisateur précédemment archivé
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DIRECTEUR_GENERAL') or (hasRole('DIRECTEUR') and principal.department != null and principal.department.name == 'Direction Générale')")
    public ResponseEntity<Void> restoreUser(@PathVariable Long id) {
        userService.restoreUser(id);
        return ResponseEntity.noContent().build();
    }

    // Supprime définitivement un utilisateur archivé
    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DIRECTEUR_GENERAL') or (hasRole('DIRECTEUR') and principal.department != null and principal.department.name == 'Direction Générale')")
    public ResponseEntity<Void> permanentDeleteUser(@PathVariable Long id) {
        userService.permanentDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Retourne la liste des utilisateurs appartenant à un département spécifique
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<UserResponse>> getUsersByDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(userService.getUsersByDepartment(departmentId));
    }

    // Met à jour le profil personnel de l'utilisateur (téléphone, adresse)
    @PutMapping("/{id}/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable Long id,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String address) {
        return ResponseEntity.ok(userService.updateProfile(id, phoneNumber, address));
    }

    // Upload et met à jour la photo de profil de l'utilisateur via Cloudinary
    @PostMapping("/{id}/profile-picture")
    public ResponseEntity<UserResponse> uploadProfilePicture(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String imageUrl = cloudinaryService.uploadImage(file);
            return ResponseEntity.ok(userService.uploadProfilePicture(id, imageUrl));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Affecte un poste de travail à un utilisateur
    @PutMapping("/{id}/assign-job")
    @PreAuthorize("hasRole('DIRECTEUR') or hasRole('SUPER_ADMIN') or hasRole('DIRECTEUR_GENERAL') or hasRole('RESPONSABLE')")
    public ResponseEntity<UserResponse> assignJobPosition(
            @PathVariable Long id,
            @RequestParam Long jobPositionId) {
        return ResponseEntity.ok(userService.assignJobPosition(id, jobPositionId));
    }

    /**
     * Met à jour les permissions spécifiques d'un utilisateur.
     * - Super Admin / DG : peut tout modifier
     * - Chef de Département (DIRECTEUR) : peut modifier les permissions de ses employés
     */
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DIRECTEUR_GENERAL') or hasRole('DIRECTEUR')")
    public ResponseEntity<UserResponse> updateUserPermissions(
            @PathVariable Long id,
            @RequestBody java.util.Set<org.example.gestionrh.tcproject.Entities.Permission> permissions,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.example.gestionrh.tcproject.Entities.User currentUser) {
        return ResponseEntity.ok(userService.updateUserPermissions(id, permissions, currentUser));
    }
}
