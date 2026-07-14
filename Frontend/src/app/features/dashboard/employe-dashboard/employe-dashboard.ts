import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { DocumentService, DocumentStatsResponse } from '../../../core/services/document.service';
import { AuthResponse } from '../../../core/models/auth.model';

@Component({
  selector: 'app-employe-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './employe-dashboard.html',
  styleUrls: ['./employe-dashboard.scss']
})
export class EmployeDashboard implements OnInit {
  currentUser: AuthResponse | null = null;
  currentDate = new Date();
  docStats: DocumentStatsResponse | null = null;

  // Greeting based on time of day
  greeting = '';

  constructor(
    private authService: AuthService,
    private documentService: DocumentService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });

    this.documentService.getStats().subscribe({
      next: (stats) => this.docStats = stats,
      error: () => {}
    });

    this.setGreeting();
  }

  private setGreeting(): void {
    const hour = new Date().getHours();
    if (hour < 12) this.greeting = 'Bonjour';
    else if (hour < 18) this.greeting = 'Bon après-midi';
    else this.greeting = 'Bonsoir';
  }

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }

  getInitials(): string {
    if (!this.currentUser) return '?';
    const f = this.currentUser.fullName?.split(' ');
    if (f && f.length >= 2) return f[0].charAt(0) + f[1].charAt(0);
    return this.currentUser.fullName?.charAt(0) || '?';
  }
}
