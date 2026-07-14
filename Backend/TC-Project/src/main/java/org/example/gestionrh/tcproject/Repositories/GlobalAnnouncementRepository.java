package org.example.gestionrh.tcproject.Repositories;

import org.example.gestionrh.tcproject.Entities.GlobalAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GlobalAnnouncementRepository extends JpaRepository<GlobalAnnouncement, Long> {
    
    @Query("SELECT a FROM GlobalAnnouncement a WHERE a.isActive = true ORDER BY a.createdAt DESC")
    List<GlobalAnnouncement> findActiveAnnouncements();
}
