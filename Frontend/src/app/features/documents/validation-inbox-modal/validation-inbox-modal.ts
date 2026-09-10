import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { DocumentService, DocumentInboxResponse } from '../../../core/services/document.service';


@Component({
  selector: 'app-validation-inbox-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './validation-inbox-modal.html',
  styleUrl: './validation-inbox-modal.scss',
  providers: [DatePipe]
})
export class ValidationInboxModalComponent implements OnInit {
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
    this.documentService.getPendingValidations().subscribe({
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

  validateDoc(item: DocumentInboxResponse) {
    this.documentService.validateDocument(item.id).subscribe({
      next: () => {
        this.inboxItems = this.inboxItems.filter(i => i.id !== item.id);
      },
      error: (err) => console.error('Failed to validate', err)
    });
  }

  rejectDoc(item: DocumentInboxResponse) {
    this.documentService.rejectDocument(item.id).subscribe({
      next: () => {
        this.inboxItems = this.inboxItems.filter(i => i.id !== item.id);
      },
      error: (err) => console.error('Failed to reject', err)
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

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }
}
