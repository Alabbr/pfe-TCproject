import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SettingService } from '../../../core/services/setting.service';
import { ToastService } from '../../services/toast';

@Component({
  selector: 'app-settings-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './settings-modal.html',
  styleUrls: ['./settings-modal.scss']
})
export class SettingsModalComponent {
  @Input() isOpen = false;
  @Output() closeEvent = new EventEmitter<void>();

  selectedFile: File | null = null;
  imagePreview: string | ArrayBuffer | null = null;
  isUploading = false;

  constructor(
    private settingService: SettingService,
    private toastService: ToastService
  ) {}

  closeModal() {
    this.closeEvent.emit();
    this.resetState();
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) { // 5MB max
        this.toastService.error('Fichier trop lourd', 'La taille de l\'image ne doit pas dépasser 5 Mo.');
        return;
      }
      this.selectedFile = file;
      
      const reader = new FileReader();
      reader.onload = e => this.imagePreview = reader.result;
      reader.readAsDataURL(file);
    }
  }

  uploadBackground() {
    if (!this.selectedFile) return;

    this.isUploading = true;
    this.settingService.uploadLoginBackground(this.selectedFile).subscribe({
      next: () => {
        this.toastService.success('Succès', 'Background de connexion mis à jour avec succès.');
        this.isUploading = false;
        this.closeModal();
      },
      error: (err) => {
        console.error('Upload error', err);
        this.toastService.error('Erreur', 'Impossible de mettre à jour le background.');
        this.isUploading = false;
      }
    });
  }

  private resetState() {
    this.selectedFile = null;
    this.imagePreview = null;
    this.isUploading = false;
  }
}
