import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { JobPositionService } from '../../../core/services/job-position.service';
import { InterimService, InterimDelegation } from '../../../core/services/interim.service';
import { DocumentService } from '../../../core/services/document.service';
import { TransferRequestService } from '../../../core/services/transfer-request.service';
import { DepartmentService } from '../../../core/services/department.service';
import { User, JobPosition, AuthResponse } from '../../../core/models/auth.model';
import { ToastService } from '../../../shared/services/toast';
import { RoleFormatPipe } from '../../../shared/pipes/role-format.pipe';

@Component({
  selector: 'app-chef-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RoleFormatPipe],
  templateUrl: './chef-dashboard.html',
  styleUrls: ['./chef-dashboard.scss']
})
export class ChefDashboard implements OnInit {
  currentUser: AuthResponse | null = null;
  departmentUsers: User[] = [];
  pendingUsers: User[] = [];
  jobPositions: JobPosition[] = [];
  documentRequests: any[] = [];
  
  // Modal state
  isAssignModalOpen = false;
  selectedUserForAssign: User | null = null;
  selectedJobPositionId: number | '' = '';
  isAssigning = false;

  // Interim state
  activeDelegations: InterimDelegation[] = [];
  isInterimModalOpen = false;
  interimForm = {
    userId: '',
    startDate: '',
    endDate: ''
  };
  isSubmittingInterim = false;

  // Document Delegation state
  showDelegateDocModal = false;
  selectedDocRequest: any = null;
  selectedDelegateeId: number | null = null;
  isDelegatingDoc = false;

