export interface Contact {
  id?: string;
  phone: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  tags?: string[];
  createdAt?: Date;
  updatedAt?: Date;
}

export interface CreateContactRequest {
  phone: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  tags?: string[];
}

export interface ImportResult {
  totalProcessed: number;
  successCount: number;
  failureCount: number;
  errors?: string[];
}

export interface ContactSearchParams {
  query?: string;
  tags?: string[];
  page?: number;
  size?: number;
}