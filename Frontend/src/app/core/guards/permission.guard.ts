import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const permissionGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const currentUser = authService.getCurrentUser();

  if (!currentUser) {
    router.navigate(['/login']);
    return false;
  }

  // Check required permissions from route data
  const requiredPermissions = route.data?.['permissions'] as string[];
  
  if (requiredPermissions && requiredPermissions.length > 0) {
    if (currentUser.role === 'SUPER_ADMIN' || currentUser.role === 'DIRECTEUR_GENERAL') {
      return true;
    }
    const hasPermission = requiredPermissions.some(p => currentUser.permissions?.includes(p));
    if (!hasPermission) {
      router.navigate(['/dashboard']);
      return false;
    }
  }

  return true;
};
