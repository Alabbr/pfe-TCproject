package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.Task;
import org.example.gestionrh.tcproject.Entities.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {
    List<TaskHistory> findByTaskOrderByChangedAtDesc(Task task);

    @Modifying
    @Query("DELETE FROM TaskHistory th WHERE th.changedBy.id = :userId")
    void deleteAllByChangedById(@Param("userId") Long userId);
}
