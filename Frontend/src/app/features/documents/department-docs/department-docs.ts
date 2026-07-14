import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { DocumentService } from '../../../core/services/document.service';
import { DepartmentService } from '../../../core/services/department.service';
import { AuthService } from '../../../core/services/auth.service';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ToastService } from '../../../shared/services/toast';

@Component({
  selector: 'app-department-docs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './department-docs.html',
  styleUrl: './department-docs.scss'
})
export class DepartmentDocs implements OnInit {
  departmentId!: number;
  departmentName: string = '';
  documents: any[] = [];
  currentUser: any;
  isLoading = true;

  // Request modal
  showRequestModal = false;
  requestDescription = '';
  selectedDocType = '';
  isRequesting = false;

  // Upload modal (for chef / interim)
  showUploadModal = false;
  uploadFile: File | null = null;
  uploadTitle = '';
  uploadDescription = '';
  isUploading = false;

  // Permissions
  isChefOfDepartment = false;
  isInterimOfDepartment = false;
  canManageDocs = false;

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private documentService: DocumentService,
    private departmentService: DepartmentService,
    private authService: AuthService,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.departmentId = Number(params.get('id'));
      if (this.departmentId) {
        this.loadDepartmentInfo();
        this.loadDocuments();
      }
    });

    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.checkPermissions();
    });
  }

  checkPermissions() {
    const user = this.currentUser;
    if (!user) return;

    // Chef of this department
    const isChefRole = user.role === 'DIRECTEUR' || user.role === 'SUPER_ADMIN' || user.role === 'DIRECTEUR_GENERAL';
    this.isChefOfDepartment = isChefRole && user.departmentId === this.departmentId;

    // Check if interim of this department
    this.http.get<any[]>('http://localhost:8080/api/interim/my-delegation').subscribe({
      next: (dels) => {
        if (dels && dels.length > 0) {
          this.isInterimOfDepartment = dels.some((d: any) => d.departmentId === this.departmentId);
        }
        this.canManageDocs = this.isChefOfDepartment || this.isInterimOfDepartment || user.role === 'SUPER_ADMIN';
      },
      error: () => {
        this.canManageDocs = this.isChefOfDepartment || user.role === 'SUPER_ADMIN';
      }
    });
  }

  loadDepartmentInfo() {
    this.departmentService.getDepartments().subscribe(deps => {
      const dept = deps.find((d: any) => d.id === this.departmentId);
      if (dept) this.departmentName = dept.name;
    });
  }

  loadDocuments() {
    this.isLoading = true;
    this.documentService.getDepartmentPublicDocs(this.departmentId).subscribe({
      next: (docs) => {
        this.documents = docs;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load docs', err);
        this.isLoading = false;
      }
    });
  }

  downloadDoc(doc: any) {
    this.documentService.downloadDocument(doc.id).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.originalFileName || doc.title;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    });
  }

  // --- Request Modal ---
  openRequestModal() {
    this.showRequestModal = true;
    this.requestDescription = '';
    this.selectedDocType = '';
  }

  submitRequest() {
    if (!this.requestDescription.trim() && !this.selectedDocType) return;
    this.isRequesting = true;
    
    let params = new HttpParams()
      .set('departmentId', this.departmentId.toString())
      .set('description', this.requestDescription.trim() || `Demande de document: ${this.selectedDocType}`);

    if (this.selectedDocType) {
      params = params.set('documentType', this.selectedDocType);
    }

    this.http.post('http://localhost:8080/api/documents/requests/create', null, { params }).subscribe({
      next: () => {
        this.isRequesting = false;
        this.showRequestModal = false;
        this.toastService.success('Succès', 'Votre demande a été envoyée au Chef de Département.');
      },
      error: (err) => {
        console.error(err);
        this.isRequesting = false;
        this.toastService.error('Erreur', 'Erreur lors de l\'envoi de la demande.');
      }
    });
  }

  // --- Upload Modal (Chef / Interim) ---
  openUploadModal() {
    this.showUploadModal = true;
    this.uploadFile = null;
    this.uploadTitle = '';
    this.uploadDescription = '';
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.uploadFile = file;
      if (!this.uploadTitle) {
        this.uploadTitle = file.name.replace(/\.[^/.]+$/, '');
      }
    }
  }

  submitUpload() {
    if (!this.uploadFile || !this.uploadTitle.trim()) return;
    this.isUploading = true;

    // Upload as a public department document
    this.documentService.uploadDocument(
      this.uploadFile,
      this.uploadTitle,
      this.uploadDescription,
      true, // isPublic - visible to all in department docs
      [],   // no specific recipients
      this.departmentId
    ).subscribe({
      next: () => {
        this.isUploading = false;
        this.showUploadModal = false;
        this.toastService.success('Succès', 'Document importé avec succès.');
        this.loadDocuments(); // Refresh the list
      },
      error: (err) => {
        console.error(err);
        this.isUploading = false;
        this.toastService.error('Erreur', 'Erreur lors de l\'upload du document.');
      }
    });
  }
}
