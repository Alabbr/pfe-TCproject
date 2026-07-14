package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.TransferRequest;
import org.example.gestionrh.tcproject.Entities.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {
    List<TransferRequest> findByStatus(ValidationStatus status);
    List<TransferRequest> findByRequestedBy_Id(Long requestedById);
    List<TransferRequest> findByEmployee_Id(Long employeeId);
}
