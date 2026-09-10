package org.example.gestionrh.tcproject.config;

import lombok.RequiredArgsConstructor;
import org.example.gestionrh.tcproject.Entities.*;
import org.example.gestionrh.tcproject.Repositories.DepartmentRepository;
import org.example.gestionrh.tcproject.Repositories.JobPositionRepository;
import org.example.gestionrh.tcproject.Repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final JobPositionRepository jobPositionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (departmentRepository.count() == 0) {
            Department ops = createDepartmentWithGestions("Département des Opérations", 
                "Gestion des opérations", 
                List.of("Conservation", "Reglement/ Livraison", "Gestion des opérations sur titres", "Gestion des Registres des propriétaires", "Contrôle des opérations"));

            Department it = createDepartmentWithGestions("Département Technologie de l'Information", 
                "DSI", 
                List.of("Support utilisateurs Help desk", "Assistance et support", "Développement informatique", "Etude et veille technologique"));

            Department daf = createDepartmentWithGestions("Département Administratif & Financier", 
                "DAF", 
                List.of("Ressources humaines", "Comptabilite", "Facturation / Recouvrement", "Achat et paiement"));

            Department etudes = createDepartmentWithGestions("Département Études & Analyses", 
                "Sous supervision directe du DG", 
                List.of("Codification et Admission", "Statistiques, Analyse et communication", "Etude et projets", "Gestion des courbes des taux et des LEI"));

            Department dg = createDepartmentWithGestions("Direction Générale", 
                "Direction Générale et Sécurité", 
                List.of("Directeur Général", "Directeur Général Adjoint", "Sécurité", "Audit Interne"));

            // Initialize Admin User
            User adminUser = userRepository.findByEmail("alabbr55@gmail.com").orElse(null);
            if (adminUser == null) {
                // Also check old email in case of migration
                adminUser = userRepository.findByEmail("admin@tunisie-clearing.tn").orElse(null);
                if (adminUser != null) {
                    adminUser.setEmail("alabbr55@gmail.com");
                    adminUser.setPassword(passwordEncoder.encode("123456"));
                    adminUser.setRole(RoleName.SUPER_ADMIN);
                    adminUser.setPermissions(Set.of(Permission.values()));
                    userRepository.save(adminUser);
                } else {
                    adminUser = User.builder()
                            .firstName("Super")
                            .lastName("Admin")
                            .email("alabbr55@gmail.com")
                            .password(passwordEncoder.encode("123456"))
                            .role(RoleName.SUPER_ADMIN)
                            .department(ops)
                            .permissions(Set.of(Permission.values()))
                            .isActive(true)
                            .build();
                    userRepository.save(adminUser);
                }
            } else {
                // Ensure it is always SUPER_ADMIN with correct password
                adminUser.setRole(RoleName.SUPER_ADMIN);
                if (adminUser.getPassword() == null || adminUser.getPassword().isEmpty()) {
                    adminUser.setPassword(passwordEncoder.encode("123456"));
                }
                adminUser.setPermissions(Set.of(Permission.values()));
                userRepository.save(adminUser);
            }
            
            // Initialize Chef Departement User
            if (!userRepository.existsByEmail("chef@tunisie-clearing.tn")) {
                User chefUser = User.builder()
                        .firstName("Chef")
                        .lastName("Ops")
                        .email("chef@tunisie-clearing.tn")
                        .password(passwordEncoder.encode("Chef@TC2024"))
                        .role(RoleName.DIRECTEUR) // DIRECTEUR maps to Chef de Département
                        .department(ops)
                        .permissions(Set.of(Permission.MANAGE_DOCUMENTS, Permission.VIEW_DASHBOARD))
                        .isActive(true)
                        .build();
                userRepository.save(chefUser);
            }
        }
    }

    private Department createDepartmentWithGestions(String name, String desc, List<String> gestions) {
        Department dept = departmentRepository.save(Department.builder()
                .name(name)
                .description(desc)
                .isActive(true)
                .build());

        for (String gestionName : gestions) {
            jobPositionRepository.save(JobPosition.builder()
                    .name(gestionName)
                    .department(dept)
                    .build());
        }
        return dept;
    }
}
