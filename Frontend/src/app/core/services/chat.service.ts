import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Client, Message, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { AuthService } from './auth.service';

export interface ChatMessageDto {
  id?: number;
  content: string;
  senderId?: number;
  senderName?: string;
  senderAvatar?: string;
  senderAvatarUrl?: string;
  senderColor?: string;
  receiverId?: number;
  channelId?: string;
  timestamp?: string;
  isRead?: boolean;
  attachmentUrl?: string;
  attachmentName?: string;
  attachmentType?: string;
  isEdited?: boolean;
  isForwarded?: boolean;
  reactions?: { [emoji: string]: number[] };
  readAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private client: Client;
  private backendUrl = 'http://localhost:8080';
  public zone: NgZone;
  
  // Observables for real-time updates
  private messageSubject = new Subject<ChatMessageDto>();
  public messages$ = this.messageSubject.asObservable();

  private presenceSubject = new BehaviorSubject<Set<string>>(new Set());
  public presence$ = this.presenceSubject.asObservable();

  private readReceiptSubject = new Subject<{readBy: number, senderId: number, readAt?: string}>();
  public readReceipts$ = this.readReceiptSubject.asObservable();

  // For Topbar notifications
  private unreadMessagesSubject = new BehaviorSubject<ChatMessageDto[]>([]);
  public unreadMessages$ = this.unreadMessagesSubject.asObservable();
  
  public activeChatId: string | null = null; // Set by team-chat component
  private notificationSound = new Audio('https://assets.mixkit.co/active_storage/sfx/2869/2869-preview.mp3');

  // Active subscriptions
  private subscriptions: Map<string, StompSubscription> = new Map();

