import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MessageTemplateService } from '../../../core/services/message-template.service';
import { MessageTemplate } from '../../../shared/models/message-template.model';

@Component({
  selector: 'app-message-template-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './list.component.html',
  styleUrl: './list.component.scss'
})
export class MessageTemplateListComponent implements OnInit {
  private templateService = inject(MessageTemplateService);
  private router = inject(Router);

  templates = signal<MessageTemplate[]>([]);
  searchQuery = '';
  filterType = '';
  showTypeDropdown = false;

  // Modal state
  showModal = false;
  editingTemplate: MessageTemplate | null = null;
  isSaving = false;

  modalForm: Partial<MessageTemplate> = {
    libelle: '',
    type: 'CLASSIC',
    message: ''
  };
  modalCharCount = 0;
  modalPageCount = 1;

  /** Computed filtered list */
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
    document.addEventListener('click', (e) => this.onDocumentClick(e));
  }

  ngOnDestroy() {
    document.removeEventListener('click', (e) => this.onDocumentClick(e));
  }

  private onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    if (!target.closest('.filter-dropdown-wrapper')) {
      this.showTypeDropdown = false;
    }
  }

  loadTemplates() {
    this.templateService.getTemplates().subscribe({
      next: (response) => this.templates.set(response.data),
      error: (error) => console.error('Error loading templates:', error)
    });
  }

  // ── Filters ──

  applySearch() {
    // filteredTemplates() is computed, no action needed — triggers on property change
    // Force signal refresh if needed:
    this.templates.update(t => [...t]);
  }

  toggleTypeDropdown() {
    this.showTypeDropdown = !this.showTypeDropdown;
  }

  setTypeFilter(type: string) {
    this.filterType = type;
    this.showTypeDropdown = false;
    this.applySearch();
  }

  resetFilters() {
    this.searchQuery = '';
    this.filterType = '';
    this.applySearch();
  }

  // ── Text analysis helpers ──

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
    const matches = message.match(/@\w+/g) || [];
    return [...new Set(matches)];
  }

  // ── Actions ──

  useInCampaign(template: MessageTemplate) {
    this.router.navigate(['/campaigns/create'], {
      queryParams: { templateId: template.id }
    });
  }

  editTemplate(template: MessageTemplate) {
    this.editingTemplate = template;
    this.modalForm = {
      libelle: template.libelle,
      type: template.type,
      message: template.message
    };
    this.updateModalCharCount();
    this.showModal = true;
  }

  deleteTemplate(id: string) {
    if (!confirm('Supprimer ce modèle ? Cette action est irréversible.')) return;
    this.templateService.deleteTemplate(id).subscribe({
      next: () => this.loadTemplates(),
      error: (error) => console.error('Error deleting template:', error)
    });
  }

  // ── Modal ──

  openCreateModal() {
    this.editingTemplate = null;
    this.modalForm = { libelle: '', type: 'CLASSIC', message: '' };
    this.modalCharCount = 0;
    this.modalPageCount = 1;
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.editingTemplate = null;
  }

  updateModalCharCount() {
    const msg = this.modalForm.message || '';
    const hasArabic = /[\u0600-\u06FF]/.test(msg);
    this.modalCharCount = msg.length;
    this.modalPageCount = Math.ceil(msg.length / (hasArabic ? 70 : 160)) || 1;
  }

  insertModalVariable(variable: string) {
    const textarea = document.querySelector('.modal textarea') as HTMLTextAreaElement;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = this.modalForm.message || '';
    this.modalForm.message = text.substring(0, start) + `@${variable}` + text.substring(end);
    this.updateModalCharCount();
    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(start + variable.length + 1, start + variable.length + 1);
    }, 0);
  }

  isModalValid(): boolean {
    return !!(this.modalForm.libelle?.trim() && this.modalForm.message?.trim() && this.modalForm.type);
  }

  saveTemplate() {
    if (!this.isModalValid()) return;
    this.isSaving = true;

    const obs = this.editingTemplate
      ? this.templateService.updateTemplate(this.editingTemplate.id!, this.modalForm as MessageTemplate)
      : this.templateService.createTemplate(this.modalForm as MessageTemplate);

    obs.subscribe({
      next: () => {
        this.isSaving = false;
        this.closeModal();
        this.loadTemplates();
      },
      error: (error) => {
        console.error('Error saving template:', error);
        this.isSaving = false;
      }
    });
  }
}