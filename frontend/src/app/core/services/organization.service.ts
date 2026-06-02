import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Organization, UserOrganization } from '../../shared/models/organization.model';

@Injectable({
  providedIn: 'root'
})
export class OrganizationService {
  private selectedOrganizationSubject = new BehaviorSubject<Organization | null>(null);
  public selectedOrganization$ = this.selectedOrganizationSubject.asObservable();

  constructor(private http: HttpClient) {
    // Charger l'organisation depuis le localStorage au démarrage
    const savedOrg = localStorage.getItem('selectedOrganization');
    if (savedOrg) {
      this.selectedOrganizationSubject.next(JSON.parse(savedOrg));
    }
  }

  getUserOrganizations(): Observable<UserOrganization[]> {
    return this.http.get<UserOrganization[]>(`${environment.apiUrl}/auth/user/organizations`);
  }

  selectOrganization(organization: Organization): Observable<any> {
    return this.http.post(`${environment.apiUrl}/auth/select-organization`, {
      organizationId: organization.id
    }).pipe(
      tap(() => {
        localStorage.setItem('selectedOrganization', JSON.stringify(organization));
        this.selectedOrganizationSubject.next(organization);
      })
    );
  }

  getSelectedOrganization(): Organization | null {
    return this.selectedOrganizationSubject.value;
  }

  clearSelectedOrganization(): void {
    localStorage.removeItem('selectedOrganization');
    this.selectedOrganizationSubject.next(null);
  }
}