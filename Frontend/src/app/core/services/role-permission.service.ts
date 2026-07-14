import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RolePermission {
  roleName: string;
  permissions: string[];
}

@Injectable({
  providedIn: 'root'
})
export class RolePermissionService {
  private apiUrl = 'http://localhost:8080/api/roles';

  constructor(private http: HttpClient) { }

  // Récupère toutes les permissions par rôle
  getAllRolePermissions(): Observable<RolePermission[]> {
    return this.http.get<RolePermission[]>(`${this.apiUrl}/permissions`);
  }

  // Récupère les permissions spécifiques d'un rôle
  getRolePermissions(roleName: string): Observable<RolePermission> {
    return this.http.get<RolePermission>(`${this.apiUrl}/${roleName}/permissions`);
  }

  // Met à jour les permissions pour un rôle
  updateRolePermissions(roleName: string, permissions: string[]): Observable<RolePermission> {
    return this.http.put<RolePermission>(`${this.apiUrl}/${roleName}/permissions`, permissions);
  }
}
