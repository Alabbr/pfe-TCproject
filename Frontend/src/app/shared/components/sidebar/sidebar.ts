import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ProfileModalService } from '../../services/profile-modal.service';
import { DocumentService } from '../../../core/services/document.service';
import { DepartmentService } from '../../../core/services/department.service';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../../services/toast';
import { RoleFormatPipe } from '../../pipes/role-format.pipe';

interface NavGroup {
  name: string;
  items: NavItem[];
  isAccordion?: boolean;
}

interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: number;
  isSubItem?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, RoleFormatPipe],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class Sidebar implements OnInit, OnDestroy {
  currentUser: any;
  navGroups: NavGroup[] = [];
  departments: any[] = [];
  unreadDocsCount = -1;
  pendingRequestsCount = -1;
  assignedTasksCount = -1;
  isDocsExpanded = false;
  isInterimActive = false;
  interimDelegation: any = null;
  private pollInterval: any;

  constructor(
    private authService: AuthService,
    private profileModalService: ProfileModalService,
    private documentService: DocumentService,
    private departmentService: DepartmentService,
    private http: HttpClient,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    // Refresh permissions from backend on every page load/refresh
    this.authService.refreshCurrentUser();

    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      
      this.departmentService.getDepartments().subscribe({
        next: (deps) => {
          this.departments = deps;
          this.checkInterimStatus();
          this.buildNav();
        },
        error: (err) => {
          console.error('Failed to load departments', err);
          this.buildNav();
        }
      });

      if (this.currentUser) {
        this.fetchUnreadDocs();
      }
    });

    this.pollInterval = setInterval(() => {
      if (this.currentUser) {
        this.fetchUnreadDocs();
      }
    }, 30000); // Poll every 30 seconds
  }

  ngOnDestroy() {
    if (this.pollInterval) {
      clearInterval(this.pollInterval);
    }
  }

  private checkInterimStatus() {
    if (!this.currentUser) return;
    this.http.get<any[]>('http://localhost:8080/api/interim/my-delegation').subscribe({
      next: (dels) => {
        if (dels && dels.length > 0) {
          this.isInterimActive = true;
          this.interimDelegation = dels[0];
        } else {
          this.isInterimActive = false;
          this.interimDelegation = null;
        }
        this.buildNav();
      },
      error: () => {
        this.isInterimActive = false;
        this.interimDelegation = null;
      }
    });
  }

  private fetchUnreadDocs() {
    this.documentService.getStats().subscribe({
      next: (stats) => {
        // 1. Documents in Inbox (including pending validation docs)
        const newCount = stats.unreadCount + (stats.pendingCount || 0);
        if (this.unreadDocsCount !== -1 && newCount > this.unreadDocsCount) {
          this.playNotificationSound();
          this.toastService.success('Nouveau document', 'Vous avez reçu un nouveau document dans votre boîte de réception.');
        }
        this.unreadDocsCount = newCount;
        
        // 2. Pending Document Requests (Chef)
        const newPendingRequests = stats.pendingRequestsCount || 0;
        if (this.pendingRequestsCount !== -1 && newPendingRequests > this.pendingRequestsCount) {
          this.playNotificationSound();
          this.toastService.success('Nouvelle demande', 'Une nouvelle demande de document a été soumise à votre département.');
        }
        this.pendingRequestsCount = newPendingRequests;

        // 3. Assigned Tasks (HR Employee)
        const newAssignedTasks = stats.assignedTasksCount || 0;
        if (this.assignedTasksCount !== -1 && newAssignedTasks > this.assignedTasksCount) {
          this.playNotificationSound();
          this.toastService.success('Nouvelle tâche', 'Une nouvelle tâche de génération de document vous a été assignée.');
        }
        this.assignedTasksCount = newAssignedTasks;
        
        // Update the badge without rebuilding the entire nav array to avoid UI flicker
        const docGroup = this.navGroups.find(g => g.name === 'Documents');
        if (docGroup) {
          const docItem = docGroup.items.find(i => i.route === '/documents');
          if (docItem) {
            docItem.badge = this.unreadDocsCount;
          }
        }
        
        const gestionGroup = this.navGroups.find(g => g.name === 'Gestion');
        if (gestionGroup) {
          const taskItem = gestionGroup.items.find(i => i.route === '/my-tasks');
          if (taskItem) {
            taskItem.badge = this.assignedTasksCount > 0 ? this.assignedTasksCount : undefined;
          }
        }
      },
      error: () => console.error('Failed to fetch unread docs count')
    });
  }

  toggleDocsAccordion() {
    this.isDocsExpanded = !this.isDocsExpanded;
  }

  private buildNav() {
    const isSuperAdmin = this.currentUser?.role === 'SUPER_ADMIN' || 
                         this.currentUser?.role === 'DIRECTEUR_GENERAL' || 
                         (this.currentUser?.role === 'DIRECTEUR' && this.currentUser?.departmentName === 'Direction Générale');

    const isChef = this.currentUser?.role === 'DIRECTEUR' || this.currentUser?.role === 'RESPONSABLE' || isSuperAdmin;

    const hasDocAccess = isSuperAdmin || this.currentUser?.permissions?.includes('VIEW_DOCUMENTS') || this.currentUser?.permissions?.includes('MANAGE_DOCUMENTS');
    const hasBillingAccess = isSuperAdmin || this.currentUser?.permissions?.includes('VIEW_BILLING') || this.currentUser?.permissions?.includes('MANAGE_BILLING');
    const hasChatAccess = isSuperAdmin || this.currentUser?.permissions?.includes('VIEW_CHAT');

    const docSubItems: NavItem[] = this.departments.map(dept => ({
      label: dept.name.replace('Département ', '').replace('Direction ', 'Dir. '),
      icon: 'topic',
      route: `/department-docs/${dept.id}`,
      isSubItem: true
    }));

    this.navGroups = [
      {
        name: 'Gestion',
        items: [
          { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
          { label: 'Mes Tâches', icon: 'task', route: '/my-tasks' },
          ...(isSuperAdmin ? [{ label: 'Utilisateurs', icon: 'group', route: '/users' }] : []),
          ...(isChef ? [{ label: 'Accès & Utilisateurs', icon: 'admin_panel_settings', route: '/roles' }] : []),
          { label: 'Structure & Départements', icon: 'corporate_fare', route: '/departments' }
        ]
      },
      ...(hasDocAccess ? [{
        name: 'Documents',
        isAccordion: true,
        items: [
          { label: 'Espace Documents', icon: 'folder', route: '/documents', badge: this.unreadDocsCount },
          ...docSubItems
        ]
      }] : []),
      ...(this.isInterimActive && this.interimDelegation ? [{
        name: 'Intérim',
        items: [
          { 
            label: `Validation (${this.interimDelegation.departmentName || 'Département'})`, 
            icon: 'verified_user', 
            route: '/documents' 
          }
        ]
      }] : []),
      {
        name: 'Projets',
        items: [
          { label: 'Tous les Projets', icon: 'folder_special', route: '/projects' },
          { label: 'Mes Missions', icon: 'assignment', route: '/project-tasks' }
        ]
      },
      ...(hasChatAccess ? [{
        name: 'IA & Communication',
        items: [
          { label: 'Chat Inter-équipes', icon: 'chat', route: '/chat' },
          { label: 'Chatbot RAG', icon: 'smart_toy', route: '/rag' },
          ...(isSuperAdmin ? [{ label: 'Base de Connaissances', icon: 'library_books', route: '/knowledge-base' }] : [])
        ]
      }] : []),
      ...(hasBillingAccess ? [{
        name: 'Finance',
        items: [
          { label: 'Facturation IA', icon: 'receipt_long', route: '/billing' }
        ]
      }] : [])
    ];
  }

  openProfile() {
    this.profileModalService.open();
  }

  private playNotificationSound() {
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const oscillator = audioCtx.createOscillator();
      const gainNode = audioCtx.createGain();

      oscillator.type = 'sine';
      oscillator.frequency.setValueAtTime(523.25, audioCtx.currentTime); // C5
      oscillator.frequency.exponentialRampToValueAtTime(1046.50, audioCtx.currentTime + 0.1); // C6

      gainNode.gain.setValueAtTime(0, audioCtx.currentTime);
      gainNode.gain.linearRampToValueAtTime(0.5, audioCtx.currentTime + 0.05);
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.5);

      oscillator.connect(gainNode);
      gainNode.connect(audioCtx.destination);

      oscillator.start(audioCtx.currentTime);
      oscillator.stop(audioCtx.currentTime + 0.5);
    } catch (e) {
      console.error('Audio play failed', e);
    }
  }
}
