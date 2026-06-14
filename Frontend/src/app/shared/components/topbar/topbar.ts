import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthResponse } from '../../../core/models/auth.model';

import { ProfileModalComponent } from '../profile-modal/profile-modal';
import { ProfileModalService } from '../../services/profile-modal.service';
import { SettingsModalComponent } from '../settings-modal/settings-modal';
import { ChatService, ChatMessageDto } from '../../../core/services/chat.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule, ProfileModalComponent, SettingsModalComponent],
  templateUrl: './topbar.html',
  styleUrl: './topbar.scss'
})
export class Topbar implements OnInit {
  isProfileMenuOpen = false;
  isProfileModalOpen = false;
  isSettingsModalOpen = false;
  isNotificationsOpen = false;
  currentUser: AuthResponse | null = null;
  unreadMessages: ChatMessageDto[] = [];
  isDarkMode = false;

  constructor(
    private router: Router,
    private authService: AuthService,
    private profileModalService: ProfileModalService,
    private chatService: ChatService,
    private themeService: ThemeService
  ) {}

  ngOnInit() {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });

    this.profileModalService.openModal$.subscribe(() => {
      this.openProfileModal();
    });

    this.chatService.unreadMessages$.subscribe(messages => {
      this.unreadMessages = messages;
    });

    this.themeService.isDarkMode$.subscribe(isDark => {
      this.isDarkMode = isDark;
    });
  }

  toggleProfileMenu() {
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
    this.isNotificationsOpen = false;
  }

  toggleNotifications() {
    this.isNotificationsOpen = !this.isNotificationsOpen;
    this.isProfileMenuOpen = false;
  }

  closeAllMenus() {
    this.isProfileMenuOpen = false;
    this.isNotificationsOpen = false;
  }

  openProfileModal() {
    this.isProfileMenuOpen = false; // close the dropdown
    this.isProfileModalOpen = true;
  }

  openSettingsModal() {
    this.isProfileMenuOpen = false;
    this.isSettingsModalOpen = true;
  }

  closeSettingsModal() {
    this.isSettingsModalOpen = false;
  }

  closeProfileModal() {
    this.isProfileModalOpen = false;
  }

  onProfileUpdated(updatedUser: any) {
    // Optionally update local auth context if needed
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  goToChat(msg?: ChatMessageDto) {
    this.closeAllMenus();
    if (msg) {
      if (msg.channelId) {
        this.router.navigate(['/chat'], { queryParams: { type: 'channel', id: msg.channelId } });
      } else if (msg.senderId) {
        this.router.navigate(['/chat'], { queryParams: { type: 'dm', id: msg.senderId } });
      }
    } else {
      this.router.navigate(['/chat']);
    }
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }
}
