
export interface UserRole {
  group: string;
  groupId: string;
  role: string;
}

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  roles: UserRole[];
  isViewer?: boolean;
  isSuperadmin?: boolean;
  active: boolean;
  status: 'active' | 'inactive' | 'pending';
  campaignStats?: { pending: number; running: number };
}

export interface Group {
  id: string;
  name: string;
}

export interface InviteUserPayload {
  email: string;
  isViewer: boolean;
  roles: { group: string; role: string }[];
}

export interface UpdateUserPayload {
  firstName: string;
  lastName: string;
  roles: { group: string; role: string }[];
}

export interface UserFilters {
  firstName?: string;
  lastName?: string;
  status?: string;
  group?: string;
  role?: string;
  page?: number;
  size?: number;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}