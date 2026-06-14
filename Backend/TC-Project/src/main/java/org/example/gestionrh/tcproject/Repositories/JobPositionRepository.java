package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.JobPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {
    List<JobPosition> findByDepartment_Id(Long departmentId);
}
