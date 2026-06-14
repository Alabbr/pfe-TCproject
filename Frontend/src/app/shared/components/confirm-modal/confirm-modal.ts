import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, ConfirmConfig } from '../../services/toast';

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="modal-overlay" [class.show]="config" (click)="close(false)"></div>
    
    <div class="confirm-modal" [class.open]="config">
      <div class="confirm-icon" [ngClass]="{'danger': config?.isDanger}">
        <span class="material-symbols-outlined">{{ config?.isDanger ? 'warning' : 'help' }}</span>
      </div>
      
      <div class="confirm-content">
        <h3>{{ config?.title }}</h3>
        <p>{{ config?.message }}</p>
      </div>
      
      <div class="confirm-actions">
        <button class="btn-outline" (click)="close(false)">
          {{ config?.cancelText || 'Annuler' }}
        </button>
        <button class="btn-primary" [ngClass]="{'danger': config?.isDanger}" (click)="close(true)">
          {{ config?.confirmText || 'Confirmer' }}
        </button>
      </div>
    </div>
  `,
  styleUrls: ['./confirm-modal.scss']
})
export class ConfirmModalComponent {
  config: ConfirmConfig | null = null;

  constructor(private toastService: ToastService) {
    this.toastService.confirmState$.subscribe(state => {
      this.config = state.config;
    });
  }

  close(result: boolean) {
    this.toastService.closeConfirm(result);
  }
}
