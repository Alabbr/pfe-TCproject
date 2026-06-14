package org.example.gestionrh.tcproject.Dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.gestionrh.tcproject.Entities.Permission;
import org.example.gestionrh.tcproject.Entities.RoleName;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private RoleName role;
    private String departmentName;
    private Long departmentId;
    
    private String phoneNumber;
    private String address;
    private String profilePictureUrl;
    private Long jobPositionId;
    private String jobPositionName;

    private Set<Permission> permissions;
    private boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
}
