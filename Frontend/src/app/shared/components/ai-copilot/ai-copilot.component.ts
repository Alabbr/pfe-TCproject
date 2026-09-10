import { Component, ElementRef, HostListener, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AgentService } from '../../../core/services/agent.service';
import { ToastService } from '../../services/toast';
import { Router } from '@angular/router';

@Component({
  selector: 'app-ai-copilot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-copilot.component.html',
  styleUrl: './ai-copilot.component.scss'
})
export class AiCopilotComponent {
  isOpen = false;
  command = '';
  isProcessing = false;
  history: { type: 'user' | 'agent', text: string }[] = [
    { type: 'agent', text: 'Bonjour ! Je suis votre assistant IA. Demandez-moi de créer un projet, une tâche ou d\'envoyer un message.' }
  ];

  @ViewChild('commandInput') commandInput!: ElementRef;
  @ViewChild('chatBody') chatBody!: ElementRef;

  constructor(
    private agentService: AgentService,
    private toast: ToastService,
    private router: Router
  ) {}

  toggleOpen() {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      setTimeout(() => this.commandInput?.nativeElement.focus(), 100);
    }
  }

  close() {
    this.isOpen = false;
  }

  sendCommand() {
    if (!this.command.trim() || this.isProcessing) return;

    const userText = this.command.trim();
    this.history.push({ type: 'user', text: userText });
    this.command = '';
    this.isProcessing = true;
    this.scrollToBottom();

    this.agentService.executeCommand(userText).subscribe({
      next: (res) => {
        this.isProcessing = false;
        this.history.push({ type: 'agent', text: res.reply });
        
        if (res.action === 'CREATE_PROJECT' && res.projectId) {
          // Optional: navigate to project
          // this.router.navigate(['/projects', res.projectId]);
        }
        
        this.scrollToBottom();
      },
      error: (err) => {
        this.isProcessing = false;
        this.history.push({ type: 'agent', text: err.error?.reply || 'Une erreur est survenue lors de l\'exécution.' });
        this.scrollToBottom();
      }
    });
  }

  private scrollToBottom() {
    setTimeout(() => {
      if (this.chatBody) {
        this.chatBody.nativeElement.scrollTop = this.chatBody.nativeElement.scrollHeight;
      }
    }, 50);
  }
}
