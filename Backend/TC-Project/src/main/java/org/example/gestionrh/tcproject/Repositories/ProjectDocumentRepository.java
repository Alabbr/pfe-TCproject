package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.Project;
import org.example.gestionrh.tcproject.Entities.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {
    List<ProjectDocument> findByProject(Project project);

    @Modifying
    @Query("DELETE FROM ProjectDocument pd WHERE pd.uploadedBy.id = :userId")
    void deleteAllByUploadedById(@Param("userId") Long userId);
}
