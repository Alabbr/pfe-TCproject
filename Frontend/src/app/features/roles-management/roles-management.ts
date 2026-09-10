import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RolePermissionService, RolePermission } from '../../core/services/role-permission.service';
import { DepartmentService } from '../../core/services/department.service';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { Department, User, AuthResponse } from '../../core/models/auth.model';
import { ToastService } from '../../shared/services/toast';

@Component({
  selector: 'app-roles-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './roles-management.html',
  styleUrls: ['./roles-management.scss']
})
export class RolesManagementComponent implements OnInit {
  availablePermissions: string[] = [
    'VIEW_DASHBOARD', 'MANAGE_USERS', 'VIEW_DOCUMENTS', 'MANAGE_DOCUMENTS',
    'USE_CHATBOT', 'VIEW_BILLING', 'MANAGE_BILLING', 'VIEW_CHAT',
    'VIEW_PROJECTS', 'MANAGE_PROJECTS', 'MANAGE_KNOWLEDGE_BASE',
    'MANAGE_SETTINGS'
  ];

  // Permissions a Chef de Département can toggle for their employees
  chefAllowedPermissions: string[] = [
    'VIEW_DASHBOARD', 'VIEW_DOCUMENTS', 'MANAGE_DOCUMENTS',
    'USE_CHATBOT', 'VIEW_CHAT', 'VIEW_BILLING',
    'VIEW_PROJECTS', 'MANAGE_PROJECTS'
  ];
  
  availableRoles: string[] = [
    'SUPER_ADMIN', 'DIRECTEUR_GENERAL', 'DIRECTEUR', 'EMPLOYE'
  ];

  currentUser: AuthResponse | null = null;

  // Department & User Selection
  departments: Department[] = [];
  selectedDepartment: Department | null = null;
  departmentUsers: User[] = [];
  
  selectedUser: User | null = null;
  selectedUserRole: string = '';
  currentPermissions: string[] = [];
  isUpdatingUser = false;
  isUpdatingPermissions = false;

