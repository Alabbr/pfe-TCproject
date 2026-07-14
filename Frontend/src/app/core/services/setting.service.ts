import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SettingService {
  private readonly API_URL = 'http://localhost:8080/api/settings';

  constructor(private http: HttpClient) {}

  // Récupère la valeur d'un paramètre par sa clé
  getSetting(key: string): Observable<{key: string, value: string}> {
    return this.http.get<{key: string, value: string}>(`${this.API_URL}/${key}`);
  }

  // Upload une nouvelle image d'arrière-plan de connexion
  uploadLoginBackground(file: File): Observable<{key: string, value: string}> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{key: string, value: string}>(`${this.API_URL}/login-background`, formData);
  }
}
