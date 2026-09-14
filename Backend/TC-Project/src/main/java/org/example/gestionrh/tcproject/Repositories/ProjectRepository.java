package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.Project;
import org.example.gestionrh.tcproject.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCreatedBy(User createdBy);
    List<Project> findByMembersContaining(User member);

    @Modifying
    @Query(value = "DELETE FROM project_members WHERE user_id = :userId", nativeQuery = true)
    void removeUserFromAllProjects(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Project p SET p.createdBy = NULL WHERE p.createdBy.id = :userId")
    void nullifyCreatedByUserId(@Param("userId") Long userId);
}
