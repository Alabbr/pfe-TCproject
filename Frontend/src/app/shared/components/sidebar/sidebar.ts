import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ProfileModalService } from '../../services/profile-modal.service';

interface NavGroup {
  name: string;
  items: NavItem[];
}

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class Sidebar {
  currentUser: any;
  navGroups: NavGroup[] = [];

  constructor(
    private authService: AuthService,
    private profileModalService: ProfileModalService
  ) {
    this.authService.currentUser$.subscribe((user: any) => {
      this.currentUser = user;
      this.buildNav();
    });
  }

  private buildNav() {
    const isSuperAdmin = this.currentUser?.role === 'SUPER_ADMIN';

    this.navGroups = [
      {
        name: 'Gestion',
        items: [
          { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
          ...(isSuperAdmin ? [{ label: 'Utilisateurs', icon: 'group', route: '/users' }] : []),
          { label: 'Structure & Départements', icon: 'corporate_fare', route: '/departments' }
        ]
      },
      {
        name: 'Documents',
        items: [
          { label: 'Espace Documents', icon: 'folder', route: '/documents' }
        ]
      },
      {
        name: 'IA & Communication',
        items: [
          { label: 'Chat Inter-équipes', icon: 'chat', route: '/chat' },
          { label: 'Chatbot RAG', icon: 'smart_toy', route: '/rag' }
        ]
      },
      {
        name: 'Finance',
        items: [
          { label: 'Facturation IA', icon: 'receipt_long', route: '/billing' }
        ]
      }
    ];
  }

  openProfile() {
    this.profileModalService.open();
  }
}
