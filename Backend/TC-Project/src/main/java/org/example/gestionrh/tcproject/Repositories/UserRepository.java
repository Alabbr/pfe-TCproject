package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.RoleName;
import org.example.gestionrh.tcproject.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByDepartment_Id(Long departmentId);
    List<User> findByRole(RoleName role);
    List<User> findByIsActiveTrue();
    List<User> findByIsActiveFalse();
    List<User> findTop5ByIsActiveTrueOrderByLastLoginDesc();
}
