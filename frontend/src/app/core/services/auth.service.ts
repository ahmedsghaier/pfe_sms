import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private currentUserSubject = new BehaviorSubject<any>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    const storedUser = localStorage.getItem('currentUser');
    if (storedUser) {
      this.currentUserSubject.next(JSON.parse(storedUser));
    }
  }

  // 🔐 LOGIN
  login(credentials: { email: string; password: string }): Observable<any> {
  return this.http.post<any>(`${environment.apiUrl}/auth/login`, credentials).pipe(
    tap(response => {
      console.log('🔍 Réponse login complète:', response); // Debug

      // ✅ Correction ici
      const token = response?.data?.accessToken || response?.accessToken;

      if (!token) {
        console.error("❌ Token non trouvé dans la réponse", response);
        return;
      }

      console.log('✅ Token reçu avec succès');

      // 💾 Stockage
      localStorage.setItem('access_token', token);

      // 👤 User ID
      const user = response?.data?.user;
      if (user?.id) {
        localStorage.setItem('user_id', user.id);
        localStorage.setItem('currentUser', JSON.stringify({
          id: user.id,
          email: user.email,
          firstName: user.firstName,
          lastName: user.lastName
        }));

        this.currentUserSubject.next(user);
      }
    })
  );
}

  // 🔓 decode JWT
  private decodeToken(token: string): any {
    try {
      return JSON.parse(atob(token.split('.')[1]));
    } catch (e) {
      console.error("❌ Decode JWT error", e);
      return null;
    }
  }

  // 🚪 LOGOUT
  logout(): void {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('currentUser');
    localStorage.removeItem('selectedOrganization');
    localStorage.removeItem('user_id');

    this.currentUserSubject.next(null);
    this.router.navigate(['/auth/login']);
  }

  // 🔐 check auth
  isAuthenticated(): boolean {
    return !!localStorage.getItem('access_token');
  }

  // 🔑 get token
  getToken(): string | null {
    return localStorage.getItem('access_token');
  }

  // 🔄 password reset
  requestPasswordReset(email: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${environment.apiUrl}/auth/forgot-password`,
      { email }
    );
  }

  resetPassword(token: string, newPassword: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${environment.apiUrl}/auth/reset-password`,
      { token, newPassword }
    );
  }

  // 👤 user id
  getCurrentUserId(): string | null {
    const user = this.currentUserSubject.value;
    return user?.id || localStorage.getItem('user_id');
  }
}