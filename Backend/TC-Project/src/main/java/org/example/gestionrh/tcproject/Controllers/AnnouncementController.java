package org.example.gestionrh.tcproject.Controllers;

import org.example.gestionrh.tcproject.Entities.GlobalAnnouncement;
import org.example.gestionrh.tcproject.Repositories.GlobalAnnouncementRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final GlobalAnnouncementRepository announcementRepo;

    public AnnouncementController(GlobalAnnouncementRepository announcementRepo) {
        this.announcementRepo = announcementRepo;
    }

    // Retourne la liste des annonces globales actives
    @GetMapping("/active")
    public ResponseEntity<List<GlobalAnnouncement>> getActiveAnnouncements() {
        return ResponseEntity.ok(announcementRepo.findActiveAnnouncements());
    }
}
