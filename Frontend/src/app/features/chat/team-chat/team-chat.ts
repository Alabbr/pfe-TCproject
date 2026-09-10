import { Component, OnInit, OnDestroy, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService, ChatMessageDto } from '../../../core/services/chat.service';
import { UserService } from '../../../core/services/user.service';
import { DepartmentService } from '../../../core/services/department.service';
import { AuthService } from '../../../core/services/auth.service';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { RoleFormatPipe } from '../../../shared/pipes/role-format.pipe';

@Component({
  selector: 'app-team-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, RoleFormatPipe],
  templateUrl: './team-chat.html',
  styleUrl: './team-chat.scss',
  encapsulation: ViewEncapsulation.None
})
export class TeamChat implements OnInit, OnDestroy {
  channels: any[] = [];
  directMessages: any[] = [];
  members: any[] = [];
  
  activeItemType: 'channel' | 'dm' = 'channel';
  activeItemId: string = 'general';
  
  activeMessages: ChatMessageDto[] = [];
  newMessageText: string = '';
  currentUser: any;

  selectedFile: File | null = null;
  selectedFilePreview: string | ArrayBuffer | null = null;
  isUploading: boolean = false;

  // Editing state
  editingMessageId: number | null = null;
  editingOriginalContent: string = '';

  // Reaction emoji picker
  showEmojiPickerForMessageId: number | null = null;
  availableEmojis: string[] = ['👍', '❤️', '😂', '😮', '😢', '🔥'];

  // Forward modal
  showForwardModal: boolean = false;
  forwardingMessage: ChatMessageDto | null = null;

  // Hover menu
  hoveredMessageId: number | null = null;
  
  // Voice Recording
  isRecording = false;
  mediaRecorder: MediaRecorder | null = null;
  audioChunks: Blob[] = [];
  recordingTime = 0;
  recordingInterval: any;

  private messageSub!: Subscription;
  private presenceSub!: Subscription;
  private readReceiptSub!: Subscription;

  constructor(
    private chatService: ChatService,
    private userService: UserService,
    private departmentService: DepartmentService,
    private authService: AuthService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.currentUser = this.authService.getCurrentUser();
    this.loadChannels();
    this.loadUsers();

    this.route.queryParams.subscribe(params => {
      const type = params['type'];
      const id = params['id'];
      if (type === 'dm' && id) {
        // slight delay to ensure users are loaded
        setTimeout(() => this.selectDM(id), 100);
      } else if (type === 'channel' && id) {
        setTimeout(() => this.selectChannel(id), 100);
      }
    });

    // Subscribe to new messages via WebSocket
    this.messageSub = this.chatService.messages$.subscribe((msg: ChatMessageDto) => {
      // Check if this is an update to an existing message (edit or reaction)
      const existingIdx = this.activeMessages.findIndex(m => m.id === msg.id);
      if (existingIdx !== -1) {
        this.activeMessages[existingIdx] = msg;
        return;
      }

      // If message belongs to active channel
      if (this.activeItemType === 'channel' && msg.channelId === this.activeItemId) {
        this.activeMessages.push(msg);
        this.scrollToBottom();
      } 
      // If message is a DM and belongs to active DM conversation
      else if (this.activeItemType === 'dm' && 
               (msg.senderId === Number(this.activeItemId) || msg.receiverId === Number(this.activeItemId))) {
        this.activeMessages.push(msg);
        // Auto mark as read when receiving
        if (msg.senderId === Number(this.activeItemId)) {
          this.chatService.markAsRead(msg.senderId!);
        }
        this.scrollToBottom();
      }
      // Else, increment unread counter
      else {
        if (msg.channelId) {
          const ch = this.channels.find(c => c.id === msg.channelId);
          if (ch) ch.unread++;
        } else if (msg.senderId) {
          const dm = this.directMessages.find(d => d.id === msg.senderId!.toString());
          if (dm) dm.unread = (dm.unread || 0) + 1;
        }
      }
    });

    // Subscribe to read receipts
    this.readReceiptSub = this.chatService.readReceipts$.subscribe((data) => {
      // Mark all my messages to that user as read
      this.activeMessages.forEach(m => {
        if (m.senderId === this.currentUser.userId && m.receiverId === data.readBy) {
          m.isRead = true;
          if (data.readAt) {
            m.readAt = data.readAt;
          }
        }
      });
    });

    // Subscribe to online presence
    this.presenceSub = this.chatService.presence$.subscribe((onlineEmails: Set<string>) => {
      this.directMessages.forEach(user => {
        user.online = onlineEmails.has(user.email);
      });
      this.members.forEach(member => {
        member.online = onlineEmails.has(member.email);
      });
    });
  }

  ngOnDestroy() {
    if (this.messageSub) this.messageSub.unsubscribe();
    if (this.presenceSub) this.presenceSub.unsubscribe();
    if (this.readReceiptSub) this.readReceiptSub.unsubscribe();
    if (this.activeItemType === 'channel') {
      this.chatService.unsubscribeFromChannel(this.activeItemId);
    }
    this.chatService.activeChatId = null;
  }

