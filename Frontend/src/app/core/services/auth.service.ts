import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { AuthResponse } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = '/api/auth';
  private readonly TOKEN_KEY = 'tc_hub_token';
  private readonly USER_KEY = 'tc_hub_user';

  private currentUserSubject = new BehaviorSubject<AuthResponse | null>(this.getStoredUser());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  // Authentifie l'utilisateur avec ses identifiants et enregistre le token et les données utilisateur
  login(credentials: any): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, credentials).pipe(
      tap(response => {
        if (response && response.token) {
          localStorage.setItem(this.TOKEN_KEY, response.token);
          localStorage.setItem(this.USER_KEY, JSON.stringify(response));
          this.currentUserSubject.next(response);
        }
      })
    );
  }

  /**
   * Fetch fresh user data from the backend without re-login.
   * Called on page refresh to pick up any permission changes made by an admin.
   */
  refreshCurrentUser(): void {
    if (!this.getToken()) return;

    this.http.get<AuthResponse>(`${this.API_URL}/me`).subscribe({
      next: (response) => {
        if (response) {
          // Keep the existing token, update everything else
          const existingToken = this.getToken();
          response.token = existingToken || response.token;
          localStorage.setItem(this.USER_KEY, JSON.stringify(response));
          this.currentUserSubject.next(response);
        }
      },
      error: (err) => {
        console.error('Failed to refresh user data', err);
        // If 401, the token is invalid — logout
        if (err.status === 401) {
          this.logout();
        }
      }
    });
  }

  // Déconnecte l'utilisateur en supprimant le token et les données locales
  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);
  }

  // Retourne le token JWT stocké en local
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  // Retourne les données de l'utilisateur actuellement connecté
  getCurrentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }

  // Vérifie si l'utilisateur est authentifié (présence du token)
  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  // Vérifie si l'utilisateur possède l'un des rôles spécifiés
  hasRole(roles: string[]): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;
    return roles.includes(user.role);
  }

  // Vérifie si l'utilisateur possède une permission spécifique
  hasPermission(permission: string): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;
    if (user.role === 'SUPER_ADMIN' || user.role === 'DIRECTEUR_GENERAL') return true;
    return user.permissions?.includes(permission) ?? false;
  }

  // Récupère les données utilisateur depuis le localStorage
  private getStoredUser(): AuthResponse | null {
    const storedUser = localStorage.getItem(this.USER_KEY);
    if (storedUser) {
      try {
        return JSON.parse(storedUser);
      } catch (e) {
        return null;
      }
    }
    return null;
  }
}
