import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, ToastMessage } from '../../services/toast';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      <div *ngFor="let toast of toasts" 
           class="toast-item" 
           [ngClass]="'toast-' + toast.type">
        
        <div class="toast-icon">
          <span class="material-symbols-outlined" *ngIf="toast.type === 'success'">check_circle</span>
          <span class="material-symbols-outlined" *ngIf="toast.type === 'error'">error</span>
          <span class="material-symbols-outlined" *ngIf="toast.type === 'warning'">warning</span>
          <span class="material-symbols-outlined" *ngIf="toast.type === 'info'">info</span>
        </div>
        
        <div class="toast-content">
          <h4>{{ toast.title }}</h4>
          <p>{{ toast.message }}</p>
        </div>
        
        <button class="toast-close" (click)="close(toast.id)">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>
    </div>
  `,
  styleUrls: ['./toast.scss']
})
export class ToastComponent {
  toasts: ToastMessage[] = [];

  constructor(public toastService: ToastService) {
    this.toastService.toasts$.subscribe(t => this.toasts = t);
  }

  close(id: number) {
    this.toastService.remove(id);
  }
}
