package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.Project;
import org.example.gestionrh.tcproject.Entities.Task;
import org.example.gestionrh.tcproject.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProject(Project project);
    List<Task> findByAssignee(User assignee);
    List<Task> findByReporter(User reporter);

    @Modifying
    @Query("UPDATE Task t SET t.assignee = NULL WHERE t.assignee.id = :userId")
    void nullifyAssigneeByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Task t SET t.reporter = NULL WHERE t.reporter.id = :userId")
    void nullifyReporterByUserId(@Param("userId") Long userId);
}
