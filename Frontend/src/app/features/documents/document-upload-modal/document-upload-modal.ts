import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DocumentService } from '../../../core/services/document.service';
import { UserService } from '../../../core/services/user.service';
import { DepartmentService } from '../../../core/services/department.service';
import { RoleFormatPipe } from '../../../shared/pipes/role-format.pipe';

@Component({
  selector: 'app-document-upload-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RoleFormatPipe],
  templateUrl: './document-upload-modal.html',
  styleUrl: './document-upload-modal.scss'
})
export class DocumentUploadModalComponent implements OnInit {
  @Output() close = new EventEmitter<void>();
  
  uploadForm!: FormGroup;
  users: any[] = [];
  filteredUsers: any[] = [];
  departments: any[] = [];
  selectedFile: File | null = null;
  isDragging = false;
  isUploading = false;
  
  constructor(
    private fb: FormBuilder,
    private documentService: DocumentService,
    private userService: UserService,
    private departmentService: DepartmentService
  ) {}

  ngOnInit() {
    this.uploadForm = this.fb.group({
      title: ['', Validators.required],
      description: [''],
      isPublic: ['true'], // 'true' or 'false'
      departmentId: [''],
      recipientIds: [[]],
      needsValidation: [false]
    });

    this.departmentService.getDepartments().subscribe({
      next: (deps) => this.departments = deps,
      error: (err) => console.error('Failed to load departments', err)
    });

    this.userService.getUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.filteredUsers = users;
      },
      error: (err) => console.error('Failed to load users', err)
    });

    this.uploadForm.get('departmentId')?.valueChanges.subscribe(deptId => {
      if (deptId) {
        this.filteredUsers = this.users.filter(u => u.departmentId && u.departmentId === +deptId);
      } else {
        this.filteredUsers = this.users;
      }
      this.uploadForm.get('recipientIds')?.setValue([]); // reset selected users
    });
  }

  toggleRecipient(userId: number, event: any) {
    const isChecked = event.target.checked;
    const current = this.uploadForm.get('recipientIds')?.value as number[] || [];
    if (isChecked) {
      if (!current.includes(userId)) {
        this.uploadForm.get('recipientIds')?.setValue([...current, userId]);
      }
    } else {
      this.uploadForm.get('recipientIds')?.setValue(current.filter((id: number) => id !== userId));
    }
  }

  isRecipientSelected(userId: number): boolean {
    const current = this.uploadForm.get('recipientIds')?.value as number[] || [];
    return current.includes(userId);
  }

  get isPrivate(): boolean {
    return this.uploadForm.get('isPublic')?.value === 'false';
  }

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
      this.selectedFile = event.dataTransfer.files[0];
    }
  }

  onFileSelected(event: any) {
    if (event.target.files && event.target.files.length > 0) {
      this.selectedFile = event.target.files[0];
    }
  }

  onSubmit() {
    if (this.uploadForm.invalid || !this.selectedFile) {
      this.uploadForm.markAllAsTouched();
      return;
    }

    this.isUploading = true;
    const val = this.uploadForm.value;
    const isPublic = val.isPublic === 'true';
    const recipientIds = isPublic ? [] : val.recipientIds;
    const targetDeptId = isPublic ? undefined : (val.departmentId ? +val.departmentId : undefined);
    const needsValidation = isPublic ? false : val.needsValidation;

    this.documentService.uploadDocument(
      this.selectedFile,
      val.title,
      val.description,
      isPublic,
      recipientIds,
      targetDeptId,
      false, // skipValidation default
      needsValidation
    ).subscribe({
      next: () => {
        this.isUploading = false;
        this.close.emit();
      },
      error: (err) => {
        console.error('Upload failed', err);
        this.isUploading = false;
      }
    });
  }
}
