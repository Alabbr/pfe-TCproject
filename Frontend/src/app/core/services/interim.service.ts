import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InterimDelegation {
  id: number;
  delegator: any;
  interim: any;
  startDate: string;
  endDate: string;
  isActive: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class InterimService {
  private apiUrl = '/api/interim';

  constructor(private http: HttpClient) {}

  // Assigne un intérimaire pour une période donnée
  assignInterim(interimUserId: number, startDate: string, endDate: string): Observable<InterimDelegation> {
    const formData = new FormData();
    formData.append('interimUserId', interimUserId.toString());
    formData.append('startDate', startDate);
    formData.append('endDate', endDate);
    return this.http.post<InterimDelegation>(`${this.apiUrl}/assign`, formData);
  }

  // Récupère les délégations actives
  getActiveDelegations(): Observable<InterimDelegation[]> {
    return this.http.get<InterimDelegation[]>(`${this.apiUrl}/active`);
  }

  // Révoque une délégation d'intérim
  revokeDelegation(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/revoke/${id}`, {});
  }
}
