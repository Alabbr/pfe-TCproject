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

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/archived")
    public ResponseEntity<List<UserResponse>> getArchivedUsers() {
        return ResponseEntity.ok(userService.getArchivedUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DIRECTEUR_GENERAL')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('DIRECTEUR_GENERAL')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> restoreUser(@PathVariable Long id) {
        userService.restoreUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> permanentDeleteUser(@PathVariable Long id) {
        userService.permanentDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<UserResponse>> getUsersByDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(userService.getUsersByDepartment(departmentId));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable Long id,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String address) {
        return ResponseEntity.ok(userService.updateProfile(id, phoneNumber, address));
    }

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

    @PutMapping("/{id}/assign-job")
    @PreAuthorize("hasRole('DIRECTEUR') or hasRole('SUPER_ADMIN') or hasRole('RESPONSABLE')")
    public ResponseEntity<UserResponse> assignJobPosition(
            @PathVariable Long id,
            @RequestParam Long jobPositionId) {
        return ResponseEntity.ok(userService.assignJobPosition(id, jobPositionId));
    }
}
