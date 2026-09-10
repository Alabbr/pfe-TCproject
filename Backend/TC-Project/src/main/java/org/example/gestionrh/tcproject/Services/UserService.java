package org.example.gestionrh.tcproject.Services;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Entities.Department;
import org.example.gestionrh.tcproject.Entities.User;
import org.example.gestionrh.tcproject.Dtos.request.CreateUserRequest;
import org.example.gestionrh.tcproject.Dtos.request.UpdateUserRequest;
import org.example.gestionrh.tcproject.Dtos.response.UserResponse;
import org.example.gestionrh.tcproject.Repositories.DepartmentRepository;
import org.example.gestionrh.tcproject.Repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final org.example.gestionrh.tcproject.Repositories.JobPositionRepository jobPositionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Retourne tous les utilisateurs actifs
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .filter(User::isActive)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Retourne tous les utilisateurs archivés (inactifs)
    public List<UserResponse> getArchivedUsers() {
        return userRepository.findAll()
                .stream()
                .filter(user -> !user.isActive())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Retourne un utilisateur par son ID
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + id));
        return toResponse(user);
    }

    // Crée un nouvel utilisateur, envoie un email au chef et un email de bienvenue
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé: " + request.getEmail());
        }

        Department dept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Département introuvable: " + request.getDepartmentId()));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .department(dept)
                .permissions(request.getPermissions())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        // Si c'est un employé, on notifie le chef de département
        if (request.getRole() == org.example.gestionrh.tcproject.Entities.RoleName.EMPLOYE) {
            userRepository.findByDepartment_Id(dept.getId()).stream()
                    .filter(u -> u.getRole() == org.example.gestionrh.tcproject.Entities.RoleName.DIRECTEUR || u.getRole() == org.example.gestionrh.tcproject.Entities.RoleName.RESPONSABLE)
                    .findFirst()
                    .ifPresent(chef -> {
                        emailService.sendChefNotificationEmail(chef.getEmail(), chef.getFullName(), savedUser.getFullName(), dept.getName());
                    });
        }

        // Email de bienvenue au nouvel utilisateur
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName(), request.getEmail(), request.getPassword());

        return toResponse(savedUser);
    }

    // Met à jour les informations d'un utilisateur existant (nom, rôle, département, permissions, mot de passe)
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + id));

        Department dept = null;
        if (request.getDepartmentId() != null) {
            dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Département introuvable: " + request.getDepartmentId()));
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole());
        user.setDepartment(dept);
        user.setPermissions(request.getPermissions());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return toResponse(userRepository.save(user));
    }

    // Supprime un utilisateur (soft delete : le met en inactif)
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + id));
        user.setActive(false); // Soft delete
        userRepository.save(user);
    }

    // Restaure un utilisateur archivé (le remet actif)
    @Transactional
    public void restoreUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + id));
        user.setActive(true);
        userRepository.save(user);
    }

    // Supprime définitivement un utilisateur (uniquement s'il est déjà archivé)
    @Transactional
    public void permanentDeleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + id));
        if (user.isActive()) {
            throw new RuntimeException("Seuls les utilisateurs archivés peuvent être supprimés définitivement.");
        }
        userRepository.deleteById(id);
    }

    // Retourne tous les utilisateurs d'un département spécifique
    public List<UserResponse> getUsersByDepartment(Long departmentId) {
        return userRepository.findByDepartment_Id(departmentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Met à jour le téléphone et l'adresse d'un utilisateur (profil personnel)
    @Transactional
    public UserResponse updateProfile(Long id, String phoneNumber, String address) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + id));
        user.setPhoneNumber(phoneNumber);
        user.setAddress(address);
        return toResponse(userRepository.save(user));
    }

    // Met à jour la photo de profil d'un utilisateur avec l'URL Cloudinary
    @Transactional
    public UserResponse uploadProfilePicture(Long id, String imageUrl) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + id));
        user.setProfilePictureUrl(imageUrl);
        return toResponse(userRepository.save(user));
    }

    // Affecte un poste de travail à un utilisateur
    @Transactional
    public UserResponse assignJobPosition(Long userId, Long jobPositionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + userId));
        
        org.example.gestionrh.tcproject.Entities.JobPosition position = jobPositionRepository.findById(jobPositionId)
                .orElseThrow(() -> new RuntimeException("Poste introuvable: " + jobPositionId));
        
        user.setJobPosition(position);
        return toResponse(userRepository.save(user));
    }

    /**
     * Update individual user permissions.
     * Chef de Département can only update employees in their own department,
     * and only a subset of permissions (no admin-level permissions).
     */
    @Transactional
    public UserResponse updateUserPermissions(Long userId, java.util.Set<org.example.gestionrh.tcproject.Entities.Permission> permissions, User currentUser) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + userId));

        boolean isSuperAdmin = currentUser.getRole() == org.example.gestionrh.tcproject.Entities.RoleName.SUPER_ADMIN
                || currentUser.getRole() == org.example.gestionrh.tcproject.Entities.RoleName.DIRECTEUR_GENERAL;

        if (!isSuperAdmin) {
            // Chef de Département: validate same department
            if (currentUser.getDepartment() == null || targetUser.getDepartment() == null
                    || !currentUser.getDepartment().getId().equals(targetUser.getDepartment().getId())) {
                throw new RuntimeException("Vous ne pouvez modifier que les employés de votre département.");
            }

            // Chef cannot modify other Chefs or admins
            if (targetUser.getRole() == org.example.gestionrh.tcproject.Entities.RoleName.SUPER_ADMIN
                    || targetUser.getRole() == org.example.gestionrh.tcproject.Entities.RoleName.DIRECTEUR_GENERAL
                    || targetUser.getRole() == org.example.gestionrh.tcproject.Entities.RoleName.DIRECTEUR) {
                throw new RuntimeException("Vous ne pouvez pas modifier les permissions d'un chef ou administrateur.");
            }

            // Chef can only set allowed permissions (no admin-level ones)
            java.util.Set<org.example.gestionrh.tcproject.Entities.Permission> chefAllowed = java.util.Set.of(
                    org.example.gestionrh.tcproject.Entities.Permission.VIEW_DASHBOARD,
                    org.example.gestionrh.tcproject.Entities.Permission.VIEW_DOCUMENTS,
                    org.example.gestionrh.tcproject.Entities.Permission.MANAGE_DOCUMENTS,
                    org.example.gestionrh.tcproject.Entities.Permission.USE_CHATBOT,
                    org.example.gestionrh.tcproject.Entities.Permission.VIEW_CHAT,
                    org.example.gestionrh.tcproject.Entities.Permission.VIEW_BILLING,
                    org.example.gestionrh.tcproject.Entities.Permission.VIEW_PROJECTS,
                    org.example.gestionrh.tcproject.Entities.Permission.MANAGE_PROJECTS
            );
            // Filter out any non-allowed permissions
            permissions.removeIf(p -> !chefAllowed.contains(p));
        }

        targetUser.setPermissions(new java.util.HashSet<>(permissions));
        return toResponse(userRepository.save(targetUser));
    }

    // Convertit une entité User en DTO UserResponse pour la réponse API
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .profilePictureUrl(user.getProfilePictureUrl())
                .jobPositionId(user.getJobPosition() != null ? user.getJobPosition().getId() : null)
                .jobPositionName(user.getJobPosition() != null ? user.getJobPosition().getName() : null)
                .permissions(user.getPermissions())
                .isActive(user.isActive())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
