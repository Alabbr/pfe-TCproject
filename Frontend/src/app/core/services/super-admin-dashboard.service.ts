import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChartData {
  name: string;
  value: number;
}

export interface TopUser {
  id: number;
  fullName: string;
  role: string;
  departmentName: string;
  profilePictureUrl: string;
  lastLogin: string;
  activityScore: number;
}

export interface PendingUser {
  id: number;
  fullName: string;
  email: string;
  role: string;
  departmentName: string;
  requestedAt: string;
}

export interface SuperAdminStats {
  totalUsers: number;
  totalDepartments: number;
  activeUsersToday: number;
  pendingUsersCount: number;
  departmentDistribution: ChartData[];
  roleDistribution: ChartData[];
  topUsers: TopUser[];
  pendingUsers: PendingUser[];
}

@Injectable({
  providedIn: 'root'
})
export class SuperAdminDashboardService {
  private apiUrl = 'http://localhost:8080/api/dashboard/super-admin';

  constructor(private http: HttpClient) {}

  getStats(): Observable<SuperAdminStats> {
    return this.http.get<SuperAdminStats>(`${this.apiUrl}/stats`);
  }
}
