export interface Organization {
  id: string;
  name: string;
  description?: string;
  createdAt?: Date;
}

export interface UserOrganization {
  userId: string;
  organizationId: string;
  role: string;
  organization: Organization;
}