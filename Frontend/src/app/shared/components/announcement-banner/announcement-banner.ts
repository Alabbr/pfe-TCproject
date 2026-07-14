import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AnnouncementService, GlobalAnnouncement } from '../../../core/services/announcement.service';

@Component({
  selector: 'app-announcement-banner',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './announcement-banner.html',
  styleUrls: ['./announcement-banner.scss']
})
export class AnnouncementBannerComponent implements OnInit {
  announcements: GlobalAnnouncement[] = [];
  currentIndex = 0;
  isVisible = false;

  constructor(private announcementService: AnnouncementService) {}

  ngOnInit() {
    this.announcementService.getActiveAnnouncements().subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          const closedIds = JSON.parse(localStorage.getItem('closedAnnouncements') || '[]');
          this.announcements = data.filter(a => !closedIds.includes(a.id));

          if (this.announcements.length > 0) {
            this.isVisible = true;
            this.startRotation();
            
            // Auto close after 15 seconds
            setTimeout(() => {
              this.isVisible = false;
            }, 15000);
          }
        }
      },
      error: (err) => console.error('Failed to load announcements', err)
    });
  }

  startRotation() {
    if (this.announcements.length > 1) {
      setInterval(() => {
        this.currentIndex = (this.currentIndex + 1) % this.announcements.length;
      }, 10000); // Rotate every 10 seconds
    }
  }

  closeBanner() {
    this.isVisible = false;
    
    // Mark current announcement as closed in localStorage
    if (this.announcements.length > 0) {
      const currentAnn = this.announcements[this.currentIndex];
      const closedIds = JSON.parse(localStorage.getItem('closedAnnouncements') || '[]');
      if (!closedIds.includes(currentAnn.id)) {
        closedIds.push(currentAnn.id);
        localStorage.setItem('closedAnnouncements', JSON.stringify(closedIds));
      }
    }
  }
}
