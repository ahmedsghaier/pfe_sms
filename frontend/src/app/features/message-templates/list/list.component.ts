import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { MessageTemplateService } from '../../../core/services/message-template.service';
import { SmsRecommenderService, Suggestion } from '../../../core/services/sms-recommender.service';
import { MessageTemplate } from '../../../shared/models/message-template.model';
import { BidiModule } from "@angular/cdk/bidi";

@Component({
  selector: 'app-message-template-list',
  standalone: true,
  imports: [CommonModule, FormsModule, BidiModule],
  templateUrl: './list.component.html',
  styleUrl: './list.component.scss'
})
export class MessageTemplateListComponent implements OnInit, OnDestroy {
  private templateService = inject(MessageTemplateService);
  private recommender     = inject(SmsRecommenderService);
  private router          = inject(Router);

  // ── Signals ──
  templates        = signal<MessageTemplate[]>([]);
  searchQuery      = '';
  filterType       = '';
  showTypeDropdown = false;

  showModal        = false;
  editingTemplate: MessageTemplate | null = null;
  isSaving         = false;

  modalForm: Partial<MessageTemplate> = {
    libelle: '',
    type: 'CLASSIC',
    message: ''
  };
  modalCharCount = 0;
  modalPageCount = 1;

  // ── Autocomplétion ──
  suggestions: Suggestion[] = [];
  showSuggestions           = false;
  detectedLang              = 'fr';
  private textInput$        = new Subject<string>();
  private lastPartialText   = '';
  private docClickHandler   = (e: Event) => this.onDocumentClick(e);

  filteredTemplates = computed(() => {
    let list = this.templates();
    if (this.searchQuery) {
      const q = this.searchQuery.toLowerCase();
      list = list.filter(t =>
        t.libelle.toLowerCase().includes(q) ||
        t.message.toLowerCase().includes(q)
      );
    }
    if (this.filterType) {
      list = list.filter(t => t.type === this.filterType);
    }
    return list;
  });

