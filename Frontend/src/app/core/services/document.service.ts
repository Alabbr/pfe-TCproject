import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';


export interface DocumentResponse {
  id: number;
  title: string;
  description: string;
  fileUrl: string;
  fileType: string;
  originalFileName: string;
  fileSize: number;
  isPublic: boolean;
  uploadDate: string;
  uploader: any;
}

export interface DocumentInboxResponse {
  id: number;
  document: DocumentResponse;
  isRead: boolean;
  readAt?: string;
  validationStatus: string;
  recipient?: any;
}

export interface DocumentStats {
  totalSent: number;
  totalReceived: number;
  unreadCount: number;
  publicDocsCount: number;
  pendingCount: number;
  pendingRequestsCount: number;
  assignedTasksCount: number;
}

export interface DocumentStatsResponse {
  totalSent: number;
  totalReceived: number;
  unreadCount: number;
  publicDocsCount: number;
  pendingCount: number;
  pendingRequestsCount: number;
  assignedTasksCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private apiUrl = '/api/documents';

  constructor(private http: HttpClient) {}

  // Upload un document avec ses métadonnées (titre, destinataires, etc.)
  uploadDocument(file: File, title: string, description: string, isPublic: boolean, recipientIds: number[], targetDepartmentId?: number, skipValidation: boolean = false, needsValidation: boolean = false): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    if (description) formData.append('description', description);
    formData.append('isPublic', String(isPublic));
    if (!isPublic && recipientIds && recipientIds.length > 0) {
      formData.append('recipientIds', recipientIds.join(','));
    }
    if (!isPublic && targetDepartmentId) {
      formData.append('targetDepartmentId', targetDepartmentId.toString());
    }
    formData.append('skipValidation', String(skipValidation));
    formData.append('needsValidation', String(needsValidation));

    // Do NOT set Content-Type header for FormData, browser sets it automatically with boundary
    return this.http.post<DocumentResponse>(`${this.apiUrl}/upload`, formData);
  }

  // Récupère l'historique des documents uploadés par l'utilisateur (ou globaux pour admin)
  getUploadHistory(): Observable<DocumentInboxResponse[]> {
    return this.http.get<DocumentInboxResponse[]>(`${this.apiUrl}/upload-history`);
  }

  // Récupère tous les documents publics
  getPublicDocuments(): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(`${this.apiUrl}/public`);
  }

  // Récupère les documents publics ciblant un département spécifique
  getDepartmentPublicDocs(departmentId: number): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(`${this.apiUrl}/department/${departmentId}`);
  }

  // Récupère les demandes de documents pour un département spécifique
  getDepartmentRequests(departmentId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/requests/department/${departmentId}`);
  }

  // Marque une demande de document comme traitée (complétée)
  fulfillRequest(requestId: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/requests/${requestId}/fulfill`, {});
  }

  // Assigne une demande de document à un employé spécifique
  assignRequest(requestId: number, assigneeId: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/requests/${requestId}/assign?assigneeId=${assigneeId}`, {});
  }

  // Délègue une demande de document à un autre employé
  delegateRequest(requestId: number, delegatedToId: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/requests/${requestId}/delegate?delegatedToId=${delegatedToId}`, {});
  }

  // Récupère les demandes assignées ou déléguées à l'utilisateur connecté
  getAssignedRequests(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/requests/assigned`);
  }

  // Récupère la boîte de réception (documents reçus) de l'utilisateur connecté
  getMyInbox(): Observable<DocumentInboxResponse[]> {
    return this.http.get<DocumentInboxResponse[]>(`${this.apiUrl}/private/me`);
  }

  // Marque un document reçu comme lu
  markAsRead(documentRecipientId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/private/${documentRecipientId}/read`, {});
  }

  // Récupère les statistiques d'utilisation des documents
  getStats(): Observable<DocumentStatsResponse> {
    return this.http.get<DocumentStatsResponse>(`${this.apiUrl}/stats`);
  }

  // Récupère le nombre de documents en attente de validation
  getPendingValidationCount(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/stats/pending-count`);
  }

  // Récupère la liste des documents en attente de validation
  getPendingValidations(): Observable<DocumentInboxResponse[]> {
    return this.http.get<DocumentInboxResponse[]>(`${this.apiUrl}/pending-validation`);
  }

  // Valide un document en attente
  validateDocument(recipientId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/validate/${recipientId}`, {});
  }

  // Rejette un document en attente
  rejectDocument(recipientId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/reject/${recipientId}`, {});
  }

  // Récupère les documents envoyés par l'utilisateur connecté avec leur statut
  getMySentDocs(): Observable<DocumentInboxResponse[]> {
    return this.http.get<DocumentInboxResponse[]>(`${this.apiUrl}/my-sent-docs`);
  }

  // Télécharge un document (retourne un objet Blob)
  downloadDocument(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${id}`, { responseType: 'blob' });
  }
}
