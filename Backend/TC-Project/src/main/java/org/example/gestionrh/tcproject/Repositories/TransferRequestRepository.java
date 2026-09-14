package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.TransferRequest;
import org.example.gestionrh.tcproject.Entities.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {
    List<TransferRequest> findByStatus(ValidationStatus status);
    List<TransferRequest> findByRequestedBy_Id(Long requestedById);
    List<TransferRequest> findByEmployee_Id(Long employeeId);

    @Modifying
    @Query("DELETE FROM TransferRequest tr WHERE tr.employee.id = :userId OR tr.requestedBy.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE TransferRequest tr SET tr.decidedBy = NULL WHERE tr.decidedBy.id = :userId")
    void nullifyDecidedByUserId(@Param("userId") Long userId);
}
