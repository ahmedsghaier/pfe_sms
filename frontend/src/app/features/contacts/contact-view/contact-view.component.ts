import { Component, HostListener, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Contact } from '../../../shared/models/contact.model';
import { PhoneFormatPipe } from '../../../shared/pipe/PhoneFormatPipe';

@Component({
  selector: 'app-contact-view',
  standalone: true,
  imports: [CommonModule, PhoneFormatPipe],
  templateUrl: './contact-view.component.html',
  styleUrls: ['./contact-view.component.scss']
})
export class ContactViewComponent {
  contact = input.required<Contact>();
  closed = output<void>();
  editRequested = output<Contact>();
  deleteRequested = output<Contact>();

  // Affiche un check temporaire après une copie réussie
  copiedField = signal<'email' | 'phone' | null>(null);

  @HostListener('document:keydown.escape')
  onEscape() {
    this.closed.emit();
  }

  initials(): string {
    const c = this.contact();
    const a = c.firstName?.[0] ?? '';
    const b = c.lastName?.[0] ?? '';
    return (a + b).toUpperCase() || '?';
  }

  // Couleur d'avatar dérivée du nom, pour distinguer les contacts d'un coup d'œil
  avatarColor(): string {
    const palette = ['#eef1ff', '#fdf2f8', '#ecfdf5', '#fffbeb', '#f0f9ff'];
    const textPalette = ['#4f6ef7', '#be185d', '#047857', '#b45309', '#0369a1'];
    const key = this.contact().id ?? this.contact().phone ?? '';
    let hash = 0;
    for (let i = 0; i < key.length; i++) hash = key.charCodeAt(i) + ((hash << 5) - hash);
    const idx = Math.abs(hash) % palette.length;
    return `background:${palette[idx]};color:${textPalette[idx]}`;
  }

  relativeDate(): string {
    const created = this.contact().createdAt;
    if (!created) return '';
    const diffDays = Math.floor((Date.now() - new Date(created).getTime()) / 86_400_000);
    if (diffDays <= 0) return "Ajouté aujourd'hui";
    if (diffDays === 1) return 'Ajouté hier';
    if (diffDays < 30) return `Ajouté il y a ${diffDays} jours`;
    const months = Math.floor(diffDays / 30);
    return `Ajouté il y a ${months} mois`;
  }

  async copy(field: 'email' | 'phone') {
    const value = field === 'email' ? this.contact().email : this.contact().phone;
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
      this.copiedField.set(field);
      setTimeout(() => this.copiedField.set(null), 1500);
    } catch {
      // Presse-papiers indisponible (contexte non sécurisé, permission refusée) : on ignore silencieusement
    }
  }

  onEdit() {
    this.editRequested.emit(this.contact());
  }

  onDelete() {
    if (confirm('Voulez-vous vraiment supprimer ce contact ?')) {
      this.deleteRequested.emit(this.contact());
    }
  }
}