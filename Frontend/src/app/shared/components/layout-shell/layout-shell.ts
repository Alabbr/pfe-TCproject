import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Sidebar } from '../sidebar/sidebar';
import { Topbar } from '../topbar/topbar';
import { AnnouncementBannerComponent } from '../announcement-banner/announcement-banner';

@Component({
  selector: 'app-layout-shell',
  standalone: true,
  imports: [CommonModule, RouterModule, Sidebar, Topbar, AnnouncementBannerComponent],
  templateUrl: './layout-shell.html',
  styleUrl: './layout-shell.scss'
})
export class LayoutShell {}