  // Transfer state
  isTransferModalOpen = false;
  selectedUserForTransfer: User | null = null;
  allDepartments: any[] = [];
  targetDepartmentJobs: JobPosition[] = [];
  transferForm = {
    targetDepartmentId: '',
    newJobPositionId: '',
    comment: ''
  };
  isSubmittingTransfer = false;

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private jobPositionService: JobPositionService,
    private interimService: InterimService,
    private documentService: DocumentService,
    private transferRequestService: TransferRequestService,
    private departmentService: DepartmentService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      if (this.currentUser && this.currentUser.departmentId) {
        this.loadJobPositions(this.currentUser.departmentId);
        this.loadDepartmentUsers(this.currentUser.departmentId);
        this.loadActiveDelegations();
        this.loadDocumentRequests(this.currentUser.departmentId);
      }
    });
  }

  // --- Document Requests ---

  loadDocumentRequests(deptId: number) {
    this.documentService.getDepartmentRequests(deptId).subscribe({
      next: (reqs) => {
        // Filter out FULFILLED, keep only PENDING, DELEGATED, ASSIGNED
        const activeReqs = reqs.filter(r => r.status !== 'FULFILLED');
        // Sort: PENDING first, then ASSIGNED, then DELEGATED
        this.documentRequests = activeReqs.sort((a, b) => {
          const statusOrder: { [key: string]: number } = { 'PENDING': 0, 'ASSIGNED': 1, 'DELEGATED': 2 };
          return (statusOrder[a.status] || 0) - (statusOrder[b.status] || 0);
        });
      },
      error: (err) => console.error('Failed to load requests', err)
    });
  }

  takeChargeRequest(id: number) {
    if (!this.currentUser) return;
    this.documentService.assignRequest(id, this.currentUser.userId).subscribe({
      next: () => {
        this.toastService.success('Succès', 'Demande prise en charge. Elle est ajoutée à vos tâches.');
        if (this.currentUser?.departmentId) {
          this.loadDocumentRequests(this.currentUser.departmentId);
        }
      },
      error: (err) => {
        this.toastService.error('Erreur', 'Impossible de prendre en charge la demande.');
      }
    });
  }

  openDelegateDocModal(req: any) {
    this.selectedDocRequest = req;
    this.selectedDelegateeId = null;
    this.showDelegateDocModal = true;
  }

  closeDelegateDocModal() {
    this.showDelegateDocModal = false;
    this.selectedDocRequest = null;
    this.selectedDelegateeId = null;
  }

  delegateDocRequest() {
    if (!this.selectedDelegateeId || !this.selectedDocRequest) return;
    this.isDelegatingDoc = true;
    
    this.documentService.delegateRequest(this.selectedDocRequest.id, this.selectedDelegateeId).subscribe({
      next: () => {
        this.toastService.success('Succès', 'Tâche déléguée avec succès.');
        this.closeDelegateDocModal();
        this.isDelegatingDoc = false;
        if (this.currentUser?.departmentId) {
          this.loadDocumentRequests(this.currentUser.departmentId);
        }
      },
      error: () => {
        this.toastService.error('Erreur', 'Échec de la délégation de la tâche.');
        this.isDelegatingDoc = false;
      }
    });
  }

  loadActiveDelegations() {
    this.interimService.getActiveDelegations().subscribe({
      next: (dels) => this.activeDelegations = dels,
      error: (err) => console.error(err)
    });
  }

  loadJobPositions(departmentId: number) {
    this.jobPositionService.getJobPositionsByDepartment(departmentId).subscribe({
      next: (positions) => this.jobPositions = positions,
      error: (err) => console.error('Erreur chargement postes', err)
    });
  }

  loadDepartmentUsers(departmentId: number) {
    this.userService.getUsers().subscribe({
      next: (users) => {
        // Filtre les utilisateurs de CE département (excluant le chef lui-même et les super admins)
        this.departmentUsers = users.filter(u =>
          u.departmentId === departmentId && 
          u.id !== this.currentUser?.userId &&
          u.role !== 'SUPER_ADMIN' &&
          u.role !== 'DIRECTEUR_GENERAL'
        );
        // Les employés en attente sont ceux qui n'ont pas de poste
        this.pendingUsers = this.departmentUsers.filter(u => u.role === 'EMPLOYE' && !u.jobPositionId);
      },
      error: (err) => console.error('Erreur chargement utilisateurs', err)
    });
  }

  openAssignModal(user: User) {
    this.selectedUserForAssign = user;
    this.selectedJobPositionId = '';
    this.isAssignModalOpen = true;
  }

  closeAssignModal() {
    this.isAssignModalOpen = false;
    this.selectedUserForAssign = null;
  }

  assignJobPosition() {
    if (!this.selectedUserForAssign || !this.selectedJobPositionId) return;

    this.isAssigning = true;
    this.userService.assignJobPosition(this.selectedUserForAssign.id, Number(this.selectedJobPositionId))
      .subscribe({
        next: () => {
          this.toastService.success('Succès', 'Poste attribué avec succès.');
          if (this.currentUser && this.currentUser.departmentId) {
             this.loadDepartmentUsers(this.currentUser.departmentId);
          }
          this.closeAssignModal();
          this.isAssigning = false;
        },
        error: (err) => {
          console.error(err);
          this.toastService.error('Erreur', 'Impossible d\'attribuer le poste.');
          this.isAssigning = false;
        }
      });
  }

  // Interim Modal Handlers
  openInterimModal() {
    this.isInterimModalOpen = true;
    this.interimForm = { userId: '', startDate: '', endDate: '' };
  }

  closeInterimModal() {
    this.isInterimModalOpen = false;
  }

  submitInterim() {
    if (!this.interimForm.userId || !this.interimForm.startDate || !this.interimForm.endDate) {
      this.toastService.error('Erreur', 'Veuillez remplir tous les champs.');
      return;
    }

    this.isSubmittingInterim = true;
    this.interimService.assignInterim(
      Number(this.interimForm.userId), 
      this.interimForm.startDate, 
      this.interimForm.endDate
    ).subscribe({
      next: () => {
        this.toastService.success('Succès', 'Intérim assigné. L\'annonce a été envoyée.');
        this.loadActiveDelegations();
        this.closeInterimModal();
        this.isSubmittingInterim = false;
      },
      error: (err) => {
        this.toastService.error('Erreur', 'Échec de l\'assignation.');
        this.isSubmittingInterim = false;
      }
    });
  }

  revokeDelegation(id: number) {
    if (confirm('Êtes-vous sûr de vouloir révoquer cet intérim ?')) {
      this.interimService.revokeDelegation(id).subscribe(() => {
        this.toastService.success('Succès', 'Intérim révoqué.');
        this.loadActiveDelegations();
      });
    }
  }

  // --- Transfer Request ---
  
  openTransferModal(user: User) {
    this.selectedUserForTransfer = user;
    this.isTransferModalOpen = true;
    this.transferForm = { targetDepartmentId: '', newJobPositionId: '', comment: '' };
    this.targetDepartmentJobs = [];
    
    // Load departments if not loaded
    if (this.allDepartments.length === 0) {
      this.departmentService.getDepartments().subscribe({
        next: (deps) => {
          this.allDepartments = deps.filter(d => d.id !== this.currentUser?.departmentId);
        },
        error: (err) => console.error(err)
      });
    }
  }

  closeTransferModal() {
    this.isTransferModalOpen = false;
    this.selectedUserForTransfer = null;
  }

  onTargetDepartmentChange() {
    this.transferForm.newJobPositionId = '';
    const deptId = Number(this.transferForm.targetDepartmentId);
    if (!deptId) {
      this.targetDepartmentJobs = [];
      return;
    }
    this.jobPositionService.getJobPositionsByDepartment(deptId).subscribe({
      next: (positions) => this.targetDepartmentJobs = positions,
      error: (err) => console.error(err)
    });
  }

  submitTransferRequest() {
    if (!this.selectedUserForTransfer || !this.transferForm.targetDepartmentId || !this.transferForm.comment) return;
    
    this.isSubmittingTransfer = true;
    const reqData = {
      employeeId: this.selectedUserForTransfer.id,
      targetDepartmentId: Number(this.transferForm.targetDepartmentId),
      newJobPositionId: this.transferForm.newJobPositionId ? Number(this.transferForm.newJobPositionId) : undefined,
      comment: this.transferForm.comment
    };

    this.transferRequestService.createRequest(reqData).subscribe({
      next: () => {
        this.toastService.success('Succès', 'Demande de transfert envoyée avec succès.');
        this.closeTransferModal();
        this.isSubmittingTransfer = false;
      },
      error: (err) => {
        this.toastService.error('Erreur', 'Impossible d\'envoyer la demande de transfert.');
        this.isSubmittingTransfer = false;
        console.error(err);
      }
    });
  }
}

