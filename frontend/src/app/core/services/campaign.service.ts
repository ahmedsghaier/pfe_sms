import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Campaign, CampaignStats, SmsLog } from '../../shared/models/campaign.model';
import { ApiResponse } from '../../shared/models/api-response.model';

// ── Fenêtre optimale retournée par le backend ────────────────────────────────
export interface WindowScore {
  windowKey:   string;   // "BUSINESS_HOURS" | "EVENING" | "ALL_DAY"
  windowLabel: string;   // "Jours ouvrables (08h–18h)"
  rate:        number;   // taux en %
  peakHour:    number;   // heure de pic
  icon:        string;   // emoji
}

// ── Payload envoyé au backend ────────────────────────────────────────────────
export interface EngagementPredictionPayload {
  messageTemplate: string;
  startDate?:      string;
  endDate?:        string;
  sendingWindow?:  'ALL_DAY' | 'BUSINESS_HOURS' | 'EVENING';
  campaignType?:   string;
  operateur?:      string;
}

// ── Réponse du backend — tous les champs ML sont nullable ───────────────────
// null = ML ne l'a pas retourné, ne pas afficher de valeur inventée
export interface EngagementPredictionResponse {
  nlpType:                 string | null;
  predictedEngagementRate: number | null;   // Double nullable côté Java
  bestHour:                number | null;   // Integer nullable côté Java
  bestWindow:              string | null;   // null si ML ne retourne pas optimalWindow
  chosenHourScore:         number | null;
  recommendation:          string | null;
  topWindows:              WindowScore[];   // liste vide si ML indisponible
}

@Injectable({ providedIn: 'root' })
export class CampaignService {
  private http   = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/campaigns`;

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Authorization':     `Bearer ${localStorage.getItem('access_token') || ''}`,
      'X-Organization-Id': localStorage.getItem('organizationId') || '',
      'X-User-Id':         localStorage.getItem('user_id')        || '',
      'X-User-Role':       localStorage.getItem('userRole')        || ''
    });
  }

  createCampaign(campaign: Campaign): Observable<ApiResponse<Campaign>> {
    return this.http.post<ApiResponse<Campaign>>(this.apiUrl, campaign, { headers: this.getHeaders() });
  }

  updateCampaign(id: string, campaign: Campaign): Observable<ApiResponse<Campaign>> {
    return this.http.put<ApiResponse<Campaign>>(`${this.apiUrl}/${id}`, campaign, { headers: this.getHeaders() });
  }

  submitForValidation(id: string): Observable<ApiResponse<Campaign>> {
    return this.http.post<ApiResponse<Campaign>>(`${this.apiUrl}/${id}/submit`, {}, { headers: this.getHeaders() });
  }

  validateCampaign(id: string, approved: boolean, comments?: string): Observable<ApiResponse<Campaign>> {
    return this.http.post<ApiResponse<Campaign>>(`${this.apiUrl}/${id}/validate`, { approved, comments }, { headers: this.getHeaders() });
  }

  sendTestSms(id: string, phoneNumber: string): Observable<ApiResponse<void>> {
    const params = new HttpParams().set('phoneNumber', phoneNumber);
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/${id}/test`, {}, { headers: this.getHeaders(), params });
  }

  stopCampaign(id: string): Observable<ApiResponse<Campaign>> {
    return this.http.post<ApiResponse<Campaign>>(`${this.apiUrl}/${id}/stop`, {}, { headers: this.getHeaders() });
  }

  getCampaignsByOrganization(orgId: string, page = 0, size = 20): Observable<ApiResponse<any>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/organization/${orgId}`, { headers: this.getHeaders(), params });
  }

  getCampaignsByGroup(groupId: string, page = 0, size = 20): Observable<ApiResponse<any>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/group/${groupId}`, { headers: this.getHeaders(), params });
  }

  getMyCampaigns(page = 0, size = 20): Observable<ApiResponse<any>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/my-campaigns`, { headers: this.getHeaders(), params });
  }

  getCampaign(id: string): Observable<ApiResponse<Campaign>> {
    return this.http.get<ApiResponse<Campaign>>(`${this.apiUrl}/${id}`, { headers: this.getHeaders() });
  }

  getCampaignStats(id: string): Observable<ApiResponse<CampaignStats>> {
    return this.http.get<ApiResponse<CampaignStats>>(`${this.apiUrl}/${id}/stats`, { headers: this.getHeaders() });
  }

  getCampaignLogs(id: string, page = 0, size = 50): Observable<ApiResponse<any>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<any>>(`${this.apiUrl}/${id}/logs`, { headers: this.getHeaders(), params });
  }

predictEngagement(payload: EngagementPredictionPayload): Observable<ApiResponse<EngagementPredictionResponse>> {
  return this.http.post<ApiResponse<EngagementPredictionResponse>>(
    `${this.apiUrl}/predict-engagement`,
    {
      messageTemplate: payload.messageTemplate,
      startDate:       payload.startDate   ?? null,
      endDate:         payload.endDate     ?? null,
      sendingWindow:   payload.sendingWindow ?? 'ALL_DAY',
      campaignType:    payload.campaignType ?? 'CLASSIC',
      operateur:       payload.operateur    ?? 'ORANGE'
    },
    { headers: this.getHeaders() }
  );
}
}