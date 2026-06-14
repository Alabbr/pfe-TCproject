import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Department } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class DepartmentService {
  private readonly API_URL = 'http://localhost:8080/api/departments';

  constructor(private http: HttpClient) {}

  getDepartments(): Observable<Department[]> {
    return this.http.get<Department[]>(this.API_URL);
  }

  createDepartment(department: Partial<Department>): Observable<Department> {
    return this.http.post<Department>(this.API_URL, department);
  }

  updateDepartment(id: number, department: Partial<Department>): Observable<Department> {
    return this.http.put<Department>(`${this.API_URL}/${id}`, department);
  }

  deleteDepartment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  getArchivedDepartments(): Observable<Department[]> {
    return this.http.get<Department[]>(`${this.API_URL}/archived`);
  }

  restoreDepartment(id: number): Observable<void> {
    return this.http.put<void>(`${this.API_URL}/${id}/restore`, {});
  }

  permanentDeleteDepartment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}/permanent`);
  }
}
