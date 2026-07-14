package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.Project;
import org.example.gestionrh.tcproject.Entities.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {
    List<ProjectDocument> findByProject(Project project);
}
