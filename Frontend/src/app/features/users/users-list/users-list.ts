import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, FormsModule, Validators } from '@angular/forms';
import { UserService } from '../../../core/services/user.service';
import { DepartmentService } from '../../../core/services/department.service';
import { AuthService } from '../../../core/services/auth.service';
import { User, Department, AuthResponse } from '../../../core/models/auth.model';
import { ToastService } from '../../../shared/services/toast';
import { ProfileModalComponent } from '../../../shared/components/profile-modal/profile-modal';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, ProfileModalComponent],
  templateUrl: './users-list.html',
  styleUrl: './users-list.scss'
})
export class UsersList implements OnInit {
  users: User[] = [];
  archivedUsers: User[] = [];
  departments: Department[] = [];

  // ─── Filters & Views ───
  viewMode: 'list' | 'grid' = 'grid';
  activeTab: 'active' | 'archived' = 'active';
  searchQuery: string = '';
  selectedDepartmentId: string = '';

  // ─── User Drawer (Edit) ───
  isDrawerOpen = false;
  selectedUser: User | null = null;
  userForm!: FormGroup;
  isLoading = false;

  // ─── Profile Modal (View Details) ───
  isProfileModalOpen = false;
  selectedUserIdToView: number | null = null;

  currentUser: AuthResponse | null = null;

  constructor(
    private userService: UserService,
    private departmentService: DepartmentService,
    private authService: AuthService,
    private fb: FormBuilder,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
    this.loadDepartments();
    this.loadUsers();
    this.initUserForm();
  }

  loadDepartments(): void {
    this.departmentService.getDepartments().subscribe({
      next: (deps) => this.departments = deps,
      error: () => {}
    });
  }

  private loadUsers(): void {
    this.userService.getUsers().subscribe({
      next: (users) => this.users = users,
      error: () => {}
    });
    this.userService.getArchivedUsers().subscribe({
      next: (users) => this.archivedUsers = users,
      error: () => {}
    });
  }

  setTab(tab: 'active' | 'archived'): void {
    this.activeTab = tab;
  }

  get filteredUsers(): User[] {
    const list = this.activeTab === 'active' ? this.users : this.archivedUsers;
    return list.filter(user => {
      const matchesSearch = !this.searchQuery || 
        `${user.firstName} ${user.lastName} ${user.email} ${user.role}`.toLowerCase().includes(this.searchQuery.toLowerCase());
      
      const matchesDept = !this.selectedDepartmentId || 
        user.departmentId?.toString() === this.selectedDepartmentId;

      return matchesSearch && matchesDept;
    });
  }

  private initUserForm(user?: User): void {
    this.userForm = this.fb.group({
      firstName: [user?.firstName || '', Validators.required],
      lastName: [user?.lastName || '', Validators.required],
      email: [user?.email || '', [Validators.required, Validators.email]],
      password: ['', user ? Validators.minLength(6) : [Validators.required, Validators.minLength(6)]],
      role: [user?.role || 'EMPLOYE', Validators.required],
      departmentId: [user?.departmentId || '', Validators.required]
    });
  }

  openDrawer(user?: User): void {
    this.selectedUser = user || null;
    this.initUserForm(user);
    this.isDrawerOpen = true;
  }

  closeDrawer(): void {
    this.isDrawerOpen = false;
    this.selectedUser = null;
  }

  // ─── View Profile Logic ───
  openProfileModal(userId: number): void {
    this.selectedUserIdToView = userId;
    this.isProfileModalOpen = true;
  }

  closeProfileModal(): void {
    this.isProfileModalOpen = false;
    this.selectedUserIdToView = null;
  }

  onProfileUpdated(updatedUser: any): void {
    this.loadUsers();
  }

  saveUser(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      this.toastService.error('Erreur', 'Veuillez remplir tous les champs obligatoires (le mot de passe doit faire au moins 6 caractères).');
      return;
    }
    this.isLoading = true;
    const userData = { ...this.userForm.value };
    userData.departmentId = Number(userData.departmentId);

    if (this.selectedUser) {
      if (!userData.password) delete userData.password;
      this.userService.updateUser(this.selectedUser.id, userData).subscribe({
        next: () => {
          this.loadUsers();
          this.closeDrawer();
          this.isLoading = false;
          this.toastService.success('Succès', 'Utilisateur modifié avec succès');
        },
        error: (err) => {
          this.isLoading = false;
          this.toastService.error('Erreur', err.error?.message || 'Erreur lors de la modification');
        }
      });
    } else {
      this.userService.createUser(userData).subscribe({
        next: () => {
          this.loadUsers();
          this.closeDrawer();
          this.isLoading = false;
          this.toastService.success('Succès', 'Utilisateur ajouté avec succès');
        },
        error: (err) => {
          this.isLoading = false;
          this.toastService.error('Erreur', err.error?.message || "Erreur lors de l'ajout");
        }
      });
    }
  }

  async deleteUser(id: number): Promise<void> {
    const confirmed = await this.toastService.confirm({
      title: "Supprimer l'utilisateur",
      message: 'Êtes-vous sûr de vouloir désactiver cet utilisateur ? Il ne pourra plus se connecter.',
      confirmText: 'Supprimer',
      isDanger: true
    });

    if (confirmed) {
      this.userService.deleteUser(id).subscribe({
        next: () => {
          this.loadUsers();
          this.toastService.success('Succès', 'Utilisateur désactivé (archivé)');
        },
        error: () => this.toastService.error('Erreur', "Impossible de désactiver l'utilisateur")
      });
    }
  }

  async restoreUser(id: number): Promise<void> {
    this.userService.restoreUser(id).subscribe({
      next: () => {
        this.loadUsers();
        this.toastService.success('Succès', 'Utilisateur restauré');
      },
      error: () => this.toastService.error('Erreur', "Impossible de restaurer l'utilisateur")
    });
  }

  async permanentDeleteUser(id: number): Promise<void> {
    const confirmed = await this.toastService.confirm({
      title: 'Suppression définitive',
      message: '⚠️ Cette action est irréversible. L\'utilisateur sera supprimé définitivement de la base de données.',
      confirmText: 'Supprimer définitivement',
      isDanger: true
    });
    if (confirmed) {
      this.userService.permanentDeleteUser(id).subscribe({
        next: () => {
          this.loadUsers();
          this.toastService.success('Supprimé', 'Utilisateur supprimé définitivement');
        },
        error: () => this.toastService.error('Erreur', "Impossible de supprimer l'utilisateur")
      });
    }
  }
}