  constructor(
    private rolePermissionService: RolePermissionService,
    private departmentService: DepartmentService,
    private userService: UserService,
    private authService: AuthService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.loadDepartments();
    });
  }

  isSuperAdmin(): boolean {
    return this.currentUser?.role === 'SUPER_ADMIN' || this.currentUser?.role === 'DIRECTEUR_GENERAL';
  }

  /** Check if current user can edit permissions (Super Admin or Chef for their dept employees) */
  canEditPermissions(): boolean {
    if (this.isSuperAdmin()) return true;
    // Chef can edit employee permissions in their department
    if (this.selectedUser && this.currentUser?.role === 'DIRECTEUR') {
      return this.selectedUser.role === 'EMPLOYE';
    }
    return false;
  }

  /** Get the list of permissions the current user can see/toggle */
  getVisiblePermissions(): string[] {
    if (this.isSuperAdmin()) return this.availablePermissions;
    return this.chefAllowedPermissions;
  }

  /** Check if a specific permission toggle is enabled for current user */
  isPermissionEditable(permission: string): boolean {
    if (this.isSuperAdmin()) return true;
    if (this.currentUser?.role === 'DIRECTEUR' && this.selectedUser?.role === 'EMPLOYE') {
      return this.chefAllowedPermissions.includes(permission);
    }
    return false;
  }

  loadDepartments() {
    this.departmentService.getDepartments().subscribe({
      next: (deps) => {
        if (this.isSuperAdmin()) {
          this.departments = deps;
        } else {
          // Chef de département : only their department
          this.departments = deps.filter(d => d.id === this.currentUser?.departmentId);
          if (this.departments.length > 0) {
            this.selectDepartment(this.departments[0]);
          }
        }
      },
      error: (err) => console.error(err)
    });
  }

  selectDepartment(dep: Department) {
    this.selectedDepartment = dep;
    this.selectedUser = null;
    this.departmentUsers = [];
    this.currentPermissions = [];

    this.userService.getUsersByDepartment(dep.id).subscribe({
      next: (users) => this.departmentUsers = users,
      error: (err) => console.error(err)
    });
  }

  selectUser(user: User) {
    this.selectedUser = user;
    this.selectedUserRole = user.role;
    this.loadUserPermissions(user);
  }

  onUserRoleChange() {
    // When role changes, reload role-level permissions for preview
    this.loadRolePermissions(this.selectedUserRole);
  }

  /** Load the selected user's effective permissions */
  loadUserPermissions(user: User) {
    // If user has individual permissions, use those
    if (user.permissions && user.permissions.length > 0) {
      this.currentPermissions = [...user.permissions];
    } else {
      // Otherwise load from role config
      this.loadRolePermissions(user.role);
    }
  }

  loadRolePermissions(roleName: string) {
    this.rolePermissionService.getRolePermissions(roleName).subscribe({
      next: (data) => {
        if (data && data.permissions) {
          this.currentPermissions = [...data.permissions];
        } else {
          this.currentPermissions = [];
        }
      },
      error: (err) => {
        console.error('Error fetching role permissions', err);
        this.currentPermissions = [];
      }
    });
  }

  togglePermission(permission: string, event: Event) {
    if (!this.canEditPermissions()) return;

    const isChecked = (event.target as HTMLInputElement).checked;
    if (isChecked) {
      if (!this.currentPermissions.includes(permission)) {
        this.currentPermissions.push(permission);
      }
    } else {
      this.currentPermissions = this.currentPermissions.filter((p: string) => p !== permission);
    }
  }

  hasPermission(permission: string): boolean {
    return this.currentPermissions.includes(permission);
  }

  roleLabel(role: string): string {
    const labels: Record<string, string> = {
      'SUPER_ADMIN': 'Super Admin',
      'DIRECTEUR_GENERAL': 'Directeur Général',
      'DIRECTEUR': 'Chef de Département',
      'EMPLOYE': 'Employé'
    };
    return labels[role] || role.replace(/_/g, ' ');
  }

  permissionLabel(perm: string): string {
    const labels: Record<string, string> = {
      'VIEW_DASHBOARD': 'Tableau de bord',
      'MANAGE_USERS': 'Gestion des utilisateurs',
      'VIEW_DOCUMENTS': 'Voir les documents',
      'MANAGE_DOCUMENTS': 'Gérer les documents',
      'USE_CHATBOT': 'Chatbot IA (RAG)',
      'VIEW_BILLING': 'Voir la facturation',
      'MANAGE_BILLING': 'Gérer la facturation',
      'VIEW_CHAT': 'Chat inter-équipes',
      'VIEW_PROJECTS': 'Voir les projets & missions',
      'MANAGE_PROJECTS': 'Gérer les projets (création, modif)',
      'MANAGE_KNOWLEDGE_BASE': 'Gérer la base de connaissances (RAG)',
      'MANAGE_SETTINGS': 'Paramètres système'
    };
    return labels[perm] || perm.replace(/_/g, ' ');
  }

  savePermissions() {
    if (!this.canEditPermissions() || !this.selectedUser) return;

    this.isUpdatingPermissions = true;

    if (this.isSuperAdmin()) {
      // Super Admin: update role-level permissions (affects all users with this role)
      this.rolePermissionService.updateRolePermissions(this.selectedUserRole, this.currentPermissions).subscribe({
        next: () => {
          this.toastService.success('Succès', 'Permissions globales mises à jour pour le rôle ' + this.roleLabel(this.selectedUserRole));
          this.isUpdatingPermissions = false;
        },
        error: (err) => {
          this.toastService.error('Erreur', 'Erreur lors de la mise à jour des permissions.');
          console.error(err);
          this.isUpdatingPermissions = false;
        }
      });
    } else {
      // Chef: update per-user permissions
      this.userService.updateUserPermissions(this.selectedUser.id, this.currentPermissions).subscribe({
        next: (updated) => {
          this.toastService.success('Succès', 'Accès mis à jour pour ' + (updated.fullName || this.selectedUser!.firstName + ' ' + this.selectedUser!.lastName));
          this.isUpdatingPermissions = false;

          // Update the user in the local list
          const idx = this.departmentUsers.findIndex(u => u.id === updated.id);
          if (idx !== -1) {
            this.departmentUsers[idx] = updated;
          }
          if (this.selectedUser) {
            this.selectedUser.permissions = updated.permissions;
          }
        },
        error: (err) => {
          this.toastService.error('Erreur', err?.error?.message || 'Erreur lors de la mise à jour des permissions.');
          console.error(err);
          this.isUpdatingPermissions = false;
        }
      });
    }
  }

  updateUserRole() {
    if (!this.selectedUser) return;
    this.isUpdatingUser = true;

    const updateReq = {
      firstName: this.selectedUser.firstName,
      lastName: this.selectedUser.lastName,
      email: this.selectedUser.email,
      role: this.selectedUserRole,
      departmentId: this.selectedUser.departmentId
    };

    this.userService.updateUser(this.selectedUser.id, updateReq).subscribe({
      next: (updated) => {
        this.toastService.success('Succès', 'Rôle mis à jour avec succès pour ' + updated.fullName);
        this.selectedUser!.role = updated.role;
        this.isUpdatingUser = false;
        
        // Refresh the user in the list
        const idx = this.departmentUsers.findIndex(u => u.id === updated.id);
        if (idx !== -1) {
          this.departmentUsers[idx] = updated;
        }
      },
      error: (err) => {
        this.toastService.error('Erreur', 'Impossible de mettre à jour le rôle de l\'utilisateur.');
        this.isUpdatingUser = false;
        console.error(err);
      }
    });
  }
}
