import { Component, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ContactService } from '../../../core/services/contact.service';
import { CreateContactRequest } from '../../../shared/models/contact.model';

@Component({
  selector: 'app-contact-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './contact-form.component.html',
  styleUrls: ['./contact-form.component.scss']
})
export class ContactFormComponent {
  private fb = inject(FormBuilder);
  private contactService = inject(ContactService);

  @Output() contactCreated = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  contactForm: FormGroup;
  loading = signal(false);
  error = signal<string | null>(null);
  availableTags = signal<string[]>([]);
  selectedTags = signal<string[]>([]);
  newTagInput = signal('');
  showCountryDropdown = signal(false); // ← AJOUT DE CETTE LIGNE

  // Liste des pays avec leurs codes
  countries = [
    { name: 'Tunisie', code: '+216', flag: '🇹🇳' },
    { name: 'France', code: '+33', flag: '🇫🇷' },
    { name: 'Maroc', code: '+212', flag: '🇲🇦' },
    { name: 'Algérie', code: '+213', flag: '🇩🇿' },
    { name: 'Belgique', code: '+32', flag: '🇧🇪' },
    { name: 'Suisse', code: '+41', flag: '🇨🇭' },
  ];

  selectedCountry = signal(this.countries[0]);

  constructor() {
    this.contactForm = this.fb.group({
      countryCode: ['+216', Validators.required],
      phone: ['', [Validators.required, Validators.pattern(/^[0-9]{8,15}$/)]],
      firstName: [''],
      lastName: [''],
      email: ['', [Validators.email]]
    });

    // Simuler le chargement des tags existants
    this.loadExistingTags();
  }

  loadExistingTags() {
    // TODO: Récupérer les tags existants depuis le backend
    this.availableTags.set(['Client', 'Prospect', 'VIP', 'Newsletter']);
  }

  onCountryChange(country: any) {
    this.selectedCountry.set(country);
    this.contactForm.patchValue({ countryCode: country.code });
    this.showCountryDropdown.set(false); // Fermer le dropdown après sélection
  }

  addTag(tag: string) {
    const trimmedTag = tag.trim();
    if (trimmedTag && !this.selectedTags().includes(trimmedTag)) {
      this.selectedTags.set([...this.selectedTags(), trimmedTag]);
      this.newTagInput.set('');
    }
  }

  removeTag(tag: string) {
    this.selectedTags.set(this.selectedTags().filter(t => t !== tag));
  }

  onSubmit() {
    if (this.contactForm.invalid) {
      Object.keys(this.contactForm.controls).forEach(key => {
        this.contactForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const formValue = this.contactForm.value;
    const fullPhone = formValue.countryCode + formValue.phone;

    const request: CreateContactRequest = {
      phone: fullPhone,
      firstName: formValue.firstName || undefined,
      lastName: formValue.lastName || undefined,
      email: formValue.email || undefined,
      tags: this.selectedTags().length > 0 ? this.selectedTags() : undefined
    };

    this.contactService.createContact(request).subscribe({
      next: (response) => {
        if (response.success) {
          this.loading.set(false);
          this.contactCreated.emit();
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Erreur lors de la création du contact');
      }
    });
  }

  onCancel() {
    this.cancelled.emit();
  }

  get phoneControl() {
    return this.contactForm.get('phone');
  }

  get emailControl() {
    return this.contactForm.get('email');
  }
}