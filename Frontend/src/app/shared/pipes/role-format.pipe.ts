import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'roleFormat',
  standalone: true
})
export class RoleFormatPipe implements PipeTransform {
  transform(role: string | undefined | null): string {
    if (!role) return '';
    
    switch (role) {
      case 'SUPER_ADMIN':
        return 'Super Administrateur';
      case 'DIRECTEUR_GENERAL':
        return 'Directeur Général';
      case 'DIRECTEUR':
        return 'Chef de Département';
      case 'RESPONSABLE':
        return 'Responsable';
      case 'EMPLOYE':
        return 'Employé';
      default:
        return role.replace(/_/g, ' ');
    }
  }
}
