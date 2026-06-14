import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JobPosition } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class JobPositionService {
  private readonly API_URL = 'http://localhost:8080/api/job-positions';

  constructor(private http: HttpClient) {}

  getJobPositionsByDepartment(departmentId: number): Observable<JobPosition[]> {
    return this.http.get<JobPosition[]>(`${this.API_URL}/department/${departmentId}`);
  }
}
