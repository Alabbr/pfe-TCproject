import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DepartmentService } from '../../../core/services/department.service';
import { JobPositionService } from '../../../core/services/job-position.service';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { Department, AuthResponse, User } from '../../../core/models/auth.model';
import { ToastService } from '../../../shared/services/toast';

@Component({
  selector: 'app-departments-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './departments-list.html',
  styleUrl: './departments-list.scss'
})
export class DepartmentsListComponent implements OnInit {
  departments: Department[] = [];
  archivedDepartments: Department[] = [];
  viewMode: 'list' | 'grid' = 'grid'; // Toggle between list and cards
  activeTab: 'active' | 'archived' = 'active';

  // ─── Form Drawer ───
  isDrawerOpen = false;
  selectedDep: Department | null = null;
  depForm!: FormGroup;
  isLoading = false;

  // ─── Details Modal ───
  isDetailsModalOpen = false;
  selectedDepDetails: Department | null = null;
  departmentJobPositions: any[] = [];
  departmentMembers: User[] = [];
  isLoadingDetails = false;

  currentUser: AuthResponse | null = null;

  constructor(
    private departmentService: DepartmentService,
    private fb: FormBuilder,
    private toastService: ToastService,
    private jobPositionService: JobPositionService,
    private authService: AuthService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
    this.loadDepartments();
    this.initForm();
  }

  loadDepartments(): void {
    this.departmentService.getDepartments().subscribe({
      next: (deps) => this.departments = deps,
      error: () => this.toastService.error('Erreur', 'Impossible de charger les départements')
    });
    this.departmentService.getArchivedDepartments().subscribe({
      next: (deps) => this.archivedDepartments = deps,
      error: () => console.error('Erreur chargement archives')
    });
  }

  setTab(tab: 'active' | 'archived'): void {
    this.activeTab = tab;
  }

  initForm(dep?: Department): void {
    this.depForm = this.fb.group({
      name: [dep?.name || '', Validators.required],
      description: [dep?.description || '']
    });
  }

  openDrawer(dep?: Department): void {
    this.selectedDep = dep || null;
    this.initForm(dep);
    this.isDrawerOpen = true;
  }

  closeDrawer(): void {
    this.isDrawerOpen = false;
    this.selectedDep = null;
  }

  // ─── Details Modal Logic ───
  openDetailsModal(dep: Department): void {
    this.selectedDepDetails = dep;
    this.isDetailsModalOpen = true;
    this.isLoadingDetails = true;
    this.departmentJobPositions = [];
    this.departmentMembers = [];
    
    // Load job positions for this department
    this.jobPositionService.getJobPositionsByDepartment(dep.id).subscribe({
      next: (positions: any[]) => {
        this.departmentJobPositions = positions;
        this.checkDetailsLoaded();
      },
      error: () => {
        this.checkDetailsLoaded();
      }
    });

    // Load members of this department
    this.userService.getUsersByDepartment(dep.id).subscribe({
      next: (members: User[]) => {
        // Filter out SUPER_ADMIN and sort by role (DIRECTEUR first)
        this.departmentMembers = members
          .filter(m => m.role !== 'SUPER_ADMIN' && m.role !== 'DIRECTEUR_GENERAL')
          .sort((a, b) => {
            if (a.role === 'DIRECTEUR' && b.role !== 'DIRECTEUR') return -1;
            if (b.role === 'DIRECTEUR' && a.role !== 'DIRECTEUR') return 1;
            return (a.firstName || '').localeCompare(b.firstName || '');
          });
        this.checkDetailsLoaded();
      },
      error: () => {
        this.checkDetailsLoaded();
      }
    });
  }

  private detailsLoadedCount = 0;
  private checkDetailsLoaded(): void {
    this.detailsLoadedCount++;
    if (this.detailsLoadedCount >= 2) {
      this.isLoadingDetails = false;
      this.detailsLoadedCount = 0;
    }
  }

