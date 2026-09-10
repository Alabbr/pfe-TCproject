import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../../../shared/services/toast';

@Component({
  selector: 'app-knowledge-base',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './knowledge-base.html',
  styleUrl: './knowledge-base.scss'
})
export class KnowledgeBaseComponent {
  isDragging = false;
  isUploading = false;
  isBulkImporting = false;
  selectedFile: File | null = null;

  constructor(private http: HttpClient, private toastService: ToastService) {}

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.isDragging = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragging = false;
    
    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      const file = event.dataTransfer.files[0];
      this.handleFileSelection(file);
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFileSelection(input.files[0]);
    }
  }

  handleFileSelection(file: File) {
    if (file.type !== 'application/pdf') {
      this.toastService.error('Format invalide', 'Veuillez uploader un fichier PDF.');
      return;
    }
    if (file.size > 50 * 1024 * 1024) { // 50MB
      this.toastService.error('Fichier trop lourd', 'La taille maximale est de 50MB.');
      return;
    }
    this.selectedFile = file;
  }

  uploadFile() {
    if (!this.selectedFile) return;

    this.isUploading = true;
    const formData = new FormData();
    formData.append('file', this.selectedFile);

    this.http.post<{message: string}>('/api/rag/upload', formData).subscribe({
      next: (response) => {
        this.isUploading = false;
        this.selectedFile = null;
        this.toastService.success('Succès', 'Le document a été ingéré par l\'IA avec succès !');
      },
      error: (err) => {
        this.isUploading = false;
        console.error(err);
        this.toastService.error('Erreur', 'L\'ingestion du document a échoué.');
      }
    });
  }

  removeFile() {
    this.selectedFile = null;
  }

  triggerBulkImport() {
    if (confirm('Voulez-vous vraiment lancer l\'importation en masse du dossier RAG pdf ? Cela peut prendre plusieurs minutes.')) {
      this.isBulkImporting = true;
      this.http.post<{message: string}>('/api/rag/bulk-import', {}).subscribe({
        next: (response) => {
          this.isBulkImporting = false;
          this.toastService.success('Importation réussie', response.message);
        },
        error: (err) => {
          this.isBulkImporting = false;
          console.error(err);
          this.toastService.error('Erreur', 'Échec de l\'importation en masse.');
        }
      });
    }
  }
}
