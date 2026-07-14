import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentService, DocumentStatsResponse } from '../../../core/services/document.service';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentUploadModalComponent } from '../document-upload-modal/document-upload-modal';
import { PublicDocumentsModalComponent } from '../public-documents-modal/public-documents-modal';
import { PrivateInboxModalComponent } from '../private-inbox-modal/private-inbox-modal';
import { ValidationInboxModalComponent } from '../validation-inbox-modal/validation-inbox-modal';
import { ValidatedDocsModalComponent } from '../validated-docs-modal/validated-docs-modal';
import { DepartmentHistoryModalComponent } from '../department-history-modal/department-history-modal';

@Component({
  selector: 'app-documents-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    DocumentUploadModalComponent,
    PublicDocumentsModalComponent,
    PrivateInboxModalComponent,
    ValidationInboxModalComponent,
    ValidatedDocsModalComponent,
    DepartmentHistoryModalComponent
  ],
  templateUrl: './documents-dashboard.html',
  styleUrl: './documents-dashboard.scss'
})
export class DocumentsDashboardComponent implements OnInit {
  stats: DocumentStatsResponse | null = null;
  pendingCount: number = 0;
  isLoading = true;
  userRole: string = '';

  // Modals state
  showUploadModal = false;
  showPublicModal = false;
  showInboxModal = false;
  showValidationModal = false;
  showValidatedDocsModal = false;
  showHistoryModal = false;

  constructor(
    private documentService: DocumentService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.userRole = user.role;
    }
    this.loadStats();
  }

  loadStats() {
    this.isLoading = true;
    this.documentService.getStats().subscribe({
      next: (res) => {
        this.stats = res;
        this.isLoading = false;
        
        // If user is Chef de Département, load pending validations count
        if (this.userRole === 'RESPONSABLE' || this.userRole === 'DIRECTEUR' || this.userRole === 'DIRECTEUR_GENERAL') {
          this.documentService.getPendingValidationCount().subscribe({
            next: (count) => this.pendingCount = count,
            error: (err) => console.error('Error loading pending count', err)
          });
        }
      },
      error: (err) => {
        console.error('Error loading stats', err);
        this.isLoading = false;
      }
    });
  }

  openUpload() {
    this.showUploadModal = true;
  }

  openPublic() {
    this.showPublicModal = true;
  }

  openInbox() {
    this.showInboxModal = true;
  }

  openValidation() {
    this.showValidationModal = true;
  }

  openValidatedDocs() {
    this.showValidatedDocsModal = true;
  }

  openHistory() {
    this.showHistoryModal = true;
  }

  closeModals() {
    this.showUploadModal = false;
    this.showPublicModal = false;
    this.showInboxModal = false;
    this.showValidationModal = false;
    this.showValidatedDocsModal = false;
    this.showHistoryModal = false;
    this.loadStats(); // Refresh stats when modal closes
  }

  getReadPercentage(): number {
    if (!this.stats || this.stats.totalReceived === 0) return 0;
    const read = this.stats.totalReceived - this.stats.unreadCount;
    return Math.round((read / this.stats.totalReceived) * 100);
  }

  getBarHeight(value: number): number {
    if (!this.stats) return 10;
    const max = Math.max(this.stats.totalSent, this.stats.totalReceived, this.stats.publicDocsCount, this.pendingCount || 0);
    if (max === 0) return 10;
    return Math.max((value / max) * 120, 10);
  }
}
