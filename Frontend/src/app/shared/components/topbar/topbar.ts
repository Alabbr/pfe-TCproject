import { Component, OnInit, HostListener, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthResponse } from '../../../core/models/auth.model';
import { HttpClient } from '@angular/common/http';

import { ProfileModalComponent } from '../profile-modal/profile-modal';
import { ProfileModalService } from '../../services/profile-modal.service';
import { SettingsModalComponent } from '../settings-modal/settings-modal';
import { ChatService, ChatMessageDto } from '../../../core/services/chat.service';
import { ThemeService } from '../../../core/services/theme.service';
import { NotificationService, SystemNotification } from '../../../core/services/notification.service';

interface SearchResult {
  label: string;
  description: string;
  icon: string;
  route: string;
  type: 'page' | 'user' | 'department' | 'project';
}

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, FormsModule, ProfileModalComponent, SettingsModalComponent],
  templateUrl: './topbar.html',
  styleUrl: './topbar.scss'
})
export class Topbar implements OnInit {
  isProfileMenuOpen = false;
  isProfileModalOpen = false;
  isSettingsModalOpen = false;
  isNotificationsOpen = false;
  currentUser: AuthResponse | null = null;
  unreadMessages: ChatMessageDto[] = [];
  systemNotifications: SystemNotification[] = [];
  unreadNotifsCount = 0;
  isDarkMode = false;

  // Search
  searchQuery = '';
  searchResults: SearchResult[] = [];
  isSearchOpen = false;
  selectedIndex = -1;

  private allPages: SearchResult[] = [];
  private cachedUsers: SearchResult[] = [];
  private cachedDepartments: SearchResult[] = [];
  private cachedProjects: SearchResult[] = [];
  private dataLoaded = false;

  constructor(
    private router: Router,
    private authService: AuthService,
    private profileModalService: ProfileModalService,
    private chatService: ChatService,
    private themeService: ThemeService,
    private notificationService: NotificationService,
    private http: HttpClient,
    private elRef: ElementRef
  ) {}

  @HostListener('document:keydown', ['$event'])
  handleKeyboardShortcut(event: KeyboardEvent) {
    if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
      event.preventDefault();
      this.openSearch();
    }
    if (event.key === 'Escape' && this.isSearchOpen) {
      this.closeSearch();
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (this.isSearchOpen && !this.elRef.nativeElement.querySelector('.search-bar')?.contains(event.target)) {
      this.closeSearch();
    }
  }

