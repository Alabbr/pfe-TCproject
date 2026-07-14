package org.example.gestionrh.tcproject.Entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "role_permissions_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {
    @Id
    @Enumerated(EnumType.STRING)
    private RoleName roleName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_default_permissions", joinColumns = @JoinColumn(name = "role_name"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<Permission> permissions;
}