  closeDetailsModal(): void {
    this.isDetailsModalOpen = false;
    this.selectedDepDetails = null;
  }

  saveDep(): void {
    if (this.depForm.invalid) {
      this.depForm.markAllAsTouched();
      this.toastService.error('Erreur', 'Veuillez remplir le nom du département.');
      return;
    }
    
    this.isLoading = true;
    const data = this.depForm.value;

    if (this.selectedDep) {
      this.departmentService.updateDepartment(this.selectedDep.id, data).subscribe({
        next: () => {
          this.loadDepartments();
          this.closeDrawer();
          this.isLoading = false;
          this.toastService.success('Succès', 'Département mis à jour avec succès');
        },
        error: (err) => {
          this.isLoading = false;
          this.toastService.error('Erreur', err.error?.message || 'Erreur lors de la modification');
        }
      });
    } else {
      this.departmentService.createDepartment(data).subscribe({
        next: () => {
          this.loadDepartments();
          this.closeDrawer();
          this.isLoading = false;
          this.toastService.success('Succès', 'Département créé avec succès');
        },
        error: (err) => {
          this.isLoading = false;
          this.toastService.error('Erreur', err.error?.message || 'Erreur lors de la création');
        }
      });
    }
  }

  async deleteDep(id: number): Promise<void> {
    const confirmed = await this.toastService.confirm({
      title: 'Désactiver ce département ?',
      message: 'Ce département sera déplacé vers les archives.',
      confirmText: 'Désactiver',
      isDanger: true
    });

    if (confirmed) {
      this.departmentService.deleteDepartment(id).subscribe({
        next: () => {
          this.loadDepartments();
          this.toastService.success('Succès', 'Département archivé avec succès');
        },
        error: () => this.toastService.error('Erreur', 'Impossible d\'archiver ce département')
      });
    }
  }

  async restoreDep(id: number): Promise<void> {
    this.departmentService.restoreDepartment(id).subscribe({
      next: () => {
        this.loadDepartments();
        this.toastService.success('Succès', 'Département restauré avec succès');
      },
      error: () => this.toastService.error('Erreur', 'Impossible de restaurer ce département')
    });
  }

  async permanentDeleteDep(id: number): Promise<void> {
    const confirmed = await this.toastService.confirm({
      title: 'Suppression définitive',
      message: '⚠️ Cette action est irréversible. Le département sera supprimé définitivement de la base de données.',
      confirmText: 'Supprimer définitivement',
      isDanger: true
    });
    if (confirmed) {
      this.departmentService.permanentDeleteDepartment(id).subscribe({
        next: () => {
          this.loadDepartments();
          this.toastService.success('Supprimé', 'Département supprimé définitivement');
        },
        error: () => this.toastService.error('Erreur', 'Impossible de supprimer ce département')
      });
    }
  }

  getDepartmentIcon(name: string): string {
    const n = name.toLowerCase();
    if (n.includes('it') || n.includes('informatique') || n.includes('tech') || n.includes('développement')) return 'terminal';
    if (n.includes('rh') || n.includes('ressources humaines') || n.includes('personnel')) return 'diversity_3';
    if (n.includes('finance') || n.includes('compta') || n.includes('trésorerie')) return 'account_balance';
    if (n.includes('marketing') || n.includes('communication') || n.includes('vente')) return 'campaign';
    if (n.includes('opération') || n.includes('operation') || n.includes('logistique') || n.includes('production')) return 'precision_manufacturing';
    if (n.includes('direction') || n.includes('management') || n.includes('stratégie')) return 'account_tree';
    if (n.includes('support') || n.includes('client') || n.includes('service')) return 'support_agent';
    if (n.includes('juridique') || n.includes('loi') || n.includes('droit')) return 'gavel';
    if (n.includes('recherche') || n.includes('r&d')) return 'science';
    return 'domain'; // Default modern fallback
  }
}
