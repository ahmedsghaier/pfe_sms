// src/app/shared/models/campaign.model.ts
export interface Campaign {
  id?: string;
  libelle: string;
  description?: string;
  groupId: string;
  ownerId?: string;
  type: 'CLASSIC' | 'TRANSACTIONAL';
  status: CampaignStatus;
  startDate: Date;
  endDate: Date;
  headerAlpha: string;
  validityHours: number;
  sendingWindow?: SendingWindow;
  messageTemplate?: string;
  recipientCount?: number;
  estimatedCost?: number;
  createdAt?: Date;
  updatedAt?: Date;
}

export enum CampaignStatus {
  DRAFT = 'DRAFT',
  PENDING_VALIDATION = 'PENDING_VALIDATION',
  VALIDATED = 'VALIDATED',
  SCHEDULED = 'SCHEDULED',
  RUNNING = 'RUNNING',
  COMPLETED = 'COMPLETED',
  STOPPED = 'STOPPED',
  CANCELLED = 'CANCELLED',
  REJECTED = 'REJECTED',
  ERROR = 'ERROR'
}

export enum SendingWindow {
  ALL_DAY = 'ALL_DAY',
  BUSINESS_HOURS = 'BUSINESS_HOURS',
  EVENING = 'EVENING'
}

export interface CampaignStats {
  totalSent: number;
  delivered: number;
  failed: number;
  pending: number;
  deliveryRate: number;
}

export interface SmsLog {
  id: string;
  campaignId: string;
  recipient: string;
  message: string;
  status: string;
  sentAt: Date;
  deliveredAt?: Date;
}