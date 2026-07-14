import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ProjectService } from '../project.service';
import { Project } from '../projects.models';
import { FormsModule } from '@angular/forms';
import { ToastService } from '../../../shared/services/toast';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-projects-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './projects-list.component.html',
  styleUrl: './projects-list.component.scss'
})
export class ProjectsListComponent implements OnInit {
  projects: Project[] = [];
  users: any[] = [];
  isLoading = true;
  showCreateModal = false;

  newProject = {
    title: '',
    description: '',
    deadline: '',
    memberIds: [] as number[]
  };

  constructor(
    private projectService: ProjectService, 
    private userService: UserService,
    private router: Router, 
    private toast: ToastService
  ) {}

  ngOnInit() {
    this.loadProjects();
    this.loadUsers();
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
        this.newProject = { title: '', description: '', deadline: '', memberIds: [] };
      },
      error: (err) => {
        this.toast.error('Erreur', 'Impossible de créer le projet');
      }
    });
  }
}