  ngOnInit() {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.buildPageIndex();
    });

    this.profileModalService.openModal$.subscribe(() => {
      this.openProfileModal();
    });

    this.chatService.unreadMessages$.subscribe(messages => {
      this.unreadMessages = messages;
    });

    this.notificationService.notifications$.subscribe(notifs => {
      this.systemNotifications = notifs.slice(0, 5);
    });

    this.notificationService.unreadCount$.subscribe(count => {
      this.unreadNotifsCount = count;
    });

    this.themeService.isDarkMode$.subscribe(isDark => {
      this.isDarkMode = isDark;
    });
  }

  // === Search Logic ===
  private buildPageIndex() {
    const role = this.currentUser?.role;
    const perms = this.currentUser?.permissions || [];
    const isSuperAdmin = role === 'SUPER_ADMIN' || role === 'DIRECTEUR_GENERAL';

    this.allPages = [
      { label: 'Dashboard', description: 'Tableau de bord principal', icon: 'dashboard', route: '/dashboard', type: 'page' },
      { label: 'Mes Tâches', description: 'Gestion de mes tâches assignées', icon: 'task', route: '/my-tasks', type: 'page' },
      { label: 'Structure & Départements', description: 'Organigramme de l\'entreprise', icon: 'corporate_fare', route: '/departments', type: 'page' },
      { label: 'Chatbot IA (RAG)', description: 'Assistant IA intelligent', icon: 'smart_toy', route: '/rag', type: 'page' },
    ];

    if (isSuperAdmin) {
      this.allPages.push(
        { label: 'Utilisateurs', description: 'Gestion des comptes utilisateurs', icon: 'group', route: '/users', type: 'page' },
        { label: 'Accès & Rôles', description: 'Gestion des rôles et permissions', icon: 'admin_panel_settings', route: '/roles', type: 'page' }
      );
    }

    if (isSuperAdmin || perms.includes('VIEW_DOCUMENTS') || perms.includes('MANAGE_DOCUMENTS')) {
      this.allPages.push(
        { label: 'Espace Documents', description: 'Boîte de réception et envoi de documents', icon: 'folder', route: '/documents', type: 'page' }
      );
    }

    if (isSuperAdmin || perms.includes('VIEW_PROJECTS') || perms.includes('MANAGE_PROJECTS')) {
      this.allPages.push(
        { label: 'Projets', description: 'Gestion des projets de l\'entreprise', icon: 'folder_special', route: '/projects', type: 'page' },
        { label: 'Mes Missions', description: 'Mes tâches de projet', icon: 'assignment', route: '/project-tasks', type: 'page' }
      );
    }

    if (isSuperAdmin || perms.includes('VIEW_CHAT')) {
      this.allPages.push(
        { label: 'Chat Inter-équipes', description: 'Messagerie d\'équipe', icon: 'chat', route: '/chat', type: 'page' }
      );
    }

    if (isSuperAdmin || perms.includes('VIEW_BILLING') || perms.includes('MANAGE_BILLING')) {
      this.allPages.push(
        { label: 'Dashboard Facturation', description: 'Statistiques et rapports de facturation', icon: 'receipt_long', route: '/billing-dashboard', type: 'page' }
      );
    }

    if (isSuperAdmin || perms.includes('MANAGE_KNOWLEDGE_BASE')) {
      this.allPages.push(
        { label: 'Base de Connaissances', description: 'Gestion des fichiers RAG', icon: 'library_books', route: '/knowledge-base', type: 'page' }
      );
    }
  }

  private loadSearchData() {
    if (this.dataLoaded) return;
    this.dataLoaded = true;

    this.http.get<any[]>('/api/users').subscribe({
      next: (users) => {
        this.cachedUsers = users.map(u => ({
          label: u.fullName || `${u.firstName} ${u.lastName}`,
          description: `${u.email} — ${u.roleName || u.role || ''}`,
          icon: 'person',
          route: '/users',
          type: 'user' as const
        }));
      },
      error: () => {}
    });

    this.http.get<any[]>('/api/departments').subscribe({
      next: (deps) => {
        this.cachedDepartments = deps.map(d => ({
          label: d.name,
          description: `${d.employeeCount || 0} employé(s)`,
          icon: 'corporate_fare',
          route: `/department-docs/${d.id}`,
          type: 'department' as const
        }));
      },
      error: () => {}
    });

    this.http.get<any[]>('/api/projects').subscribe({
      next: (projects) => {
        this.cachedProjects = projects.map(p => ({
          label: p.name,
          description: `Projet — ${p.status || ''}`,
          icon: 'folder_special',
          route: `/projects/${p.id}`,
          type: 'project' as const
        }));
      },
      error: () => {}
    });
  }

  openSearch() {
    this.isSearchOpen = true;
    this.selectedIndex = -1;
    this.loadSearchData();
    this.onSearchChange();
    setTimeout(() => {
      const input = this.elRef.nativeElement.querySelector('.search-input');
      if (input) input.focus();
    }, 50);
  }

  closeSearch() {
    this.isSearchOpen = false;
    this.searchQuery = '';
    this.searchResults = [];
    this.selectedIndex = -1;
  }

  onSearchChange() {
    const q = this.searchQuery.toLowerCase().trim();
    this.selectedIndex = -1;

    if (!q) {
      this.searchResults = [...this.allPages].slice(0, 8);
      return;
    }

    const allItems = [
      ...this.allPages,
      ...this.cachedUsers,
      ...this.cachedDepartments,
      ...this.cachedProjects
    ];

    this.searchResults = allItems.filter(item =>
      item.label.toLowerCase().includes(q) ||
      item.description.toLowerCase().includes(q)
    ).slice(0, 10);
  }

  onSearchKeydown(event: KeyboardEvent) {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.selectedIndex = Math.min(this.selectedIndex + 1, this.searchResults.length - 1);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.selectedIndex = Math.max(this.selectedIndex - 1, 0);
    } else if (event.key === 'Enter' && this.selectedIndex >= 0) {
      event.preventDefault();
      this.navigateTo(this.searchResults[this.selectedIndex]);
    }
  }

  navigateTo(result: SearchResult) {
    this.closeSearch();
    this.router.navigateByUrl(result.route);
  }

  getTypeLabel(type: string): string {
    switch (type) {
      case 'page': return 'Page';
      case 'user': return 'Utilisateur';
      case 'department': return 'Département';
      case 'project': return 'Projet';
      default: return '';
    }
  }

  // === Existing Methods ===
  toggleProfileMenu() {
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
    this.isNotificationsOpen = false;
  }

  toggleNotifications() {
    this.isNotificationsOpen = !this.isNotificationsOpen;
    this.isProfileMenuOpen = false;
  }

  closeAllMenus() {
    this.isProfileMenuOpen = false;
    this.isNotificationsOpen = false;
  }

  openProfileModal() {
    this.isProfileMenuOpen = false;
    this.isProfileModalOpen = true;
  }

  openSettingsModal() {
    this.isProfileMenuOpen = false;
    this.isSettingsModalOpen = true;
  }

  closeSettingsModal() {
    this.isSettingsModalOpen = false;
  }

  closeProfileModal() {
    this.isProfileModalOpen = false;
  }

  onProfileUpdated(updatedUser: any) {
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  goToChat(msg?: ChatMessageDto) {
    this.closeAllMenus();
    if (msg) {
      if (msg.channelId) {
        this.router.navigate(['/chat'], { queryParams: { type: 'channel', id: msg.channelId } });
      } else if (msg.senderId) {
        this.router.navigate(['/chat'], { queryParams: { type: 'dm', id: msg.senderId } });
      }
    } else {
      this.router.navigate(['/chat']);
    }
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }

  get totalUnreadCount(): number {
    return this.unreadMessages.length + this.unreadNotifsCount;
  }

  handleNotificationClick(notif: SystemNotification) {
    this.closeAllMenus();
    if (notif.actionUrl) {
      this.router.navigateByUrl(notif.actionUrl);
    }
  }

  markAllAsRead() {
    this.notificationService.markAllAsRead();
  }
}
