export interface MessageTemplate {
  id?: string;
  libelle: string;
  type: 'CLASSIC' | 'TRANSACTIONAL';
  message: string;
  status: boolean;
  createdAt?: Date;
  updatedAt?: Date;
  variables?: string[];
}