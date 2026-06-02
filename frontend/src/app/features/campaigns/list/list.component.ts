import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CampaignService } from '../../../core/services/campaign.service';
import { Campaign, CampaignStatus } from '../../../shared/models/campaign.model';

@Component({
  selector: 'app-campaign-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './list.component.html',
  styleUrl: './list.component.scss'
})
export class CampaignListComponent implements OnInit {
  private campaignService = inject(CampaignService);
  private router = inject(Router);

  campaigns = signal<Campaign[]>([]);
  currentPage = signal(0);
  totalPages = signal(0);

  // UI state
  viewMode: 'grid' | 'list' = 'grid';
  showStatusDropdown = false;
  showTypeDropdown = false;

  filters = {
    libelle: '',
    status: '',
    type: ''
  };

  statusList = Object.values(CampaignStatus);
  userRole = localStorage.getItem('userRole') || '';

  ngOnInit() {
    this.loadCampaigns();
    // Close dropdowns on outside click
    document.addEventListener('click', (e) => this.onDocumentClick(e));
  }

  ngOnDestroy() {
    document.removeEventListener('click', (e) => this.onDocumentClick(e));
  }

  private onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    if (!target.closest('.filter-dropdown-wrapper')) {
      this.showStatusDropdown = false;
      this.showTypeDropdown = false;
    }
  }

  loadCampaigns() {
    this.campaignService.getMyCampaigns(this.currentPage(), 20)
      .subscribe({
        next: (response) => {
          this.campaigns.set(response.data.content);
          this.totalPages.set(response.data.totalPages);
        },
        error: (error) => console.error('Error loading campaigns:', error)
      });
  }

  applyFilters() {
    this.currentPage.set(0);
    this.loadCampaigns();
  }

  resetFilters() {
    this.filters = { libelle: '', status: '', type: '' };
    this.currentPage.set(0);
    this.loadCampaigns();
  }

  goToPage(page: number) {
    this.currentPage.set(page);
    this.loadCampaigns();
  }

  // ── Dropdown toggles ──
  toggleStatusDropdown() {
    this.showStatusDropdown = !this.showStatusDropdown;
    this.showTypeDropdown = false;
  }

  toggleTypeDropdown() {
    this.showTypeDropdown = !this.showTypeDropdown;
    this.showStatusDropdown = false;
  }

  setStatusFilter(status: string) {
    this.filters.status = status;
    this.showStatusDropdown = false;
    this.applyFilters();
  }

  setTypeFilter(type: string) {
    this.filters.type = type;
    this.showTypeDropdown = false;
    this.applyFilters();
  }

  // ── Stats helpers ──
  countByStatus(status: string): number {
    return this.campaigns().filter(c => c.status === status).length;
  }

  // ── Label helpers ──
  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'DRAFT':               'Brouillon',
      'PENDING_VALIDATION':  'En attente',
      'VALIDATED':           'Validée',
      'SCHEDULED':           'Planifiée',
      'RUNNING':             'En cours',
      'COMPLETED':           'Terminée',
      'STOPPED':             'Arrêtée',
      'CANCELLED':           'Annulée',
      'REJECTED':            'Refusée',
      'ERROR':               'Erreur'
    };
    return labels[status] || status;
  }

  getSendingWindowLabel(window: string): string {
    const labels: Record<string, string> = {
      'ALL_DAY':        'Tous les jours 24h/24',
      'BUSINESS_HOURS': 'Jours ouvrables 08h–18h',
      'EVENING':        'Plage du soir 18h–22h'
    };
    return labels[window] || window;
  }

  // ── CSS class helpers ──
  getStatusClass(status: string): string {
    return 'badge-' + status.toLowerCase().replace('_', '_');
  }

  getStripeClass(status: string): string {
    return 'stripe-' + status.toLowerCase().replace('_', '_');
  }

  getProgressClass(status: string): string {
    const map: Record<string, string> = {
      'RUNNING':   'fill-running',
      'COMPLETED': 'fill-completed',
      'STOPPED':   'fill-stopped'
    };
    return map[status] || 'fill-running';
  }

  getProgressLabel(status: string): string {
    const map: Record<string, string> = {
      'RUNNING':   'Progression',
      'COMPLETED': 'Terminé',
      'STOPPED':   'Interrompue à'
    };
    return map[status] || 'Progression';
  }

  /**
   * Calculate progress % based on dates for RUNNING / COMPLETED / STOPPED.
   * Falls back to 100 for COMPLETED, 0 for others if dates are missing.
   */
  getProgressPercent(campaign: Campaign): number {
    if (campaign.status === 'COMPLETED') return 100;
    if (!campaign.startDate || !campaign.endDate) return 0;

    const start = new Date(campaign.startDate).getTime();
    const end   = new Date(campaign.endDate).getTime();
    const now   = Date.now();

    if (now >= end) return 100;
    if (now <= start) return 0;

    return Math.round(((now - start) / (end - start)) * 100);
  }

  // ── Pagination helper ──
  getPagesArray(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i);
  }

  // ── Permission helpers ──
  canEdit(campaign: Campaign): boolean {
    return campaign.status === CampaignStatus.DRAFT ||
           campaign.status === CampaignStatus.PENDING_VALIDATION;
  }

  canValidate(campaign: Campaign): boolean {
    return campaign.status === CampaignStatus.PENDING_VALIDATION &&
           ['ADMIN', 'SUPERADMIN', 'SUPERVISOR'].includes(this.userRole);
  }

  canStop(campaign: Campaign): boolean {
    return campaign.status === CampaignStatus.RUNNING;
  }

  // ── Actions ──
  viewDetails(id: string) {
    this.router.navigate(['/campaigns', id])
  }

  editCampaign(id: string) {
    this.router.navigate(['/campaigns', id, 'edit'])
  }

  openValidationModal(campaign: Campaign) {
    // Open validation modal
  }

  stopCampaign(id: string) {
    if (confirm('Voulez-vous vraiment arrêter cette campagne ?')) {
      this.campaignService.stopCampaign(id).subscribe({
        next: () => this.loadCampaigns(),
        error: (error) => console.error('Error stopping campaign:', error)
      });
    }
  }
}