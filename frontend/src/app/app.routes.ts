import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';
import { SelectOrganizationComponent } from './features/auth/select-organization/select-organization.component';
import { LayoutComponent } from './shared/components/layout/layout.component';
import { HomeComponent } from './features/dashboard/home/home.component';
import { GroupsComponent } from './features/groups/groups.component';
import {ContactListComponent } from './features/contacts/list/list.component';
import { CampaignListComponent } from './features/campaigns/list/list.component';
import {  UsersComponent } from './features/users/users.component';
import { authGuard } from './core/guards/auth.guard';
import { CampaignFormComponent } from './features/campaigns/campaign-form/campaign-form.component';
import { MessageTemplateListComponent } from './features/message-templates/list/list.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'select-organization', component: SelectOrganizationComponent },
  { path: 'auth/forgot-password', component: ForgotPasswordComponent },
  { path: 'auth/reset-password', component: ResetPasswordComponent },
  { path: 'forgot-password', redirectTo: 'auth/forgot-password', pathMatch: 'full' },
  { path: 'reset-password', redirectTo: 'auth/reset-password', pathMatch: 'full' },
  { path: 'auth/select-organization', redirectTo: 'select-organization', pathMatch: 'full' },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: HomeComponent },
      { path: 'groups', component: GroupsComponent },
      { path: 'contacts', component: ContactListComponent },
      { path: 'users', component: UsersComponent },

      // Campagnes
      { path: 'campaigns', component: CampaignListComponent },
      { path: 'campaigns/create', component: CampaignFormComponent },
      { path: 'campaigns/:id/edit', component: CampaignFormComponent },

      // Modèles de messages
      { path: 'message-templates', component: MessageTemplateListComponent },
    ]
  },
  { path: '**', redirectTo: 'login' }
];