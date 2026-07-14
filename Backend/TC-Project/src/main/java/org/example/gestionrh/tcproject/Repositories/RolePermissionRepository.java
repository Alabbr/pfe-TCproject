package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.RoleName;
import org.example.gestionrh.tcproject.Entities.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RoleName> {
}
