import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
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

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });
  }
}
