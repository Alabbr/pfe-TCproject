import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentService, DocumentInboxResponse } from '../../../core/services/document.service';
import { ToastService } from '../../../shared/services/toast';

@Component({
  selector: 'app-department-history-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './department-history-modal.html',
  styleUrl: './department-history-modal.scss'
})
export class DepartmentHistoryModalComponent implements OnInit {
  @Output() close = new EventEmitter<void>();
  
  history: DocumentInboxResponse[] = [];
  isLoading = true;

  constructor(
    private documentService: DocumentService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadHistory();
  }

  loadHistory(): void {
    this.isLoading = true;
    this.documentService.getUploadHistory().subscribe({
      next: (res: DocumentInboxResponse[]) => {
        this.history = res;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error(err);
        this.toastService.error('Erreur', 'Impossible de charger l\'historique des envois.');
        this.isLoading = false;
      }
    });
  }

  downloadDoc(id: number, originalName: string): void {
    this.toastService.info('Téléchargement', 'Préparation du fichier...');
    this.documentService.downloadDocument(id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = originalName;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      },
      error: () => this.toastService.error('Erreur', 'Impossible de télécharger le document')
    });
  }
}
