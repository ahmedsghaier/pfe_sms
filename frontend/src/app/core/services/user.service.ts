import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { User,InviteUserPayload } from '../../shared/models/user';
@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private http: HttpClient) {}

  getUsers(filters?: any) {
    return this.http.get<User[]>('/api/users', { params: filters });
  }

  inviteUser(payload: InviteUserPayload) {
    return this.http.post('/api/users/invite', payload);
  }

  updateUser(id: number, payload: any) {
    return this.http.put(`/api/users/${id}`, payload);
  }

  toggleStatus(id: number, active: boolean) {
    return this.http.patch(`/api/users/${id}/status`, { active });
  }

  deleteInvitation(id: number) {
    return this.http.delete(`/api/users/${id}/invitation`);
  }
}
