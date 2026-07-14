package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.DocumentRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRecipientRepository extends JpaRepository<DocumentRecipient, Long> {
    List<DocumentRecipient> findByRecipientIdOrderByDocumentUploadDateDesc(Long recipientId);
    
    @Query("SELECT dr FROM DocumentRecipient dr WHERE dr.recipient.id = :recipientId AND dr.validationStatus IN (:statuses) ORDER BY dr.document.uploadDate DESC")
    List<DocumentRecipient> findByRecipientIdAndValidationStatusInOrderByDocumentUploadDateDesc(Long recipientId, List<org.example.gestionrh.tcproject.Entities.ValidationStatus> statuses);

    long countByRecipientIdAndIsReadFalse(Long recipientId);

    @Query("SELECT COUNT(dr) FROM DocumentRecipient dr WHERE dr.recipient.id = :recipientId AND dr.isRead = false AND dr.validationStatus IN (:statuses)")
    long countByRecipientIdAndIsReadFalseAndValidationStatusIn(Long recipientId, List<org.example.gestionrh.tcproject.Entities.ValidationStatus> statuses);
    
    Optional<DocumentRecipient> findByDocumentIdAndRecipientId(Long documentId, Long recipientId);

    @Query("SELECT dr FROM DocumentRecipient dr WHERE dr.document.uploader.department.id = :departmentId AND dr.validationStatus = :status ORDER BY dr.document.uploadDate DESC")
    List<DocumentRecipient> findByDocumentUploaderDepartmentIdAndValidationStatusOrderByDocumentUploadDateDesc(Long departmentId, org.example.gestionrh.tcproject.Entities.ValidationStatus status);

    @Query("SELECT dr FROM DocumentRecipient dr WHERE dr.document.uploader.id = :uploaderId AND dr.validationStatus = :status ORDER BY dr.document.uploadDate DESC")
    List<DocumentRecipient> findByDocumentUploaderIdAndValidationStatusOrderByDocumentUploadDateDesc(Long uploaderId, org.example.gestionrh.tcproject.Entities.ValidationStatus status);

    @Query("SELECT dr FROM DocumentRecipient dr WHERE dr.document.uploader.id = :uploaderId ORDER BY dr.document.uploadDate DESC")
    List<DocumentRecipient> findByDocumentUploaderIdOrderByDocumentUploadDateDesc(Long uploaderId);
}
