package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByIsPublicTrueOrderByUploadDateDesc();
    
    List<Document> findByTargetDepartmentIdAndIsDepartmentPublicTrueOrderByUploadDateDesc(Long departmentId);
    
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploader.id = :userId")
    long countByUploaderId(Long userId);
    
    @Query("SELECT COUNT(d) FROM Document d WHERE d.isPublic = true")
    long countPublicDocuments();

    @Modifying
    @Query("DELETE FROM Document d WHERE d.uploader.id = :userId")
    void deleteAllByUploaderId(@Param("userId") Long userId);
}
