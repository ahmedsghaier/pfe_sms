import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { OrganizationService } from '../services/organization.service';

export const organizationGuard: CanActivateFn = (route, state) => {
  const organizationService = inject(OrganizationService);
  const router = inject(Router);

  const selectedOrganization = organizationService.getSelectedOrganization();

  if (!selectedOrganization) {
    router.navigate(['/select-organization']);
    return false;
  }

  return true;
};