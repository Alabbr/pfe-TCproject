import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Project, Task, TaskHistory, ProjectDocument } from './projects.models';

@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // Projects
  getProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.apiUrl}/projects`);
  }

  getProjectById(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.apiUrl}/projects/${id}`);
  }

  createProject(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/projects`, data);
  }

  updateProject(id: number, data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/projects/${id}`, data);
  }

  deleteProject(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/projects/${id}`);
  }

  // Tasks
  getTasksByProject(projectId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.apiUrl}/tasks/project/${projectId}`);
  }

  getMyTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.apiUrl}/tasks/my-tasks`);
  }

  createTask(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/tasks`, data);
  }

  updateTaskStatus(taskId: number, status: string, comment: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/tasks/${taskId}/status`, { status, comment });
  }

  getTaskHistory(taskId: number): Observable<TaskHistory[]> {
    return this.http.get<TaskHistory[]>(`${this.apiUrl}/tasks/${taskId}/history`);
  }

  // Documents
  getProjectDocuments(projectId: number): Observable<ProjectDocument[]> {
    return this.http.get<ProjectDocument[]>(`${this.apiUrl}/projects/documents/project/${projectId}`);
  }

  uploadDocument(projectId: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${this.apiUrl}/projects/documents/${projectId}`, formData);
  }

  deleteDocument(docId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/projects/documents/${docId}`);
  }
}
