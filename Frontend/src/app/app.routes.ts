import { Routes } from '@angular/router';
import { LayoutShell } from './shared/components/layout-shell/layout-shell';
import { Login } from './features/auth/login/login';
import { Home } from './features/dashboard/home/home';
import { UsersList } from './features/users/users-list/users-list';
import { DepartmentsListComponent } from './features/departments/departments-list/departments-list';
import { TeamChat } from './features/chat/team-chat/team-chat';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: Login },
  {
    path: '',
    component: LayoutShell,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: Home },
      { path: 'users', component: UsersList },
      { path: 'departments', component: DepartmentsListComponent },
      { path: 'chat', component: TeamChat },
      // Other routes will be added here
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
