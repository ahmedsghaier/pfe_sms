import { TestBed } from '@angular/core/testing';

import { SmsRecommenderService } from './sms-recommender.service';

describe('SmsRecommenderService', () => {
  let service: SmsRecommenderService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SmsRecommenderService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
