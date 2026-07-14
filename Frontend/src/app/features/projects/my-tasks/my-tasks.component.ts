import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProjectService } from '../project.service';
import { Task } from '../projects.models';
import { ToastService } from '../../../shared/services/toast';

@Component({
  selector: 'app-my-tasks',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-tasks.component.html',
  styleUrl: './my-tasks.component.scss'
})
export class MyTasksComponent implements OnInit {
  tasks: Task[] = [];
  isLoading = true;

  constructor(private projectService: ProjectService, private toast: ToastService) {}

  ngOnInit() {
    this.loadTasks();
  }

  loadTasks() {
    this.isLoading = true;
    this.projectService.getMyTasks().subscribe({
      next: (res) => {
        this.tasks = res.sort((a, b) => new Date(a.deadline).getTime() - new Date(b.deadline).getTime());
        this.isLoading = false;
      },
      error: () => {
        this.toast.error('Erreur', 'Impossible de charger vos missions');
        this.isLoading = false;
      }
    });
  }

  changeStatus(task: Task, status: string) {
    const comment = prompt("Commentaire (optionnel) :");
    if (comment !== null) {
      this.projectService.updateTaskStatus(task.id, status, comment).subscribe({
        next: () => {
          this.toast.success('Succès', 'Statut mis à jour');
          this.loadTasks();
        }
      });
    }
  }
}
