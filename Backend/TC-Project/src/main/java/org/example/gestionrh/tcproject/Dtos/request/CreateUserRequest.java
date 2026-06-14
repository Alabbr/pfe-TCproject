package org.example.gestionrh.tcproject.Dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.gestionrh.tcproject.Entities.Permission;
import org.example.gestionrh.tcproject.Entities.RoleName;

import java.util.Set;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Le prÃ©nom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    @NotNull(message = "Le rÃ´le est obligatoire")
    private RoleName role;

    @NotNull(message = "Le dÃ©partement est obligatoire")
    private Long departmentId;

    private Set<Permission> permissions;
}
