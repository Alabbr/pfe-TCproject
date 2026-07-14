import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentService } from '../../../core/services/document.service';
import { ToastService } from '../../../shared/services/toast';

// @ts-ignore
import html2pdf from 'html2pdf.js';

@Component({
  selector: 'app-employee-tasks',
  standalone: true,
  imports: [CommonModule, FormsModule],
  providers: [DatePipe],
  templateUrl: './employee-tasks.html',
  styleUrls: ['./employee-tasks.scss']
})
export class EmployeeTasksComponent implements OnInit {
  tasks: any[] = [];
  isLoading = true;

  // Editor Workspace
  selectedTask: any = null;
  isGenerating = false;
  
  // Data for the template
  templateData = {
    fullName: '',
    department: '',
    jobPosition: '',
    hireDate: '2020-01-01',
    salary: 1500,
    location: 'Tunis',
    currentDate: new Date().toISOString().split('T')[0]
  };

  constructor(
    private documentService: DocumentService,
    private toastService: ToastService,
    private datePipe: DatePipe
  ) {}

  ngOnInit() {
    this.loadTasks();
  }

  loadTasks() {
    this.isLoading = true;
    this.documentService.getAssignedRequests().subscribe({
      next: (data) => {
        this.tasks = data;
        this.isLoading = false;
      },
      error: () => {
        this.toastService.error('Erreur', 'Impossible de charger vos tâches.');
        this.isLoading = false;
      }
    });
  }

  openTask(task: any) {
    this.selectedTask = task;
    
    // Auto-fill template data
    this.templateData.fullName = `${task.requester.firstName} ${task.requester.lastName}`;
    this.templateData.department = task.requester.department || 'Inconnu';
    this.templateData.jobPosition = task.requester.jobPosition || task.requester.role;
    this.templateData.currentDate = new Date().toISOString().split('T')[0];
    this.templateData.location = 'Tunis';
  }

  closeTask() {
    this.selectedTask = null;
  }

  async generateAndSend() {
    this.isGenerating = true;

    // 1. Get the HTML element
    const element = document.getElementById('document-template');
    if (!element) {
      this.toastService.error('Erreur', 'Modèle introuvable.');
      this.isGenerating = false;
      return;
    }

    // 2. Options for html2pdf
    const opt: any = {
      margin:       0, // No margins, let the HTML padding handle it
      filename:     `${this.selectedTask.documentType}_${this.templateData.fullName.replace(' ', '_')}.pdf`,
      image:        { type: 'jpeg', quality: 0.98 },
      html2canvas:  { scale: 2, useCORS: true, logging: false },
      jsPDF:        { unit: 'mm', format: 'a4', orientation: 'portrait' }
    };

    try {
      // 3. Generate PDF Blob
      const pdfBlob = await html2pdf().set(opt).from(element).output('blob');
      const file = new File([pdfBlob], opt.filename, { type: 'application/pdf' });

      // 4. Send via DocumentService
      const title = `${this.selectedTask.documentType.replace('_', ' ')} pour ${this.templateData.fullName}`;
      const desc = `Document généré automatiquement suite à la demande.`;
      
      this.documentService.uploadDocument(
        file, 
        title, 
        desc, 
        false, // NOT public
        [this.selectedTask.requester.id], // Send to requester only
        undefined, // targetDepartmentId
        true // skipValidation
      ).subscribe({
        next: () => {
          // 5. Mark request as fulfilled
          this.documentService.fulfillRequest(this.selectedTask.id).subscribe({
            next: () => {
              this.toastService.success('Envoyé', 'Le document a été généré et envoyé au demandeur !');
              this.isGenerating = false;
              this.selectedTask = null;
              this.loadTasks(); // Reload task list
            },
            error: () => {
              this.toastService.error('Erreur', 'Document envoyé mais impossible de clôturer la demande.');
              this.isGenerating = false;
            }
          });
        },
        error: () => {
          this.toastService.error('Erreur', 'Impossible d\'envoyer le document.');
          this.isGenerating = false;
        }
      });
    } catch (err) {
      console.error(err);
      this.toastService.error('Erreur', 'Erreur lors de la génération du PDF.');
      this.isGenerating = false;
    }
  }
}
