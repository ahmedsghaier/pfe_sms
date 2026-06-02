import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CampaignService, WindowScore } from '../../../core/services/campaign.service';
import { MessageTemplateService } from '../../../core/services/message-template.service';
import { Campaign, SendingWindow } from '../../../shared/models/campaign.model';
import { MessageTemplate } from '../../../shared/models/message-template.model';
import { FormsModule } from '@angular/forms';
import { GroupsService } from '../../../core/services/groups.service';
import { ContactService } from '../../../core/services/contact.service';

@Component({
  selector: 'app-campaign-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './campaign-form.component.html',
  styleUrl: './campaign-form.component.scss'
})
export class CampaignFormComponent implements OnInit {
  private fb              = inject(FormBuilder);
  private campaignService = inject(CampaignService);
  private templateService = inject(MessageTemplateService);
  private groupsService   = inject(GroupsService);
  private router          = inject(Router);
  private route           = inject(ActivatedRoute);
  private contactService  = inject(ContactService);

  campaignForm!: FormGroup;
  currentStep = signal(1);
  isEditMode  = false;
  campaignId?: string;

  steps = [
    { label: 'Type & Message' },
    { label: 'Contacts' },
    { label: 'Paramètres' },
    { label: 'Validation' }
  ];

  templates        = signal<MessageTemplate[]>([]);
  groups           = signal<any[]>([]);
  availableTags    = signal<string[]>([]);
  availableHeaders = signal<string[]>([]);
  selectedContacts = signal<any[]>([]);

  charCount         = signal(0);
  pageCount         = signal(1);
  audienceCount     = signal(0);
  estimatedMessages = signal(0);
  estimatedCost     = signal(0);
  availableBudget   = signal(5000);

  saveAsTemplate  = false;
  templateName    = '';
  testPhoneNumber = '';
  testSent        = signal(false);
  confirmTest     = false;
  isSubmitting    = signal(false);

  // ── Signals ML prediction — tous nullable, aucune valeur par défaut inventée
  predictedRate         = signal<number | null>(null);
  bestTime              = signal<string | null>(null);
  bestWindow            = signal<string | null>(null);
  bestWindowKey         = signal<string | null>(null);   // ← AJOUTÉ : clé brute ML (ex: "morning_priority")
  nlpTypeDetected       = signal<string | null>(null);
  recommendation        = signal<string | null>(null);
  chosenHourScore       = signal<number | null>(null);
  isPredicting          = signal(false);
  predictionError       = signal<string>('');

  // ── Fenêtres recommandées — liste vide si ML indisponible
  topWindows            = signal<WindowScore[]>([]);

  // ── Fenêtre optimale sélectionnée par l'utilisateur dans les cards
  selectedOptimalWindow = signal<string>('');

  // ══════════════════════════════════════════════════════════
  // LIFECYCLE
  // ══════════════════════════════════════════════════════════

  ngOnInit() {
    this.initForm();
    this.loadTemplates();
    this.loadGroups();
    this.checkEditMode();
    this.loadAvailableHeaders();

    this.campaignForm.get('groupId')?.valueChanges.subscribe(() => this.updateAudience());
    this.campaignForm.get('tags')?.valueChanges.subscribe(() => this.updateAudience());

    // Relancer la prédiction si sendingWindow, startDate ou endDate changent
    ['sendingWindow', 'startDate', 'endDate'].forEach(field => {
      this.campaignForm.get(field)?.valueChanges.subscribe(() => {
        if (this.predictedRate() !== null) {
          this.predictEngagementInValidation();
        }
      });
    });
  }

  // ══════════════════════════════════════════════════════════
  // FORM INIT
  // ══════════════════════════════════════════════════════════

