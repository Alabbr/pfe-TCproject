package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.Project;
import org.example.gestionrh.tcproject.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCreatedBy(User createdBy);
    List<Project> findByMembersContaining(User member);
}