  loadChannels() {
    this.channels = [
      { id: 'general', name: 'général', unread: 0, desc: 'Canal général de Tunisie Clearing · Annonces et discussions transverses' }
    ];

    const isManagement = ['SUPER_ADMIN', 'DIRECTEUR_GENERAL', 'DIRECTEUR'].includes(this.currentUser.role);
    if (isManagement) {
      this.channels.push({
        id: 'direction',
        name: 'direction',
        unread: 0,
        desc: 'Canal réservé à la Direction et aux Chefs de Département'
      });
    }
    
    this.departmentService.getDepartments().subscribe(depts => {
      // Les RH et SUPER_ADMIN peuvent potentiellement voir tous les canaux, sinon seulement le canal de leur département
      const allowedDepts = (this.currentUser.role === 'SUPER_ADMIN' || this.currentUser.role === 'RH') 
        ? depts 
        : depts.filter(d => d.id === this.currentUser.departmentId);

      allowedDepts.forEach(d => {
        this.channels.push({
          id: 'dept_' + d.id,
          name: d.name.toLowerCase().replace(/\s+/g, '-'),
          unread: 0,
          desc: 'Canal du département: ' + d.name
        });
      });
      this.selectChannel('general');
    });
  }

  loadUsers() {
    this.userService.getUsers().subscribe(users => {
      this.members = users.map(u => ({
        id: u.id.toString(),
        name: u.firstName + ' ' + u.lastName,
        email: u.email,
        role: u.role,
        jobPositionName: u.jobPositionName,
        avatar: u.firstName.charAt(0) + u.lastName.charAt(0),
        profilePictureUrl: u.profilePictureUrl,
        color: u.profilePictureUrl ? 'transparent' : '#1A4A8A', // fallback color
        online: false
      }));
      // Remove current user from DM list
      this.directMessages = this.members.filter(u => u.id !== this.currentUser.userId.toString());
      // Re-trigger presence mapping
      this.chatService.fetchOnlineUsers();
    });
  }

  selectChannel(id: string) {
    if (this.activeItemType === 'channel' && this.activeItemId !== id) {
      this.chatService.unsubscribeFromChannel(this.activeItemId);
    }
    this.activeItemType = 'channel';
    this.activeItemId = id;
    this.chatService.activeChatId = id;
    
    const channel = this.channels.find(c => c.id === id);
    if (channel) channel.unread = 0;

    this.chatService.clearUnreadForChannel(id);
    this.chatService.subscribeToChannel(id);
    this.chatService.getChannelMessages(id).subscribe(msgs => {
      this.activeMessages = msgs;
      this.scrollToBottom();
    });
  }

