import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    data: { titleKey: 'loginTitle' },
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/app-shell.component').then((m) => m.AppShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'branches' },
      {
        path: 'admin',
        canActivate: [roleGuard(['SUPER_ADMIN'])],
        loadChildren: () => import('./features/admin/admin.routes').then((m) => m.adminRoutes),
      },
      {
        path: 'branches',
        loadChildren: () => import('./features/branches/branches.routes').then((m) => m.branchesRoutes),
      },
      {
        path: 'me',
        data: { titleKey: 'myProfileTitle' },
        loadComponent: () => import('./features/account/my-profile.component').then((m) => m.MyProfileComponent),
      },
      {
        path: 'me/security',
        data: { titleKey: 'securitySettingsTitle' },
        loadComponent: () =>
          import('./features/account/security-settings.component').then((m) => m.SecuritySettingsComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
