import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../project.service';
import { UserService } from '../../../core/services/user.service';
import { Project, Task, ProjectDocument, TaskHistory } from '../projects.models';
import { ToastService } from '../../../shared/services/toast';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './project-detail.component.html',
  styleUrl: './project-detail.component.scss'
})
export class ProjectDetailComponent implements OnInit {
  project!: Project;
  tasks: Task[] = [];
  documents: ProjectDocument[] = [];
  
  activeTab: 'tasks' | 'docs' | 'history' = 'tasks';
  isLoading = true;

  // Kanban view
  tasksTodo: Task[] = [];
  tasksInProgress: Task[] = [];
  tasksReview: Task[] = [];
  tasksDone: Task[] = [];

  showCreateTaskModal = false;
  newTask = {
    title: '',
    description: '',
    priority: 'NORMALE',
    deadline: '',
    assigneeId: null as number | null
  };

  showEditProjectModal = false;
  showDeleteConfirmModal = false;
  
  showStatusCommentModal = false;
  statusComment = '';
  pendingStatusChange = '';
  pendingTaskToChange: Task | null = null;

  users: any[] = [];
  editProjectData = {
    title: '',
    description: '',
    deadline: '',
    memberIds: [] as number[]
  };

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private toast: ToastService,
    private router: Router,
    private userService: UserService
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadProject(Number(id));
      this.loadTasks(Number(id));
      this.loadDocuments(Number(id));
    }
  }

  loadProject(id: number) {
    this.projectService.getProjectById(id).subscribe({
      next: (res) => this.project = res,
      error: () => this.toast.error('Erreur', 'Projet introuvable')
    });
  }

  loadTasks(id: number) {
    this.projectService.getTasksByProject(id).subscribe({
      next: (res) => {
        this.tasks = res;
        this.organizeKanban();
      }
    });
  }

  loadDocuments(id: number) {
    this.projectService.getProjectDocuments(id).subscribe({
      next: (res) => this.documents = res
    });
  }

  organizeKanban() {
    this.tasksTodo = this.tasks.filter(t => t.status === 'A_FAIRE');
    this.tasksInProgress = this.tasks.filter(t => t.status === 'EN_COURS');
    this.tasksReview = this.tasks.filter(t => t.status === 'EN_VALIDATION');
    this.tasksDone = this.tasks.filter(t => t.status === 'TERMINE');
  }

  createTask() {
    const payload: any = { ...this.newTask, projectId: this.project.id };
    if (!payload.deadline) {
      delete payload.deadline; // Don't send empty string to backend
    }
    this.projectService.createTask(payload).subscribe({
      next: () => {
        this.toast.success('Succès', 'Mission ajoutée');
        this.showCreateTaskModal = false;
        this.loadTasks(this.project.id);
        this.newTask = { title: '', description: '', priority: 'NORMALE', deadline: '', assigneeId: null };
      },
      error: (err) => {
        console.error(err);
        this.toast.error('Erreur', err.error?.error || 'Erreur lors de la création de la mission');
      }
    });
  }

  openStatusCommentModal(task: Task, newStatus: string) {
    this.pendingTaskToChange = task;
    this.pendingStatusChange = newStatus;
    this.statusComment = '';
    this.showStatusCommentModal = true;
  }

  closeStatusCommentModal() {
    this.showStatusCommentModal = false;
    this.pendingTaskToChange = null;
    this.pendingStatusChange = '';
    this.statusComment = '';
  }

  confirmStatusChange() {
    if (!this.pendingTaskToChange || !this.pendingStatusChange) return;

    this.projectService.updateTaskStatus(this.pendingTaskToChange.id, this.pendingStatusChange, this.statusComment).subscribe({
      next: () => {
        this.toast.success('Succès', 'Statut mis à jour');
        this.closeStatusCommentModal();
        this.loadTasks(this.project.id);
      },
      error: (err) => this.toast.error('Erreur', err.error?.error || 'Erreur inconnue')
    });
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.projectService.uploadDocument(this.project.id, file).subscribe({
        next: () => {
          this.toast.success('Succès', 'Document ajouté');
          this.loadDocuments(this.project.id);
        }
      });
    }
  }

  downloadDoc(id: number) {
    window.open(`http://localhost:8080/api/projects/documents/download/${id}`, '_blank');
  }

  // Traçabilité
  selectedTaskHistory: TaskHistory[] = [];
  selectedTaskTitle = '';

  loadTaskHistory(task: Task) {
    this.selectedTaskTitle = task.title;
    this.projectService.getTaskHistory(task.id).subscribe({
      next: (res) => this.selectedTaskHistory = res
    });
  }
  // Edit / Delete Project
  openEditModal() {
    this.editProjectData = {
      title: this.project.title,
      description: this.project.description,
      deadline: this.project.deadline ? this.project.deadline.substring(0, 10) : '',
      memberIds: this.project.members ? this.project.members.map(m => m.id) : []
    };
    if (this.users.length === 0) {
      this.userService.getUsers().subscribe(res => this.users = res);
    }
    this.showEditProjectModal = true;
  }

  updateProject() {
    const payload: any = { ...this.editProjectData };
    if (!payload.deadline) {
      delete payload.deadline;
    }
    this.projectService.updateProject(this.project.id, payload).subscribe({
      next: () => {
        this.toast.success('Succès', 'Projet mis à jour');
        this.showEditProjectModal = false;
        this.loadProject(this.project.id);
      },
      error: (err) => {
        this.toast.error('Erreur', err.error?.error || 'Erreur lors de la mise à jour');
      }
    });
  }

  deleteProject() {
    this.projectService.deleteProject(this.project.id).subscribe({
      next: () => {
        this.toast.success('Succès', 'Projet supprimé');
        this.router.navigate(['/projects']);
      },
      error: (err) => {
        this.toast.error('Erreur', err.error?.error || 'Erreur lors de la suppression');
      }
    });
  }
}
