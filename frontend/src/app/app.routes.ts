import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'workspaces' },

  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(
        (component) => component.LoginComponent,
      ),
  },

  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(
        (component) => component.RegisterComponent,
      ),
  },

  {
    path: 'workspaces/:workspaceId/channels/:channelId',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/workspaces/workspace-dashboard/workspace-dashboard.component'
      ).then((component) => component.WorkspaceDashboardComponent),
  },

  {
    path: 'workspaces',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/workspaces/workspace-dashboard/workspace-dashboard.component'
      ).then((component) => component.WorkspaceDashboardComponent),
  },

  { path: '**', redirectTo: 'workspaces' },
];