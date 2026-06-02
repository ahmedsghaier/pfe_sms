import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Contact, CreateContactRequest, ImportResult, ContactSearchParams } from '../../shared/models/contact.model';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

@Injectable({
  providedIn: 'root'
})
export class ContactService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/contacts`;

  getContacts(params: ContactSearchParams = {}): Observable<ApiResponse<Page<Contact>>> {
    let httpParams = new HttpParams();
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http.get<ApiResponse<Page<Contact>>>(this.apiUrl, { params: httpParams });
  }

  searchContacts(query: string, params: ContactSearchParams = {}): Observable<ApiResponse<Page<Contact>>> {
    let httpParams = new HttpParams().set('query', query);

    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http.get<ApiResponse<Page<Contact>>>(`${this.apiUrl}/search`, { params: httpParams });
  }

  createContact(request: CreateContactRequest): Observable<ApiResponse<Contact>> {
    return this.http.post<ApiResponse<Contact>>(this.apiUrl, request);
  }

  importContacts(file: File): Observable<ApiResponse<ImportResult>> {
    // ✅ Vérification taille fichier côté client (30MB)
    if (file.size > 30 * 1024 * 1024) {
      throw new Error('File size exceeds 30MB limit');
    }

    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ApiResponse<ImportResult>>(`${this.apiUrl}/import`, formData);
  }

  addTagsToContacts(contactIds: string[], tags: string[]): Observable<ApiResponse<void>> {
    // ✅ Conversion en Set pour correspondre au backend (Set<String>)
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/bulk/add-tags`, {
      contactIds,
      tags: [...new Set(tags)]
    });
  }

  removeTagsFromContacts(contactIds: string[], tags: string[]): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/bulk/remove-tags`, {
      contactIds,
      tags: [...new Set(tags)]
    });
  }

  deleteContacts(contactIds: string[]): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/bulk`, {
      body: contactIds
    });
  }
}