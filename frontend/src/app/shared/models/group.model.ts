export type CampaignType = 'Classique' | 'Transactionnelle' | 'Classique / Transactionnelle';

export interface Group {
  id: number;
  label: string;
  userCount: number;
  type: CampaignType;
  budget: number;
  active: boolean;
}

export interface GroupFormData {
  label: string;
  budget: number;
  adminId?: number;
  alphaHeader: string[];
  campaignTypes: CampaignType[];
  description: string;
}