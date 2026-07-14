import { Component, ElementRef, ViewChild, AfterViewChecked, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../../../shared/services/toast';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

interface Conversation {
  id: number;
  title: string;
  updatedAt: string;
}

@Component({
  selector: 'app-rag-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rag-chat.html',
  styleUrl: './rag-chat.scss'
})
export class RagChatComponent implements OnInit, AfterViewChecked {
  @ViewChild('chatScroll') private chatScroll!: ElementRef;

  conversations: Conversation[] = [];
  currentConversationId: number | null = null;

  messages: ChatMessage[] = [
    {
      role: 'assistant',
      content: 'Bonjour ! Je suis l\'assistant IA de Tunisie Clearing. Je connais parfaitement toutes les lois, rapports annuels et règles de fonctionnement de l\'entreprise. Comment puis-je vous aider aujourd\'hui ?',
      timestamp: new Date()
    }
  ];
  
  newMessage = '';
  isLoading = false;
  isSidebarOpen = false;

  constructor(private http: HttpClient, private toastService: ToastService) {}

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  deleteConversation(event: Event, id: number) {
    event.stopPropagation();
    if (confirm('Êtes-vous sûr de vouloir supprimer cette discussion ?')) {
      this.http.delete(`http://localhost:8080/api/rag/conversations/${id}`).subscribe({
        next: () => {
          this.toastService.success('Succès', 'Discussion supprimée.');
          if (this.currentConversationId === id) {
            this.newConversation();
          }
          this.loadConversations();
        },
        error: (err) => {
          console.error(err);
          this.toastService.error('Erreur', 'Impossible de supprimer la discussion.');
        }
      });
    }
  }

  ngOnInit() {
    this.loadConversations();
  }

  loadConversations() {
    this.http.get<Conversation[]>('http://localhost:8080/api/rag/conversations').subscribe(res => {
      this.conversations = res;
    });
  }

  newConversation() {
    this.currentConversationId = null;
    this.messages = [{
      role: 'assistant',
      content: 'Bonjour ! Je suis l\'assistant IA de Tunisie Clearing. Je connais parfaitement toutes les lois, rapports annuels et règles de fonctionnement de l\'entreprise. Comment puis-je vous aider aujourd\'hui ?',
      timestamp: new Date()
    }];
  }

  selectConversation(id: number) {
    this.currentConversationId = id;
    this.http.get<any[]>(`http://localhost:8080/api/rag/conversations/${id}/messages`).subscribe({
      next: (res) => {
        this.messages = res.map(m => ({
          role: m.role,
          content: m.content,
          timestamp: new Date(m.createdAt)
        }));
      },
      error: (err) => {
        this.toastService.error('Erreur', 'Impossible de charger la conversation.');
      }
    });
  }

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  scrollToBottom(): void {
    try {
      this.chatScroll.nativeElement.scrollTop = this.chatScroll.nativeElement.scrollHeight;
    } catch(err) { }
  }

  sendMessage() {
    const text = this.newMessage.trim();
    if (!text) return;

    // Add user message
    this.messages.push({
      role: 'user',
      content: text,
      timestamp: new Date()
    });
    
    this.newMessage = '';
    this.isLoading = true;

    const payload: any = { question: text };
    if (this.currentConversationId) {
      payload.conversationId = this.currentConversationId;
    }

    // Call backend API
    this.http.post<{answer: string, conversationId: number}>('http://localhost:8080/api/rag/chat', payload).subscribe({
      next: (res) => {
        this.isLoading = false;
        this.messages.push({
          role: 'assistant',
          content: res.answer,
          timestamp: new Date()
        });
        
        if (!this.currentConversationId && res.conversationId) {
          this.currentConversationId = res.conversationId;
        }
        this.loadConversations();
      },
      error: (err) => {
        this.isLoading = false;
        console.error(err);
        this.toastService.error('Erreur IA', 'Désolé, je n\'ai pas pu joindre le serveur d\'intelligence artificielle.');
      }
    });
  }

  handleKeyDown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
}