  ngOnInit() {
    this.loadTemplates();
    document.addEventListener('click', this.docClickHandler);

    this.textInput$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(text => {
        this.lastPartialText = text;
        return this.recommender.recommend(text, 5);
      })
    ).subscribe({
      next: (res) => {
        this.suggestions     = res.suggestions;
        this.showSuggestions = res.suggestions.length > 0;
      },
      error: () => {
        this.suggestions     = [];
        this.showSuggestions = false;
      }
    });
  }

  ngOnDestroy() {
    document.removeEventListener('click', this.docClickHandler);
    this.textInput$.complete();
  }

  private onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    if (!target.closest('.filter-dropdown-wrapper')) {
      this.showTypeDropdown = false;
    }
    if (!target.closest('.sms-editor-wrapper')) {
      this.showSuggestions = false;
    }
  }

  // ── Templates ──

  loadTemplates() {
    this.templateService.getTemplates().subscribe({
      next:  (response) => this.templates.set(response.data),
      error: (error)    => console.error('Error loading templates:', error)
    });
  }

  // ── Filters ──

  applySearch() {
    this.templates.update(t => [...t]);
  }

  toggleTypeDropdown() {
    this.showTypeDropdown = !this.showTypeDropdown;
  }

  setTypeFilter(type: string) {
    this.filterType       = type;
    this.showTypeDropdown = false;
    this.applySearch();
  }

  resetFilters() {
    this.searchQuery = '';
    this.filterType  = '';
    this.applySearch();
  }

  // ── Autocomplétion ──

  onMessageInput(event: Event) {
    const text = (event.target as HTMLTextAreaElement).value;
    this.modalForm.message = text;
    this.updateModalCharCount();

    // Détection de langue pour le badge
    this.detectedLang = /[\u0600-\u06FF]/.test(text) ? 'ar' : 'fr';

    if (text.trim().length >= 2) {
      this.textInput$.next(text);
    } else {
      this.suggestions     = [];
      this.showSuggestions = false;
    }
  }

  selectSuggestion(suggestion: Suggestion) {
  const textarea = document.querySelector('.modal textarea') as HTMLTextAreaElement;
  const current   = this.modalForm.message || '';
  const cursorPos = textarea ? textarea.selectionStart : current.length;

  // Texte avant / après le curseur
  const beforeCursor = current.slice(0, cursorPos);
  const afterCursor  = current.slice(cursorPos);

  // On retrouve le début du mot en cours de frappe (avant le curseur)
  const lastSpaceIndex = beforeCursor.lastIndexOf(' ');
  const wordStart = lastSpaceIndex + 1; // 0 si pas d'espace trouvé

  // Nouveau message : [texte avant le mot partiel] + mot suggéré (SANS espace) + [reste]
  const newMessage =
    current.slice(0, wordStart) +
    suggestion.word +
    afterCursor;

  this.modalForm.message = newMessage;
  this.showSuggestions   = false;
  this.suggestions       = [];
  this.updateModalCharCount();

  this.recommender.recordFeedback(this.lastPartialText, suggestion.word);

  // Curseur positionné juste après le mot inséré (sans espace)
  const newCursorPos = wordStart + suggestion.word.length;

  setTimeout(() => {
    if (textarea) {
      textarea.value = newMessage;
      textarea.focus();
      textarea.setSelectionRange(newCursorPos, newCursorPos);
    }
  }, 0);
}
  hideSuggestions() {
    setTimeout(() => this.showSuggestions = false, 150);
  }

  // ── Text helpers ──

  getCharCount(message: string): number {
    return message?.length || 0;
  }

  getPageCount(message: string): number {
    if (!message) return 0;
    const hasArabic = /[\u0600-\u06FF]/.test(message);
    return Math.ceil(message.length / (hasArabic ? 70 : 160)) || 1;
  }

  hasArabic(message: string): boolean {
    return /[\u0600-\u06FF]/.test(message);
  }

  getVariables(message: string): string[] {
    if (!message) return [];
    return [...new Set(message.match(/@\w+/g) || [])];
  }

  // ── Actions ──

  useInCampaign(template: MessageTemplate) {
    this.router.navigate(['/campaigns/create'], {
      queryParams: { templateId: template.id }
    });
  }

  editTemplate(template: MessageTemplate) {
    this.editingTemplate = template;
    this.modalForm       = {
      libelle: template.libelle,
      type:    template.type,
      message: template.message
    };
    this.detectedLang    = /[\u0600-\u06FF]/.test(template.message) ? 'ar' : 'fr';
    this.updateModalCharCount();
    this.showModal = true;
  }

  deleteTemplate(id: string) {
    if (!confirm('Supprimer ce modèle ? Cette action est irréversible.')) return;
    this.templateService.deleteTemplate(id).subscribe({
      next:  () => this.loadTemplates(),
      error: (e) => console.error('Error deleting template:', e)
    });
  }

  // ── Modal ──

  openCreateModal() {
    this.editingTemplate = null;
    this.modalForm       = { libelle: '', type: 'CLASSIC', message: '' };
    this.modalCharCount  = 0;
    this.modalPageCount  = 1;
    this.suggestions     = [];
    this.showSuggestions = false;
    this.detectedLang    = 'fr';
    this.showModal       = true;
  }

  closeModal() {
    this.showModal       = false;
    this.editingTemplate = null;
    this.suggestions     = [];
    this.showSuggestions = false;
    this.detectedLang    = 'fr';
  }

  updateModalCharCount() {
    const msg       = this.modalForm.message || '';
    const hasArabic = /[\u0600-\u06FF]/.test(msg);
    this.modalCharCount = msg.length;
    this.modalPageCount = Math.ceil(msg.length / (hasArabic ? 70 : 160)) || 1;
  }

  insertModalVariable(variable: string) {
    const textarea = document.querySelector('.modal textarea') as HTMLTextAreaElement;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end   = textarea.selectionEnd;
    const text  = this.modalForm.message || '';
    this.modalForm.message = text.substring(0, start) + `@${variable}` + text.substring(end);
    this.updateModalCharCount();
    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(start + variable.length + 1, start + variable.length + 1);
    }, 0);
  }

  isModalValid(): boolean {
    return !!(
      this.modalForm.libelle?.trim() &&
      this.modalForm.message?.trim() &&
      this.modalForm.type
    );
  }

  saveTemplate() {
    if (!this.isModalValid()) return;
    this.isSaving = true;

    const obs = this.editingTemplate
      ? this.templateService.updateTemplate(this.editingTemplate.id!, this.modalForm as MessageTemplate)
      : this.templateService.createTemplate(this.modalForm as MessageTemplate);

    obs.subscribe({
      next:  () => { this.isSaving = false; this.closeModal(); this.loadTemplates(); },
      error: (e) => { console.error('Error saving template:', e); this.isSaving = false; }
    });
  }
}