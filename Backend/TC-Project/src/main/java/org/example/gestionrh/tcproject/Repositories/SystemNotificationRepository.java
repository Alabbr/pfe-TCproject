package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.SystemNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    List<SystemNotification> findByUserIdOrUserIsNullOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("UPDATE SystemNotification n SET n.isRead = true WHERE (n.user.id = :userId OR n.user IS NULL) AND n.isRead = false")
    void markAllAsRead(Long userId);

    @Modifying
    @Query("DELETE FROM SystemNotification sn WHERE sn.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