  initForm() {
    this.campaignForm = this.fb.group({
      type:            ['CLASSIC', Validators.required],
      groupId:         ['', Validators.required],
      messageTemplate: ['', Validators.required],
      tags:            [[]],
      contacts:        [[]],
      headerAlpha:     ['', Validators.required],
      libelle:         ['', Validators.required],
      description:     [''],
      startDate:       ['', Validators.required],
      endDate:         ['', Validators.required],
      validityHours:   [24, [Validators.required, Validators.min(1), Validators.max(72)]],
      sendingWindow:   ['ALL_DAY', Validators.required],
      useOnlyMyApiKey: [false]
    });
  }

  // ══════════════════════════════════════════════════════════
  // DATA LOADING
  // ══════════════════════════════════════════════════════════

  loadTemplates() {
    const type = this.campaignForm.get('type')?.value;
    this.templateService.getTemplates(type).subscribe({
      next:  (r) => this.templates.set(r.data),
      error: (e) => console.error('Error loading templates:', e)
    });
  }

  loadGroups() { this.groups.set(this.groupsService.groups()); }

  checkEditMode() {
    this.campaignId = this.route.snapshot.params['id'];
    if (this.campaignId) { this.isEditMode = true; this.loadCampaign(this.campaignId); }
  }

  loadCampaign(id: string) {
    this.campaignService.getCampaign(id).subscribe({
      next:  (r) => this.campaignForm.patchValue(r.data),
      error: (e) => console.error('Error loading campaign:', e)
    });
  }

  loadAvailableHeaders() {
    this.availableHeaders.set([
      'MYCOMPANY', 'TUNISIA BANK', 'ORANGE TN', 'PROMO2026', 'SUPPORT', 'NOTIFICATION'
    ]);
  }

  // ══════════════════════════════════════════════════════════
  // STEPPER NAVIGATION
  // ══════════════════════════════════════════════════════════

  nextStep() {
    if (this.currentStep() < 4) { this.currentStep.update(v => v + 1); window.scrollTo({ top: 0, behavior: 'smooth' }); }
  }
  previousStep() {
    if (this.currentStep() > 1) { this.currentStep.update(v => v - 1); window.scrollTo({ top: 0, behavior: 'smooth' }); }
  }
  goToStepIfAllowed(step: number) {
    if (step < this.currentStep()) { this.currentStep.set(step); window.scrollTo({ top: 0, behavior: 'smooth' }); }
  }

  // ══════════════════════════════════════════════════════════
  // TEMPLATE & MESSAGE
  // ══════════════════════════════════════════════════════════

  onTemplateSelect(event: Event) {
    const id = (event.target as HTMLSelectElement).value;
    if (id) {
      const t = this.templates().find(t => t.id === id);
      if (t) { this.campaignForm.patchValue({ messageTemplate: t.message }); this.updateCharCount(); }
    }
  }

  insertVariable(variable: string) {
    const ta = document.querySelector('textarea[formControlName="messageTemplate"]') as HTMLTextAreaElement;
    if (!ta) return;
    const s = ta.selectionStart, e = ta.selectionEnd;
    const text = this.campaignForm.get('messageTemplate')?.value || '';
    this.campaignForm.patchValue({ messageTemplate: text.substring(0, s) + `@${variable}` + text.substring(e) });
    this.updateCharCount();
    setTimeout(() => { ta.focus(); ta.setSelectionRange(s + variable.length + 1, s + variable.length + 1); }, 0);
  }

  updateCharCount() {
    const msg = this.campaignForm.get('messageTemplate')?.value || '';
    const arabic = /[\u0600-\u06FF]/.test(msg);
    this.charCount.set(msg.length);
    this.pageCount.set(Math.ceil(msg.length / (arabic ? 70 : 160)) || 1);
  }

  // ══════════════════════════════════════════════════════════
  // CONTACTS
  // ══════════════════════════════════════════════════════════

  searchContacts(event: Event) {
    const query = (event.target as HTMLInputElement).value.trim();
    if (query.length < 1) return;
    this.contactService.searchContacts(query, { page: 0, size: 10 }).subscribe({
      next: (r) => {
        this.selectedContacts.set(r.success && r.data?.content?.length ? r.data.content : []);
        this.updateAudience();
      },
      error: () => this.selectedContacts.set([])
    });
  }

