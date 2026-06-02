import { Component, Output, EventEmitter, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ContactService } from '../../../core/services/contact.service';

interface UploadState {
  file: File | null;
  uploading: boolean;
  progress: number;
  error: string | null;
  result: {
    totalProcessed: number;
    successCount: number;
    failureCount: number;
    errors?: string[];
  } | null;
}

@Component({
  selector: 'app-contact-import',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './contact-import.component.html',
  styleUrls: ['./contact-import.component.scss']
})
export class ContactImportComponent {
  private contactService = inject(ContactService);

  @Output() imported = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  uploadState = signal<UploadState>({
    file: null,
    uploading: false,
    progress: 0,
    error: null,
    result: null
  });

  dragOver = signal(false);
  maxFileSize = 30 * 1024 * 1024; // 30 MB

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.handleFile(input.files[0]);
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver.set(true);
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver.set(false);

    if (event.dataTransfer?.files && event.dataTransfer.files.length > 0) {
      this.handleFile(event.dataTransfer.files[0]);
    }
  }

  handleFile(file: File) {
    // Vérifier le type de fichier
    if (file.type !== 'text/csv' && !file.name.endsWith('.csv')) {
      this.uploadState.update(state => ({
        ...state,
        error: 'Seuls les fichiers CSV sont acceptés'
      }));
      return;
    }

    // Vérifier la taille
    if (file.size > this.maxFileSize) {
      this.uploadState.update(state => ({
        ...state,
        error: `La taille du fichier ne doit pas dépasser 30 MB`
      }));
      return;
    }

    this.uploadState.update(state => ({
      ...state,
      file,
      error: null,
      result: null
    }));
  }

  removeFile() {
    this.uploadState.update(state => ({
      ...state,
      file: null,
      error: null,
      result: null
    }));
  }

  uploadFile() {
    const file = this.uploadState().file;
    if (!file) return;

    this.uploadState.update(state => ({
      ...state,
      uploading: true,
      progress: 0,
      error: null
    }));

    // Simuler la progression
    const progressInterval = setInterval(() => {
      this.uploadState.update(state => ({
        ...state,
        progress: Math.min(state.progress + 10, 90)
      }));
    }, 200);

    this.contactService.importContacts(file).subscribe({
      next: (response) => {
        clearInterval(progressInterval);
        
        if (response.success && response.data) {
          this.uploadState.update(state => ({
            ...state,
            uploading: false,
            progress: 100,
            result: response.data
          }));

          // Auto-close after successful import
          setTimeout(() => {
            this.imported.emit();
          }, 2000);
        }
      },
      error: (err) => {
        clearInterval(progressInterval);
        
        this.uploadState.update(state => ({
          ...state,
          uploading: false,
          progress: 0,
          error: err.error?.message || 'Erreur lors de l\'importation'
        }));
      }
    });
  }

  downloadTemplate() {
    const csvContent = 'phone,firstName,lastName,email\n' +
                       '+21612345678,John,Doe,john.doe@example.com\n' +
                       '+33612345678,Jane,Smith,jane.smith@example.com';
    
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    link.setAttribute('href', url);
    link.setAttribute('download', 'template_contacts.csv');
    link.style.visibility = 'hidden';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  onCancel() {
    this.cancelled.emit();
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }
}