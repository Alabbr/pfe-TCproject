import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly API_URL = 'http://localhost:8080/api/users';

  constructor(private http: HttpClient) {}

  // Récupère la liste de tous les utilisateurs actifs
  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.API_URL);
  }

  // Récupère les détails d'un utilisateur par son ID
  getUser(id: number): Observable<User> {
    return this.http.get<User>(`${this.API_URL}/${id}`);
  }

  // Crée un nouvel utilisateur
  createUser(user: any): Observable<User> {
    return this.http.post<User>(this.API_URL, user);
  }

  // Met à jour un utilisateur existant
  updateUser(id: number, user: any): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/${id}`, user);
  }

  // Supprime un utilisateur (archivage)
  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  // Récupère la liste des utilisateurs archivés
  getArchivedUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.API_URL}/archived`);
  }

  // Restaure un utilisateur archivé
  restoreUser(id: number): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/${id}/restore`, {});
  }

  // Supprime définitivement un utilisateur archivé
  permanentDeleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}/permanent`);
  }

  // Met à jour le profil personnel de l'utilisateur (téléphone, adresse)
  updateProfile(id: number, phoneNumber?: string, address?: string): Observable<User> {
    let params: any = {};
    if (phoneNumber) params.phoneNumber = phoneNumber;
    if (address) params.address = address;
    return this.http.put<User>(`${this.API_URL}/${id}/profile`, null, { params });
  }

  // Upload et met à jour la photo de profil de l'utilisateur
  uploadProfilePicture(id: number, file: File): Observable<User> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<User>(`${this.API_URL}/${id}/profile-picture`, formData);
  }

  // Affecte un poste de travail à un utilisateur
  assignJobPosition(id: number, jobPositionId: number): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/${id}/assign-job?jobPositionId=${jobPositionId}`, {});
  }

  // Récupère la liste des utilisateurs d'un département
  getUsersByDepartment(departmentId: number): Observable<User[]> {
    return this.http.get<User[]>(`${this.API_URL}/department/${departmentId}`);
  }

  // Met à jour les permissions spécifiques d'un utilisateur
  updateUserPermissions(userId: number, permissions: string[]): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/${userId}/permissions`, permissions);
  }
}
