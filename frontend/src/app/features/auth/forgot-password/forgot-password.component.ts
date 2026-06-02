import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent {
  email = '';
  isLoading = false;
  successMessage = '';
  errorMessage = '';

  private authService = inject(AuthService);
  private router = inject(Router);

  async onSubmit(): Promise<void> {
    if (!this.email?.trim()) return;

    this.isLoading = true;
    this.successMessage = '';
    this.errorMessage = '';

    try {
      await this.authService.requestPasswordReset(this.email.trim()).toPromise();
      this.successMessage = '✅ Un lien de réinitialisation a été envoyé à votre adresse e-mail.';
      
      // Redirection automatique vers le login après 4 secondes (UX du guide)
      setTimeout(() => {
        this.router.navigate(['/auth/login']);
      }, 4000);
    } catch (error: any) {
      this.errorMessage = error?.error?.message || 
                         'Une erreur est survenue lors de l’envoi du lien. Veuillez réessayer.';
    } finally {
      this.isLoading = false;
    }
  }
}