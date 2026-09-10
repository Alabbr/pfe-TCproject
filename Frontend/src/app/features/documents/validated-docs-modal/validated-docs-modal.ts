import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { DocumentService, DocumentInboxResponse } from '../../../core/services/document.service';


@Component({
  selector: 'app-validated-docs-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './validated-docs-modal.html',
  styleUrl: './validated-docs-modal.scss',
  providers: [DatePipe]
})
export class ValidatedDocsModalComponent implements OnInit {
  @Output() close = new EventEmitter<void>();

  inboxItems: DocumentInboxResponse[] = [];
  isLoading = true;
  apiUrl = '';

  constructor(private documentService: DocumentService) {}

  ngOnInit() {
    this.loadInbox();
  }

  loadInbox() {
    this.isLoading = true;
    this.documentService.getMySentDocs().subscribe({
      next: (items) => {
        this.inboxItems = items;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load inbox', err);
        this.isLoading = false;
      }
    });
  }

  openDocument(item: DocumentInboxResponse) {
    if (!item.isRead) {
      this.documentService.markAsRead(item.id).subscribe({
        next: () => {
          item.isRead = true;
        },
        error: (err) => console.error('Failed to mark as read', err)
      });
    }
    
    this.documentService.downloadDocument(item.document.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
        setTimeout(() => window.URL.revokeObjectURL(url), 10000);
      },
      error: (err) => console.error('Download failed', err)
    });
  }

  getIconForFileType(type: string): string {
    if (!type) return 'insert_drive_file';
    if (type.includes('pdf')) return 'picture_as_pdf';
    if (type.includes('image')) return 'image';
    if (type.includes('word') || type.includes('document')) return 'description';
    if (type.includes('excel') || type.includes('spreadsheet')) return 'table_view';
    return 'insert_drive_file';
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'VALIDATED': return 'check_circle';
      case 'PENDING': return 'hourglass_empty';
      case 'REJECTED': return 'cancel';
      default: return 'send';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'VALIDATED': return '#10b981';
      case 'PENDING': return '#f59e0b';
      case 'REJECTED': return '#ef4444';
      default: return '#6b7280';
    }
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'VALIDATED': return 'Validé';
      case 'PENDING': return 'En Attente';
      case 'REJECTED': return 'Rejeté';
      case 'NOT_REQUIRED': return 'Délivré';
      default: return 'Envoyé';
    }
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }
}
