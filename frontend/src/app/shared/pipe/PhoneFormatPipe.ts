import { Pipe, PipeTransform } from '@angular/core';

/**
 * Formate un numéro tunisien du type +21653037419
 * en +216 53 037 419 pour une meilleure lisibilité.
 */
@Pipe({
  name: 'phoneFormat',
  standalone: true
})
export class PhoneFormatPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) return '-';

    // +216 suivi de 8 chiffres : +216 XX XXX XXX
    const match = value.match(/^(\+216)(\d{2})(\d{3})(\d{3})$/);
    if (match) {
      return `${match[1]} ${match[2]} ${match[3]} ${match[4]}`;
    }

    // Fallback : indicatif générique de 1 à 3 chiffres + reste groupé par 3
    const generic = value.match(/^(\+\d{1,3})(\d+)$/);
    if (generic) {
      const grouped = generic[2].replace(/(\d{3})(?=\d)/g, '$1 ');
      return `${generic[1]} ${grouped}`;
    }

    return value;
  }
}