import { Injectable, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { ToastService } from '../../shared/services/toast';

@Injectable({
  providedIn: 'root'
})
export class InactivityTimeoutService {
  private timeoutId: any;
  private readonly TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes
  // For testing you can use 5000 (5 seconds)
  // private readonly TIMEOUT_MS = 5000; 

  constructor(
    private router: Router,
    private authService: AuthService,
    private ngZone: NgZone,
    private toastService: ToastService
  ) {
    this.initListener();
  }

  private initListener() {
    this.ngZone.runOutsideAngular(() => {
      // Listen to these events to detect user activity
      window.addEventListener('mousemove', () => this.resetTimer());
      window.addEventListener('click', () => this.resetTimer());
      window.addEventListener('keydown', () => this.resetTimer());
      window.addEventListener('scroll', () => this.resetTimer(), true);
    });
    
    // Initial start
    this.resetTimer();
  }

  public resetTimer() {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
    
    // Only set the timer if the user is currently authenticated
    if (this.authService.isAuthenticated()) {
      this.ngZone.runOutsideAngular(() => {
        this.timeoutId = setTimeout(() => this.logoutUser(), this.TIMEOUT_MS);
      });
    }
  }

  private logoutUser() {
    this.ngZone.run(() => {
      if (this.authService.isAuthenticated()) {
        this.authService.logout();
        this.router.navigate(['/login']);
        this.toastService.warning(
          'Déconnexion automatique',
          'Vous avez été déconnecté suite à une longue période d\'inactivité.'
        );
      }
    });
  }
}
