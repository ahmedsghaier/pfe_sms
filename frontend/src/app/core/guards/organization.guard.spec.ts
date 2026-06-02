import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { organizationGuard } from './organization.guard';

describe('organizationGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => organizationGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
