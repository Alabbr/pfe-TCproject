import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { AuthService } from './auth.service';
import { ToastService } from '../../shared/services/toast';

export interface SystemNotification {
  id: number;
  title: string;
  message: string;
  type: string;
  actionUrl: string;
  isRead: boolean;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private client: Client;
  private backendUrl = '';
  
  private notificationsSubject = new BehaviorSubject<SystemNotification[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();

  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$ = this.unreadCountSubject.asObservable();

  // Emits real-time events for components (e.g. PROJECT_CREATED)
  private eventsSubject = new Subject<SystemNotification>();
  public events$ = this.eventsSubject.asObservable();

  private subscriptions: Map<string, StompSubscription> = new Map();

  constructor(
    private http: HttpClient, 
    private authService: AuthService, 
    private zone: NgZone,
    private toastService: ToastService
  ) {
    this.client = new Client({
      webSocketFactory: () => new SockJS(`${this.backendUrl}/ws`),
      connectHeaders: {
        Authorization: `Bearer ${this.authService.getToken()}`
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = (frame) => {
      console.log('Connected to Notifications WebSocket');
      this.subscribeToNotifications();
    };

    this.client.onStompError = (frame) => {
      console.error('Notification Broker error: ' + frame.headers['message']);
    };

    if (this.authService.isAuthenticated()) {
      this.connect();
    }
  }

  connect() {
    this.client.connectHeaders = {
      Authorization: `Bearer ${this.authService.getToken()}`
    };
    this.client.activate();
    this.fetchHistory();
  }

  disconnect() {
    this.client.deactivate();
  }

  private subscribeToNotifications() {
    const user = this.authService.getCurrentUser();
    if (!user) return;

    // Subscribe to private notifications
    const privateSub = this.client.subscribe('/user/queue/notifications', (message) => {
      const notif: SystemNotification = JSON.parse(message.body);
      this.handleNewNotification(notif);
    });
    this.subscriptions.set('private_notifs', privateSub);

    // Subscribe to global notifications
    const globalSub = this.client.subscribe('/topic/notifications', (message) => {
      const notif: SystemNotification = JSON.parse(message.body);
      this.handleNewNotification(notif);
    });
    this.subscriptions.set('global_notifs', globalSub);
  }

  private handleNewNotification(notif: SystemNotification) {
    this.zone.run(() => {
      // Add to list
      const current = this.notificationsSubject.value;
      this.notificationsSubject.next([notif, ...current]);
      
      // Update unread count
      this.updateUnreadCount();

      // Show toast
      this.toastService.info(notif.title, notif.message);

      // Emit event for components that need to refresh data
      this.eventsSubject.next(notif);
    });
  }

  private fetchHistory() {
    this.http.get<SystemNotification[]>(`${this.backendUrl}/api/notifications`).subscribe({
      next: (notifs) => {
        this.notificationsSubject.next(notifs);
        this.updateUnreadCount();
      },
      error: (err) => console.error('Failed to fetch notifications history', err)
    });
  }

  markAllAsRead() {
    this.http.post(`${this.backendUrl}/api/notifications/mark-all-read`, {}).subscribe({
      next: () => {
        const updated = this.notificationsSubject.value.map(n => ({...n, isRead: true}));
        this.notificationsSubject.next(updated);
        this.updateUnreadCount();
      }
    });
  }

  private updateUnreadCount() {
    const count = this.notificationsSubject.value.filter(n => !n.isRead).length;
    this.unreadCountSubject.next(count);
  }
}
