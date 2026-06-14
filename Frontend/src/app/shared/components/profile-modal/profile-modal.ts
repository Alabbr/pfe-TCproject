import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { User } from '../../../core/models/auth.model';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-profile-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile-modal.html',
  styleUrls: ['./profile-modal.scss']
})
export class ProfileModalComponent implements OnInit {
  @Input() isOpen = false;
  @Input() userId: number | null = null;
  @Output() closeEvent = new EventEmitter<void>();
  @Output() userUpdated = new EventEmitter<User>();

  user: User | null = null;
  editData = {
    phoneNumber: '',
    address: ''
  };

  isLoading = false;

  constructor(private userService: UserService) {}

  ngOnInit() {
    this.loadUser();
  }

  ngOnChanges() {
    if (this.isOpen && this.userId) {
      this.loadUser();
    }
  }

  loadUser() {
    if (this.userId) {
      this.isLoading = true;
      this.userService.getUser(this.userId).subscribe({
        next: (userData) => {
          this.user = userData;
          this.editData.phoneNumber = this.user.phoneNumber || '';
          this.editData.address = this.user.address || '';
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error loading user', err);
          this.isLoading = false;
        }
      });
    }
  }

  close() {
    this.isOpen = false;
    this.closeEvent.emit();
  }

  formatRole(role?: string): string {
    if (!role) return '';
    return role.replace(/_/g, ' ');
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file && this.user) {
      this.isLoading = true;
      this.userService.uploadProfilePicture(this.user.id, file).subscribe({
        next: (updatedUser) => {
          this.user = updatedUser;
          this.userUpdated.emit(updatedUser);
          this.isLoading = false;
        },
        error: (err) => {
          console.error('Error uploading image', err);
          this.isLoading = false;
        }
      });
    }
  }

  saveProfile() {
    if (this.user) {
      this.isLoading = true;
      this.userService.updateProfile(this.user.id, this.editData.phoneNumber, this.editData.address)
        .subscribe({
          next: (updatedUser) => {
            this.user = updatedUser;
            this.userUpdated.emit(updatedUser);
            this.isLoading = false;
            this.close();
          },
          error: (err) => {
            console.error('Error updating profile', err);
            this.isLoading = false;
          }
        });
    }
  }
}