  addContact(contact: any) {
    if (!this.selectedContacts().some(c => c.id === contact.id)) {
      this.selectedContacts.update(cs => [...cs, contact]);
      this.updateAudience();
    }
  }

  removeContact(contact: any) {
    this.selectedContacts.update(cs => cs.filter(c => c.id !== contact.id));
    this.updateAudience();
  }

  updateAudience() {
    const count = this.selectedContacts().length;
    this.audienceCount.set(count);
    this.estimatedMessages.set(count);
    this.estimatedCost.set(Math.round(count * 0.045));
  }

  // ══════════════════════════════════════════════════════════
  // BUDGET
  // ══════════════════════════════════════════════════════════

  hasSufficientBudget(): boolean { return this.estimatedCost() <= this.availableBudget(); }

  getBudgetPercent(): number {
    if (this.availableBudget() === 0) return 100;
    return Math.min(100, Math.round((this.estimatedCost() / this.availableBudget()) * 100));
  }

  // ══════════════════════════════════════════════════════════
  // LABELS & HELPERS
  // ══════════════════════════════════════════════════════════

  getSendingWindowLabel(value: string): string {
    const labels: Record<string, string> = {
      'ALL_DAY':        'Tous les jours (24h/24)',
      'BUSINESS_HOURS': 'Jours ouvrables (08h–18h)',
      'EVENING':        'Plage du soir (18h–22h)'
    };
    return labels[value] || value;
  }

  getNlpIcon(type: string): string {
    const icons: Record<string, string> = {
      'OTP':         '🔐',
      'Transaction': '💳',
      'Promotion':   '🎯',
      'Reminder':    '⏰',
      'Information': 'ℹ️'
    };
    return icons[type] || '💬';
  }

  getNlpDescription(type: string): string {
    const descs: Record<string, string> = {
      'OTP':         'Code d\'authentification — taux d\'ouverture très élevé',
      'Transaction': 'Message transactionnel — forte attente de lecture',
      'Promotion':   'Offre commerciale — sensible à l\'heure d\'envoi',
      'Reminder':    'Rappel — efficace en milieu de journée',
      'Information': 'Informatif — engagement modéré, pic le matin'
    };
    return descs[type] || 'Message générique';
  }

  // ══════════════════════════════════════════════════════════
  // STEP VALIDATION
  // ══════════════════════════════════════════════════════════

  isStep1Valid(): boolean {
    return !!(this.campaignForm.get('type')?.valid &&
              this.campaignForm.get('groupId')?.valid &&
              this.campaignForm.get('messageTemplate')?.valid);
  }

  isStep2Valid(): boolean { return this.audienceCount() > 0 && this.hasSufficientBudget(); }

  isStep3Valid(): boolean {
    return !!(this.campaignForm.get('headerAlpha')?.valid &&
              this.campaignForm.get('libelle')?.valid &&
              this.campaignForm.get('startDate')?.valid &&
              this.campaignForm.get('endDate')?.valid);
  }

  // ══════════════════════════════════════════════════════════
  // TEST SMS
  // ══════════════════════════════════════════════════════════

  sendTest() {
    if (!this.testPhoneNumber) return;
    const obs = this.campaignId
      ? this.campaignService.sendTestSms(this.campaignId, this.testPhoneNumber)
      : this.campaignService.sendTestSms('preview', this.testPhoneNumber);
    obs.subscribe({ next: () => this.testSent.set(true), error: (e) => console.error(e) });
  }

  // ══════════════════════════════════════════════════════════
  // SUBMIT
  // ══════════════════════════════════════════════════════════

  getSubmitButtonText(): string {
    if (localStorage.getItem('userRole') === 'AGENT') return '📤 Soumettre pour validation';
    const sd = new Date(this.campaignForm.get('startDate')?.value);
    return sd > new Date() ? '📅 Planifier la campagne' : '🚀 Lancer maintenant';
  }

