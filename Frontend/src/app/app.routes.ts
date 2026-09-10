import { Routes } from '@angular/router';
import { LayoutShell } from './shared/components/layout-shell/layout-shell';
import { Login } from './features/auth/login/login';
import { Home } from './features/dashboard/home/home';
import { UsersList } from './features/users/users-list/users-list';
import { DepartmentsListComponent } from './features/departments/departments-list/departments-list';
import { TeamChat } from './features/chat/team-chat/team-chat';

import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';

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
      { 
        path: 'chat', 
        component: TeamChat,
        canActivate: [permissionGuard],
        data: { permissions: ['VIEW_CHAT'] }
      },
      { 
        path: 'documents', 
        canActivate: [permissionGuard],
        data: { permissions: ['VIEW_DOCUMENTS', 'MANAGE_DOCUMENTS'] },
        loadComponent: () => import('./features/documents/documents-dashboard/documents-dashboard').then(m => m.DocumentsDashboardComponent)
      },
      {
        path: 'department-docs/:id',
        canActivate: [permissionGuard],
        data: { permissions: ['VIEW_DOCUMENTS', 'MANAGE_DOCUMENTS'] },
        loadComponent: () => import('./features/documents/department-docs/department-docs').then(m => m.DepartmentDocs)
      },
      {
        path: 'my-tasks',
        loadComponent: () => import('./features/tasks/employee-tasks/employee-tasks').then(m => m.EmployeeTasksComponent)
      },
      {
        path: 'roles',
        loadComponent: () => import('./features/roles-management/roles-management').then(m => m.RolesManagementComponent)
      },
      {
        path: 'knowledge-base',
        canActivate: [permissionGuard],
        data: { permissions: ['MANAGE_KNOWLEDGE_BASE'] },
        loadComponent: () => import('./features/rag/knowledge-base/knowledge-base').then(m => m.KnowledgeBaseComponent)
      },
      {
        path: 'rag',
        loadComponent: () => import('./features/rag/rag-chat/rag-chat').then(m => m.RagChatComponent)
      },
      {
        path: 'projects',
        canActivate: [permissionGuard],
        data: { permissions: ['VIEW_PROJECTS', 'MANAGE_PROJECTS'] },
        loadComponent: () => import('./features/projects/projects-list/projects-list.component').then(m => m.ProjectsListComponent)
      },
      {
        path: 'projects/:id',
        canActivate: [permissionGuard],
        data: { permissions: ['VIEW_PROJECTS', 'MANAGE_PROJECTS'] },
        loadComponent: () => import('./features/projects/project-detail/project-detail.component').then(m => m.ProjectDetailComponent)
      },
      {
        path: 'project-tasks',
        canActivate: [permissionGuard],
        data: { permissions: ['VIEW_PROJECTS', 'MANAGE_PROJECTS'] },
        loadComponent: () => import('./features/projects/my-tasks/my-tasks.component').then(m => m.MyTasksComponent)
      },
      {
        path: 'billing-dashboard',
        loadComponent: () => import('./features/billing/billing-dashboard/billing-dashboard').then(m => m.BillingDashboardComponent)
      },
      // Other routes will be added here
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
