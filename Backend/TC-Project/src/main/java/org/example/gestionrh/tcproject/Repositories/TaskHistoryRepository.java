package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.Task;
import org.example.gestionrh.tcproject.Entities.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {
    List<TaskHistory> findByTaskOrderByChangedAtDesc(Task task);
}
