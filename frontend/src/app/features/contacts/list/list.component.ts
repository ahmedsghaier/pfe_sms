import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { ContactService, Page } from '../../../core/services/contact.service';
import { Contact } from '../../../shared/models/contact.model';
import { ContactFormComponent } from '../contact-form/contact-form.component';
import { ContactImportComponent } from '../contact-import/contact-import.component';
import { ContactViewComponent } from '../contact-view/contact-view.component';
import { PhoneFormatPipe } from '../../../shared/pipe/PhoneFormatPipe';

@Component({
  selector: 'app-contact-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ContactFormComponent,
    ContactImportComponent,
    ContactViewComponent,
    PhoneFormatPipe
  ],
  templateUrl: './list.component.html',
  styleUrls: ['./list.component.scss']
})
export class ContactListComponent implements OnInit {
  private contactService = inject(ContactService);

  contacts = signal<Contact[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  searchQuery = signal('');
  currentPage = signal(0);
  totalPages = signal(0);
  totalElements = signal(0);
  pageSize = 20;

  showAddForm = signal(false);
  showImportForm = signal(false);
  selectedContacts = signal<Set<string>>(new Set());

  // Contact affiché dans la modale "Voir" (null = fermée)
  viewingContact = signal<Contact | null>(null);
  // Contact en cours d'édition, passé à app-contact-form (null = mode création)
  editingContact = signal<Contact | null>(null);

  skeletonRows = Array.from({ length: 5 });

  allSelected = computed(() =>
    this.contacts().length > 0 &&
    this.contacts().every(c => this.selectedContacts().has(c.id!))
  );

  someSelected = computed(() =>
    this.selectedContacts().size > 0 && !this.allSelected()
  );

  private searchSubject = new Subject<string>();

  ngOnInit() {
    this.loadContacts();

    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(query => {
      this.searchQuery.set(query);
      this.currentPage.set(0);
      this.loadContacts();
    });
  }

  loadContacts() {
    this.loading.set(true);
    this.error.set(null);

    const observable = this.searchQuery()
      ? this.contactService.searchContacts(this.searchQuery(), {
          page: this.currentPage(),
          size: this.pageSize
        })
      : this.contactService.getContacts({
          page: this.currentPage(),
          size: this.pageSize
        });

    observable.subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.contacts.set(response.data.content);
          this.totalPages.set(response.data.totalPages);
          this.totalElements.set(response.data.totalElements);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement des contacts');
        this.loading.set(false);
        console.error(err);
      }
    });
  }

  onSearch(query: string) {
    this.searchSubject.next(query);
  }

  onPageChange(page: number) {
    this.currentPage.set(page);
    this.loadContacts();
  }

  toggleContactSelection(contactId: string) {
    const selected = new Set(this.selectedContacts());
    if (selected.has(contactId)) {
      selected.delete(contactId);
    } else {
      selected.add(contactId);
    }
    this.selectedContacts.set(selected);
  }

  toggleSelectAll() {
    if (this.allSelected()) {
      this.selectedContacts.set(new Set());
    } else {
      this.selectedContacts.set(new Set(this.contacts().map(c => c.id!)));
    }
  }

  onContactCreated() {
    this.showAddForm.set(false);
    this.editingContact.set(null);
    this.loadContacts();
  }

  onFormCancelled() {
    this.showAddForm.set(false);
    this.editingContact.set(null);
  }

  onContactsImported() {
    this.showImportForm.set(false);
    this.loadContacts();
  }

  initials(contact: Contact): string {
    const a = contact.firstName?.[0] ?? '';
    const b = contact.lastName?.[0] ?? '';
    return (a + b).toUpperCase() || '?';
  }

  // --- Voir / Modifier / Supprimer une ligne ---

  viewContact(contact: Contact) {
    this.viewingContact.set(contact);
  }

  editContact(contact: Contact) {
    this.viewingContact.set(null);
    this.editingContact.set(contact);
    this.showAddForm.set(true);
  }

  // Appelé depuis la modale de vue quand on clique sur "Modifier"
  onEditFromView(contact: Contact) {
    this.editContact(contact);
  }

  // Appelé depuis la modale de vue quand on clique sur "Supprimer"
  onDeleteFromView(contact: Contact) {
    this.viewingContact.set(null);
    this.deleteOne(contact.id!);
  }

  deleteOne(contactId: string) {
    if (confirm('Voulez-vous vraiment supprimer ce contact ?')) {
      this.contactService.deleteContacts([contactId]).subscribe({
        next: () => {
          const selected = new Set(this.selectedContacts());
          selected.delete(contactId);
          this.selectedContacts.set(selected);
          this.loadContacts();
        },
        error: (err) => {
          this.error.set('Erreur lors de la suppression');
          console.error(err);
        }
      });
    }
  }

  deleteSelected() {
    if (this.selectedContacts().size === 0) return;

    if (confirm(`Voulez-vous vraiment supprimer ${this.selectedContacts().size} contact(s) ?`)) {
      this.contactService.deleteContacts(Array.from(this.selectedContacts())).subscribe({
        next: () => {
          this.selectedContacts.set(new Set());
          this.loadContacts();
        },
        error: (err) => {
          this.error.set('Erreur lors de la suppression');
          console.error(err);
        }
      });
    }
  }
}