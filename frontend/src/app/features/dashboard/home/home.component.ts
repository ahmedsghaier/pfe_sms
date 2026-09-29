import { Component } from '@angular/core';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-home',
  standalone: true,
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent {
  private readonly chartBase = `${environment.apiUrl}/v1/model-results/chart`;

  hourlyCurveUrl = `${this.chartBase}/hourly_curve_by_type`;
  engagementTierUrl = `${this.chartBase}/engagement_by_tier`;
}