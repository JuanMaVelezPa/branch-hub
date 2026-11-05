import { inject } from '@angular/core';
import type { CanActivateFn } from '@angular/router';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

export function roleGuard(required: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.token()) {
      return router.createUrlTree(['/login']);
    }
    const roles = auth.roles();
    if (required.some((r) => roles.includes(r))) {
      return true;
    }
    return router.createUrlTree(['/dashboard']);
  };
}

