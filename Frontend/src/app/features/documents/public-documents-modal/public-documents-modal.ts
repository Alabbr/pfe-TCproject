import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentService, DocumentResponse } from '../../../core/services/document.service';


@Component({
  selector: 'app-public-documents-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './public-documents-modal.html',
  styleUrl: './public-documents-modal.scss',
  providers: [DatePipe]
})
export class PublicDocumentsModalComponent implements OnInit {
  @Output() close = new EventEmitter<void>();

  documents: DocumentResponse[] = [];
  filteredDocuments: DocumentResponse[] = [];
  isLoading = true;
  searchQuery = '';
  apiUrl = '';

  constructor(private documentService: DocumentService) {}

  ngOnInit() {
    this.loadDocuments();
  }

  loadDocuments() {
    this.isLoading = true;
    this.documentService.getPublicDocuments().subscribe({
      next: (docs) => {
        this.documents = docs;
        this.filteredDocuments = docs;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load public documents', err);
        this.isLoading = false;
      }
    });
  }

  onSearch() {
    const query = this.searchQuery.toLowerCase();
    this.filteredDocuments = this.documents.filter(doc => 
      doc.title.toLowerCase().includes(query) ||
      (doc.uploader?.firstName + ' ' + doc.uploader?.lastName).toLowerCase().includes(query)
    );
  }

  downloadDocument(doc: DocumentResponse) {
    this.documentService.downloadDocument(doc.id).subscribe({
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
