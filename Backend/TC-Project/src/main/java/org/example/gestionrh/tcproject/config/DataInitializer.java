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

            // Initialize Admin User
            if (!userRepository.existsByEmail("admin@tunisie-clearing.tn")) {
                User adminUser = User.builder()
                        .firstName("Super")
                        .lastName("Admin")
                        .email("admin@tunisie-clearing.tn")
                        .password(passwordEncoder.encode("Admin@TC2024"))
                        .role(RoleName.SUPER_ADMIN)
                        .department(ops)
                        .permissions(Set.of(Permission.values()))
                        .isActive(true)
                        .build();
                userRepository.save(adminUser);
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
