import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';

import { OrganizationService } from '../../../core/services/organization.service';
import { Organization, UserOrganization } from '../../../shared/models/organization.model';

@Component({
  selector: 'app-select-organization',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  templateUrl: './select-organization.component.html',
  styleUrls: ['./select-organization.component.scss']
})
export class SelectOrganizationComponent implements OnInit {
  selectForm: FormGroup;
  organizations: Organization[] = [];
  loading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private organizationService: OrganizationService,
    private router: Router
  ) {
    this.selectForm = this.fb.group({
      organizationId: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadOrganizations();
  }

  loadOrganizations(): void {
    this.loading = true;
    this.organizationService.getUserOrganizations().subscribe({
      next: (userOrgs: UserOrganization[]) => {
        this.organizations = userOrgs.map(uo => uo.organization);
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = 'Erreur lors du chargement des organisations';
        console.error('Load organizations error:', err);
      }
    });
  }

  onSubmit(): void {
    if (this.selectForm.invalid) return;

    this.loading = true;
    this.errorMessage = '';

    const selectedOrg = this.organizations.find(
      org => org.id === this.selectForm.value.organizationId
    );

    if (!selectedOrg) {
      this.errorMessage = 'Organisation invalide';
      this.loading = false;
      return;
    }

    this.organizationService.selectOrganization(selectedOrg).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = 'Erreur lors de la sélection de l\'organisation';
        console.error('Select organization error:', err);
      }
    });
  }
}