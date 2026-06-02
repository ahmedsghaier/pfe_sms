export interface Organization {
  id: string;
  name: string;
  role: string;
}

export interface LoginResponse {
  access_token: string;
  refresh_token: string;
  user: {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    organizations: Organization[];
  };
}