  onSubmit() {
    if (this.campaignForm.invalid || !this.confirmTest) return;
    this.isSubmitting.set(true);
    const data = this.campaignForm.value;
    const req  = this.isEditMode && this.campaignId
      ? this.campaignService.updateCampaign(this.campaignId, data)
      : this.campaignService.createCampaign(data);
    req.subscribe({
      next:  () => { this.isSubmitting.set(false); this.router.navigate(['/campaigns']); },
      error: (e) => { console.error(e); this.isSubmitting.set(false); }
    });
  }

  cancel() { this.router.navigate(['/campaigns']); }

  // ══════════════════════════════════════════════════════════
  // SÉLECTION D'UNE FENÊTRE OPTIMALE
  // ══════════════════════════════════════════════════════════

  selectOptimalWindow(w: WindowScore): void {
    this.selectedOptimalWindow.set(w.windowKey);
    this.campaignForm.patchValue({ sendingWindow: w.windowKey });
    // valueChanges → predictEngagementInValidation() automatique via ngOnInit
  }

  // ══════════════════════════════════════════════════════════
  // ML ENGAGEMENT PREDICTION
  // ══════════════════════════════════════════════════════════

  predictEngagementInValidation(): void {
    const message       = this.campaignForm.get('messageTemplate')?.value?.trim();
    const startDate     = this.campaignForm.get('startDate')?.value;
    const endDate       = this.campaignForm.get('endDate')?.value;
    const sendingWindow = this.campaignForm.get('sendingWindow')?.value || 'ALL_DAY';

    if (!message || message.length < 10) {
      this.predictionError.set("Veuillez entrer un message d'au moins 10 caractères.");
      return;
    }
    if (!startDate || !endDate) {
      this.predictionError.set("Veuillez renseigner les dates de début et de fin.");
      return;
    }

    this.isPredicting.set(true);
    this.predictionError.set('');

    // Réinitialiser tous les signals avant l'appel
    this.predictedRate.set(null);
    this.bestTime.set(null);
    this.bestWindow.set(null);
    this.bestWindowKey.set(null);   // ← RÉINITIALISÉ
    this.nlpTypeDetected.set(null);
    this.recommendation.set(null);
    this.chosenHourScore.set(null);
    this.topWindows.set([]);

    this.campaignService.predictEngagement({ messageTemplate: message, startDate, endDate, sendingWindow })
      .subscribe({
        next: (response: any) => {
          const data = response.data || response;

          this.nlpTypeDetected.set( data.nlpType    ?? data.nlp_type    ?? null);
          this.predictedRate.set(   data.predictedEngagementRate ?? data.predicted_engagement_rate ?? null);
          this.chosenHourScore.set( data.chosenHourScore ?? data.chosen_hour_score ?? null);
          this.recommendation.set(  data.recommendation ?? null);

          // bestHour → "10h" si ML retourne un nombre, null sinon
          const bh = data.bestHour ?? data.best_hour ?? null;
          this.bestTime.set(bh !== null ? `${bh}h` : null);

          // bestWindow : libellé lisible (ex: "07h–11h")
          this.bestWindow.set(data.bestWindow ?? data.best_window ?? null);

          // bestWindowKey : clé brute ML (ex: "morning_priority") — ← AJOUTÉ
          this.bestWindowKey.set(data.bestWindowKey ?? data.best_window_key ?? null);

          // topWindows
          const tw = data.topWindows ?? data.top_windows ?? [];
          this.topWindows.set(Array.isArray(tw) && tw.length > 0 ? tw : []);
        },

        error: (err) => {
          console.error('Erreur prédiction ML:', err);
          this.predictionError.set(err.error?.message || 'Service ML indisponible. Veuillez réessayer.');
        },

        complete: () => this.isPredicting.set(false)
      });
  }
}