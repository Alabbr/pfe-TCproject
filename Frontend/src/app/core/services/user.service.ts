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

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.API_URL);
  }

  getUser(id: number): Observable<User> {
    return this.http.get<User>(`${this.API_URL}/${id}`);
  }

  createUser(user: any): Observable<User> {
    return this.http.post<User>(this.API_URL, user);
  }

  updateUser(id: number, user: any): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/${id}`, user);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  getArchivedUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.API_URL}/archived`);
  }

  restoreUser(id: number): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/${id}/restore`, {});
  }

  permanentDeleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}/permanent`);
  }

  updateProfile(id: number, phoneNumber?: string, address?: string): Observable<User> {
    let params: any = {};
    if (phoneNumber) params.phoneNumber = phoneNumber;
    if (address) params.address = address;
    return this.http.put<User>(`${this.API_URL}/${id}/profile`, null, { params });
  }

  uploadProfilePicture(id: number, file: File): Observable<User> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<User>(`${this.API_URL}/${id}/profile-picture`, formData);
  }

  assignJobPosition(id: number, jobPositionId: number): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/${id}/assign-job?jobPositionId=${jobPositionId}`, {});
  }

  getUsersByDepartment(departmentId: number): Observable<User[]> {
    return this.http.get<User[]>(`${this.API_URL}/department/${departmentId}`);
  }
}
