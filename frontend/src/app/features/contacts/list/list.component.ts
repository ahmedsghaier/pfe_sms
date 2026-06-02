import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ContactService, Page } from '../../../core/services/contact.service';
import { Contact } from '../../../shared/models/contact.model';
import { ContactFormComponent } from '../contact-form/contact-form.component';
import { ContactImportComponent } from '../contact-import/contact-import.component';

@Component({
  selector: 'app-contact-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ContactFormComponent, ContactImportComponent],
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

  ngOnInit() {
    this.loadContacts();
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
    this.searchQuery.set(query);
    this.currentPage.set(0);
    this.loadContacts();
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

  onContactCreated() {
    this.showAddForm.set(false);
    this.loadContacts();
  }

  onContactsImported() {
    this.showImportForm.set(false);
    this.loadContacts();
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