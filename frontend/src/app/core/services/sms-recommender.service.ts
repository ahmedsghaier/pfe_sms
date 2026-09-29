import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, debounceTime, distinctUntilChanged } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RecommendRequest {
  partialText: string;
  topK?: number;
}

export interface Suggestion {
  word: string;
  score: number;
}

export interface RecommendResponse {
  mode: 'autocomplete' | 'next_word';
  lang: string;
  suggestions: Suggestion[];
}

export interface FeedbackRequest {
  partialText: string;
  selectedWord: string;
  userId?: string;
}

@Injectable({ providedIn: 'root' })
export class SmsRecommenderService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/v1/sms-recommender`;

  recommend(partialText: string, topK = 5): Observable<RecommendResponse> {
    return this.http.post<RecommendResponse>(`${this.base}/recommend`, {
      partialText,
      topK
    });
  }

  recordFeedback(partialText: string, selectedWord: string): void {
    this.http.post(`${this.base}/feedback`, {
      partialText,
      selectedWord
    }).subscribe();
  }
}