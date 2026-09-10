import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Department } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class DepartmentService {
  private readonly API_URL = '/api/departments';

  constructor(private http: HttpClient) {}

  // Récupère la liste de tous les départements actifs
  getDepartments(): Observable<Department[]> {
    return this.http.get<Department[]>(this.API_URL);
  }

  // Crée un nouveau département
  createDepartment(department: Partial<Department>): Observable<Department> {
    return this.http.post<Department>(this.API_URL, department);
  }

  // Met à jour un département existant
  updateDepartment(id: number, department: Partial<Department>): Observable<Department> {
    return this.http.put<Department>(`${this.API_URL}/${id}`, department);
  }

  // Supprime un département (archivage logique)
  deleteDepartment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  // Récupère la liste des départements archivés
  getArchivedDepartments(): Observable<Department[]> {
    return this.http.get<Department[]>(`${this.API_URL}/archived`);
  }

  // Restaure un département archivé
  restoreDepartment(id: number): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/${id}/restore`, {});
  }

  // Supprime définitivement un département
  permanentDeleteDepartment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}/permanent`);
  }
}
