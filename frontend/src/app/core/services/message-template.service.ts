// src/app/core/services/message-template.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MessageTemplate } from '../../shared/models/message-template.model';
import { ApiResponse } from '../../shared/models/api-response.model';

@Injectable({
  providedIn: 'root'
})
export class MessageTemplateService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/templates`;

  getTemplates(type?: 'CLASSIC' | 'TRANSACTIONAL'): Observable<ApiResponse<MessageTemplate[]>> {
    let params = new HttpParams();
    if (type) {
      params = params.set('type', type);
    }
    return this.http.get<ApiResponse<MessageTemplate[]>>(this.apiUrl, { params });
  }

  getTemplate(id: string): Observable<ApiResponse<MessageTemplate>> {
    return this.http.get<ApiResponse<MessageTemplate>>(`${this.apiUrl}/${id}`);
  }

  createTemplate(template: MessageTemplate): Observable<ApiResponse<MessageTemplate>> {
    return this.http.post<ApiResponse<MessageTemplate>>(this.apiUrl, template);
  }

  updateTemplate(id: string, template: MessageTemplate): Observable<ApiResponse<MessageTemplate>> {
    return this.http.put<ApiResponse<MessageTemplate>>(`${this.apiUrl}/${id}`, template);
  }

  deleteTemplate(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/${id}`);
  }

  toggleStatus(id: string): Observable<ApiResponse<MessageTemplate>> {
    return this.http.patch<ApiResponse<MessageTemplate>>(`${this.apiUrl}/${id}/toggle`, {});
  }
}