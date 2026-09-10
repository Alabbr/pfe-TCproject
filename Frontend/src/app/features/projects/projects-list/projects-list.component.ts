import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ProjectService } from '../project.service';
import { Project } from '../projects.models';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../../shared/services/toast';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-projects-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './projects-list.component.html',
  styleUrl: './projects-list.component.scss'
})
export class ProjectsListComponent implements OnInit, OnDestroy {
  projects: Project[] = [];
  users: any[] = [];
  isLoading = true;
  showCreateModal = false;
  private notifSub?: Subscription;

  newProject = {
    title: '',
    description: '',
    startDate: '',
    deadline: '',
    memberIds: [] as number[]
  };

  constructor(
    private projectService: ProjectService, 
    private userService: UserService,
    public authService: AuthService,
    private router: Router, 
    private toast: ToastService,
    private notificationService: NotificationService
  ) {}

  get canCreateProject(): boolean {
    return this.authService.hasRole(['CHEF_DEPARTEMENT', 'DIRECTEUR', 'SUPER_ADMIN', 'DIRECTEUR_GENERAL']) ||
           this.authService.hasPermission('MANAGE_PROJECTS');
  }

  ngOnInit() {
    this.loadProjects();
    this.loadUsers();

    // Auto-refresh when a new project is created via WebSocket
    this.notifSub = this.notificationService.events$.subscribe(notif => {
      if (notif.type === 'PROJECT_CREATED' || notif.type === 'PROJECT_UPDATED') {
        this.loadProjects();
      }
    });
  }

  ngOnDestroy() {
    if (this.notifSub) {
      this.notifSub.unsubscribe();
    }
  }

  loadUsers() {
    this.userService.getUsers().subscribe({
      next: (res) => this.users = res,
      error: (err) => console.error('Failed to load users', err)
    });
  }

  loadProjects() {
    this.isLoading = true;
    this.projectService.getProjects().subscribe({
      next: (res) => {
        this.projects = res;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
        this.toast.error('Erreur', 'Impossible de charger les projets');
      }
    });
  }

  openProject(id: number) {
    this.router.navigate(['/projects', id]);
  }

  createProject() {
    this.projectService.createProject(this.newProject).subscribe({
      next: (res) => {
        this.toast.success('Succès', 'Projet créé avec succès');
        this.showCreateModal = false;
        this.loadProjects();
        // Reset
        this.newProject = { title: '', description: '', startDate: '', deadline: '', memberIds: [] };
      },
      error: (err) => {
        this.toast.error('Erreur', 'Impossible de créer le projet');
      }
    });
  }
}
