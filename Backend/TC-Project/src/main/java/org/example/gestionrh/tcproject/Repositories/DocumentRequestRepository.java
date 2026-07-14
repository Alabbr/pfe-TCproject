package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.DocumentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long> {
    List<DocumentRequest> findByTargetDepartmentIdOrderByCreatedAtDesc(Long departmentId);
    List<DocumentRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
}
