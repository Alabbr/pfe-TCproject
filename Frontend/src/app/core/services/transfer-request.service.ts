import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TransferRequest {
  id?: number;
  employeeId?: number;
  targetDepartmentId?: number;
  newJobPositionId?: number;
  comment: string;
  status?: string;
  requestDate?: string;
  employee?: any;
  targetDepartment?: any;
  newJobPosition?: any;
  requestedBy?: any;
}

@Injectable({
  providedIn: 'root'
})
export class TransferRequestService {
  private apiUrl = 'http://localhost:8080/api/transfers';

  constructor(private http: HttpClient) { }

  // Crée une nouvelle demande de transfert
  createRequest(data: { employeeId: number, targetDepartmentId: number, newJobPositionId?: number, comment: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}`, data);
  }

  // Récupère les demandes de transfert en attente
  getPendingRequests(): Observable<TransferRequest[]> {
    return this.http.get<TransferRequest[]>(`${this.apiUrl}/pending`);
  }

  // Approuve ou rejette une demande de transfert
  processRequest(requestId: number, isApproved: boolean): Observable<any> {
    return this.http.put(`${this.apiUrl}/${requestId}/process?isApproved=${isApproved}`, {});
  }

  // Récupère les demandes créées par l'utilisateur connecté
  getMyRequests(): Observable<TransferRequest[]> {
    return this.http.get<TransferRequest[]>(`${this.apiUrl}/my-requests`);
  }
}
