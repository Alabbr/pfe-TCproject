import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';


@Injectable({
  providedIn: 'root'
})
export class AgentService {
  private apiUrl = '/api/agent';

  constructor(private http: HttpClient) {}

  executeCommand(command: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/execute`, { command });
  }
}
