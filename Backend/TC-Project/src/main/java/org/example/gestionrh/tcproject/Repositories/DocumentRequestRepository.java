package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.DocumentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long> {
    List<DocumentRequest> findByTargetDepartmentIdOrderByCreatedAtDesc(Long departmentId);
    List<DocumentRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    @Modifying
    @Query("DELETE FROM DocumentRequest dr WHERE dr.requester.id = :userId")
    void deleteAllByRequesterId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE DocumentRequest dr SET dr.assignedTo = NULL WHERE dr.assignedTo.id = :userId")
    void nullifyAssignedToByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE DocumentRequest dr SET dr.delegatedTo = NULL WHERE dr.delegatedTo.id = :userId")
    void nullifyDelegatedToByUserId(@Param("userId") Long userId);
}