  constructor(private http: HttpClient, private authService: AuthService, zone: NgZone) {
    this.zone = zone;
    this.client = new Client({
      // We use webSocketFactory because standard WebSocket doesn't support custom headers in browser easily
      // SockJS handles fallbacks and connection
      webSocketFactory: () => new SockJS(`${this.backendUrl}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${this.authService.getToken()}`
      },
      debug: (str) => {
        // console.log(str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = (frame) => {
      console.log('Connected to Chat WebSocket');
      this.subscribeToPresence();
      this.subscribeToPrivateMessages();
      this.subscribeToReadReceipts();
    };

    this.client.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    // Connect automatically if authenticated
    if (this.authService.isAuthenticated()) {
      this.connect();
    }
  }

  // Connecte le client WebSocket (avec le token d'authentification)
  connect() {
    // Update token before connecting
    this.client.connectHeaders = {
      Authorization: `Bearer ${this.authService.getToken()}`
    };
    this.client.activate();
    this.fetchOnlineUsers();
  }

  // Déconnecte le client WebSocket
  disconnect() {
    this.client.deactivate();
  }

  // ---- REST API for History & Upload ----

  // Récupère l'historique des messages pour un canal spécifique via API REST
  getChannelMessages(channelId: string): Observable<ChatMessageDto[]> {
    return this.http.get<ChatMessageDto[]>(`${this.backendUrl}/api/chat/channel/${channelId}`);
  }

  // Récupère l'historique des messages privés entre deux utilisateurs via API REST
  getDirectMessages(userId1: number, userId2: number): Observable<ChatMessageDto[]> {
    return this.http.get<ChatMessageDto[]>(`${this.backendUrl}/api/chat/direct/${userId1}/${userId2}`);
  }

  // Récupère la liste des utilisateurs en ligne via API REST
  fetchOnlineUsers() {
    this.http.get<string[]>(`${this.backendUrl}/api/chat/presence`).subscribe(users => {
      this.presenceSubject.next(new Set(users));
    });
  }

  // Upload un fichier joint pour le chat via API REST
  uploadAttachment(file: File): Observable<{url: string, name: string, type: string}> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{url: string, name: string, type: string}>(`${this.backendUrl}/api/chat/upload`, formData);
  }

  // ---- WebSockets Publish / Subscribe ----

  // S'abonne aux messages d'un canal spécifique
  subscribeToChannel(channelId: string) {
    if (this.subscriptions.has(channelId)) return; // Already subscribed

    const sub = this.client.subscribe(`/topic/${channelId}`, (message: Message) => {
      const msgBody: ChatMessageDto = JSON.parse(message.body);
      this.zone.run(() => {
        this.messageSubject.next(msgBody);
        
        // Handle notifications
        const currentUserId = this.authService.getCurrentUser()?.userId;
        if (msgBody.senderId !== currentUserId) {
          if (this.activeChatId !== msgBody.channelId) {
            this.addUnreadMessage(msgBody);
            this.playNotificationSound();
          }
        }
      });
    });
    this.subscriptions.set(channelId, sub);
  }

  // Se désabonne d'un canal spécifique
  unsubscribeFromChannel(channelId: string) {
    const sub = this.subscriptions.get(channelId);
    if (sub) {
      sub.unsubscribe();
      this.subscriptions.delete(channelId);
    }
  }

  // S'abonne aux messages privés de l'utilisateur connecté
  private subscribeToPrivateMessages() {
    // Spring UserDestinationPrefix maps to /user/queue/messages
    const sub = this.client.subscribe(`/user/queue/messages`, (message: Message) => {
      const msgBody: ChatMessageDto = JSON.parse(message.body);
      this.zone.run(() => {
        this.messageSubject.next(msgBody);

        // Handle notifications
        const currentUserId = this.authService.getCurrentUser()?.userId;
        if (msgBody.senderId !== currentUserId && !msgBody.isRead) {
          if (this.activeChatId !== msgBody.senderId?.toString()) {
            this.addUnreadMessage(msgBody);
            this.playNotificationSound();
          }
        }
      });
    });
    this.subscriptions.set('private', sub);
  }

  // S'abonne aux mises à jour de présence (en ligne/hors ligne) des utilisateurs
  private subscribeToPresence() {
    const sub = this.client.subscribe(`/topic/presence`, (message: Message) => {
      const data = JSON.parse(message.body); // { email: string, isOnline: boolean }
      this.zone.run(() => {
        const currentPresence = new Set(this.presenceSubject.value);
        if (data.isOnline) {
          currentPresence.add(data.email);
        } else {
          currentPresence.delete(data.email);
        }
        this.presenceSubject.next(currentPresence);
      });
    });
    this.subscriptions.set('presence', sub);
  }

  // Envoie un nouveau message via WebSocket
  sendMessage(msg: ChatMessageDto) {
    this.client.publish({
      destination: `/app/chat.sendMessage`,
      body: JSON.stringify(msg)
    });
  }

  // Envoie une modification de message existant via WebSocket
  editMessage(messageId: number, content: string) {
    this.client.publish({
      destination: `/app/chat.editMessage`,
      body: JSON.stringify({ messageId, content })
    });
  }

  // Marque les messages d'un expéditeur comme lus via WebSocket
  markAsRead(senderId: number) {
    this.client.publish({
      destination: `/app/chat.markAsRead`,
      body: JSON.stringify({ senderId })
    });
    // Remove from unread list
    const unread = this.unreadMessagesSubject.value.filter(m => m.senderId !== senderId);
    this.unreadMessagesSubject.next(unread);
  }

  // Ajoute ou modifie une réaction sur un message via WebSocket
  reactToMessage(messageId: number, emoji: string) {
    this.client.publish({
      destination: `/app/chat.react`,
      body: JSON.stringify({ messageId, emoji })
    });
  }

  // Transfère un message existant vers un autre destinataire ou canal
  forwardMessage(originalMsg: ChatMessageDto, receiverId?: number, channelId?: string) {
    const forwarded: ChatMessageDto = {
      content: originalMsg.content,
      receiverId: receiverId,
      channelId: channelId,
      attachmentUrl: originalMsg.attachmentUrl,
      attachmentName: originalMsg.attachmentName,
      attachmentType: originalMsg.attachmentType,
      isForwarded: true
    };
    this.sendMessage(forwarded);
  }

  // S'abonne aux accusés de lecture (read receipts)
  private subscribeToReadReceipts() {
    const sub = this.client.subscribe(`/user/queue/read-receipts`, (message: Message) => {
      const data = JSON.parse(message.body);
      this.zone.run(() => {
        this.readReceiptSubject.next(data);
      });
    });
    this.subscriptions.set('read-receipts', sub);
  }

  // Ajoute un message non lu à la liste (pour les notifications)
  private addUnreadMessage(msg: ChatMessageDto) {
    // Only add if not already in the list
    const current = this.unreadMessagesSubject.value;
    if (!current.find(m => m.id === msg.id)) {
      this.unreadMessagesSubject.next([...current, msg]);
    }
  }

  // Joue le son de notification pour un nouveau message
  private playNotificationSound() {
    this.notificationSound.play().catch(e => console.log('Audio play blocked by browser', e));
  }

  // Efface les notifications de messages non lus pour un canal spécifique
  clearUnreadForChannel(channelId: string) {
    const unread = this.unreadMessagesSubject.value.filter(m => m.channelId !== channelId);
    this.unreadMessagesSubject.next(unread);
  }
}
