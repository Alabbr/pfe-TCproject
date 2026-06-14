import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { JobPositionService } from '../../../core/services/job-position.service';
import { User, JobPosition, AuthResponse } from '../../../core/models/auth.model';
import { ToastService } from '../../../shared/services/toast';

@Component({
  selector: 'app-chef-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chef-dashboard.html',
  styleUrls: ['./chef-dashboard.scss']
})
export class ChefDashboard implements OnInit {
  currentUser: AuthResponse | null = null;
  departmentUsers: User[] = [];
  pendingUsers: User[] = [];
  jobPositions: JobPosition[] = [];
  
  // Modal state
  isAssignModalOpen = false;
  selectedUserForAssign: User | null = null;
  selectedJobPositionId: number | '' = '';
  isAssigning = false;

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private jobPositionService: JobPositionService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      if (this.currentUser && this.currentUser.departmentId) {
        this.loadJobPositions(this.currentUser.departmentId);
        this.loadDepartmentUsers(this.currentUser.departmentId);
      }
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
}
