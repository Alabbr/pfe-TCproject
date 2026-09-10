import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../project.service';
import { Task } from '../projects.models';
import { ToastService } from '../../../shared/services/toast';
import { NotificationService } from '../../../core/services/notification.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-my-tasks',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './my-tasks.component.html',
  styleUrl: './my-tasks.component.scss'
})
export class MyTasksComponent implements OnInit, OnDestroy {
  tasks: Task[] = [];
  isLoading = true;
  private notifSub!: Subscription;

  // Modal state
  showStatusModal = false;
  pendingTask: Task | null = null;
  pendingStatus = '';
  statusComment = '';
  validationFile: File | null = null;

  constructor(
    private projectService: ProjectService, 
    private toast: ToastService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.loadTasks();
    
    this.notifSub = this.notificationService.events$.subscribe(event => {
      if (event.type === 'TASK_UPDATED' || event.type === 'TASK_REVIEW_REQUESTED') {
        this.loadTasks();
      }
    });
  }

  ngOnDestroy() {
    if (this.notifSub) {
      this.notifSub.unsubscribe();
    }
  }

  loadTasks() {
    this.isLoading = true;
    this.projectService.getMyTasks().subscribe({
      next: (res) => {
        this.tasks = res.sort((a, b) => {
          if (!a.deadline) return 1;
          if (!b.deadline) return -1;
          return new Date(a.deadline).getTime() - new Date(b.deadline).getTime();
        });
        this.isLoading = false;
      },
      error: () => {
        this.toast.error('Erreur', 'Impossible de charger vos missions');
        this.isLoading = false;
      }
    });
  }

  openStatusModal(task: Task, status: string) {
    this.pendingTask = task;
    this.pendingStatus = status;
    this.statusComment = '';
    this.validationFile = null;
    this.showStatusModal = true;
  }

  closeStatusModal() {
    this.showStatusModal = false;
    this.pendingTask = null;
    this.pendingStatus = '';
    this.statusComment = '';
    this.validationFile = null;
  }

  onValidationFileSelected(event: any) {
    this.validationFile = event.target.files[0] || null;
  }

  confirmStatusChange() {
    if (!this.pendingTask || !this.pendingStatus) return;

    if (this.pendingStatus === 'EN_VALIDATION') {
      this.projectService.requestValidation(this.pendingTask.id, this.validationFile, this.statusComment).subscribe({
        next: () => {
          this.toast.success('Succès', 'Demande de validation envoyée');
          this.closeStatusModal();
          this.loadTasks();
        },
        error: (err) => this.toast.error('Erreur', err.error?.error || 'Erreur inconnue')
      });
    } else {
      this.projectService.updateTaskStatus(this.pendingTask.id, this.pendingStatus, this.statusComment).subscribe({
        next: () => {
          this.toast.success('Succès', 'Statut mis à jour');
          this.closeStatusModal();
          this.loadTasks();
        },
        error: (err) => this.toast.error('Erreur', err.error?.error || 'Erreur inconnue')
      });
    }
  }
}