  selectDM(id: string) {
    if (this.activeItemType === 'channel') {
      this.chatService.unsubscribeFromChannel(this.activeItemId);
    }
    this.activeItemType = 'dm';
    this.activeItemId = id;
    this.chatService.activeChatId = id;
    
    const dm = this.directMessages.find(d => d.id === id);
    if (dm) dm.unread = 0;

    this.chatService.getDirectMessages(this.currentUser.userId, Number(id)).subscribe(msgs => {
      this.activeMessages = msgs;
      this.scrollToBottom();
      // Mark messages from that user as read
      this.chatService.markAsRead(Number(id));
    });
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = e => this.selectedFilePreview = reader.result;
        reader.readAsDataURL(file);
      } else {
        this.selectedFilePreview = null;
      }
    }
  }

  removeSelectedFile() {
    this.selectedFile = null;
    this.selectedFilePreview = null;
  }

  sendMessage() {
    if ((!this.newMessageText.trim() && !this.selectedFile) || this.isUploading || this.isRecording) return;

    if (this.selectedFile) {
      this.isUploading = true;
      this.chatService.uploadAttachment(this.selectedFile).subscribe({
        next: (res) => {
          this.isUploading = false;
          this.sendWebSocketMessage(res.url, res.name, res.type);
          this.removeSelectedFile();
        },
        error: (err) => {
          console.error('Error uploading file', err);
          this.isUploading = false;
        }
      });
    } else {
      this.sendWebSocketMessage();
    }
  }

  // ---- Voice Recording ----
  startRecording() {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      alert("L'enregistrement audio n'est pas supporté par votre navigateur.");
      return;
    }

    navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
      this.isRecording = true;
      this.audioChunks = [];
      this.recordingTime = 0;
      this.mediaRecorder = new MediaRecorder(stream);
      
      this.recordingInterval = setInterval(() => {
        this.recordingTime++;
      }, 1000);

      this.mediaRecorder.ondataavailable = event => {
        if (event.data.size > 0) {
          this.audioChunks.push(event.data);
        }
      };

      this.mediaRecorder.onstop = () => {
        clearInterval(this.recordingInterval);
        stream.getTracks().forEach(track => track.stop());
        
        if (this.audioChunks.length > 0 && this.isRecording) {
          const audioBlob = new Blob(this.audioChunks, { type: 'audio/webm' });
          const file = new File([audioBlob], `vocal_${new Date().getTime()}.webm`, { type: 'audio/webm' });
          this.uploadVoiceMessage(file);
        }
        this.isRecording = false;
      };

      this.mediaRecorder.start();
    }).catch(err => {
      console.error('Microphone access denied or error', err);
      alert('Veuillez autoriser l\'accès au microphone pour envoyer des messages vocaux.');
    });
  }

  stopRecording() {
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stop();
    }
  }

  cancelRecording() {
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.isRecording = false; // Prevents the upload in onstop
      this.mediaRecorder.stop();
      clearInterval(this.recordingInterval);
    }
  }

  uploadVoiceMessage(file: File) {
    this.isUploading = true;
    this.chatService.uploadAttachment(file).subscribe({
      next: (res) => {
        this.isUploading = false;
        // Even if Cloudinary returns 'raw' or 'video', we force it as 'audio' for voice messages
        this.sendWebSocketMessage(res.url, 'Message vocal', 'audio');
      },
      error: (err) => {
        console.error('Error uploading voice message', err);
        this.isUploading = false;
      }
    });
  }

  formatRecordingTime(seconds: number): string {
    const m = Math.floor(seconds / 60).toString().padStart(2, '0');
    const s = (seconds % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  }

  private sendWebSocketMessage(attachmentUrl?: string, attachmentName?: string, attachmentType?: string) {
    const msg: ChatMessageDto = {
      content: this.newMessageText,
    };

    if (attachmentUrl) {
      msg.attachmentUrl = attachmentUrl;
      msg.attachmentName = attachmentName;
      msg.attachmentType = attachmentType;
    }

    if (this.activeItemType === 'channel') {
      msg.channelId = this.activeItemId;
    } else {
      msg.receiverId = Number(this.activeItemId);
    }

    this.chatService.sendMessage(msg);
    this.newMessageText = '';
  }

  get activeChannel() {
    return this.channels.find(c => c.id === this.activeItemId);
  }

  get activeDM() {
    return this.directMessages.find(d => d.id === this.activeItemId);
  }

  formatTime(dateString?: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  private scrollToBottom() {
    setTimeout(() => {
      const container = document.querySelector('.chat-messages');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    }, 100);
  }

  openImage(url: string) {
    window.open(url, '_blank');
  }

  downloadFile(url: string) {
    window.open(url, '_blank');
  }

  getSharedFiles(): any[] {
    return this.activeMessages
      .filter(m => m.attachmentUrl)
      .map(m => ({
        url: m.attachmentUrl,
        name: m.attachmentName || 'Fichier',
        type: m.attachmentType
      }));
  }

  // ---- Edit Message ----
  startEditing(msg: ChatMessageDto) {
    this.editingMessageId = msg.id!;
    this.editingOriginalContent = msg.content;
    this.newMessageText = msg.content;
    this.hoveredMessageId = null;
  }

  cancelEditing() {
    this.editingMessageId = null;
    this.editingOriginalContent = '';
    this.newMessageText = '';
  }

  submitEdit() {
    if (!this.editingMessageId || !this.newMessageText.trim()) return;
    this.chatService.editMessage(this.editingMessageId, this.newMessageText);
    this.editingMessageId = null;
    this.editingOriginalContent = '';
    this.newMessageText = '';
  }

  // ---- Reactions ----
  toggleEmojiPicker(msgId: number) {
    this.showEmojiPickerForMessageId = this.showEmojiPickerForMessageId === msgId ? null : msgId;
    this.hoveredMessageId = null;
  }

  addReaction(msg: ChatMessageDto, emoji: string) {
    if (!msg.id) return;
    this.chatService.reactToMessage(msg.id, emoji);
    this.showEmojiPickerForMessageId = null;
  }

  getReactionEntries(reactions?: { [emoji: string]: number[] }): { emoji: string, userIds: number[] }[] {
    if (!reactions) return [];
    return Object.entries(reactions).map(([emoji, userIds]) => ({ emoji, userIds }));
  }

  hasMyReaction(userIds: number[]): boolean {
    return userIds.includes(this.currentUser.userId);
  }

  // ---- Forward ----
  openForwardModal(msg: ChatMessageDto) {
    this.forwardingMessage = msg;
    this.showForwardModal = true;
    this.hoveredMessageId = null;
  }

  closeForwardModal() {
    this.showForwardModal = false;
    this.forwardingMessage = null;
  }

  forwardTo(type: 'dm' | 'channel', targetId: string) {
    if (!this.forwardingMessage) return;
    if (type === 'dm') {
      this.chatService.forwardMessage(this.forwardingMessage, Number(targetId));
    } else {
      this.chatService.forwardMessage(this.forwardingMessage, undefined, targetId);
    }
    this.closeForwardModal();
  }
}
