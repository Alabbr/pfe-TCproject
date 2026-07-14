import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface GlobalAnnouncement {
  id: number;
  title: string;
  message: string;
  createdAt: string;
  isActive: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AnnouncementService {
  private apiUrl = 'http://localhost:8080/api/announcements';

  constructor(private http: HttpClient) {}

  // Récupère la liste des annonces globales actives
  getActiveAnnouncements(): Observable<GlobalAnnouncement[]> {
    return this.http.get<GlobalAnnouncement[]>(`${this.apiUrl}/active`);
  }
}
