import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { LoginComponent } from './components/auth/login/login.component';
import { GoogleCallbackComponent } from './components/auth/google-callback/google-callback.component';
import { TemplateRecruiterViewComponent } from './components/recruiters/template-recruiter-view/template-recruiter-view.component';
import { EmailTemplatesComponent } from './components/templates/email-templates/email-templates.component';
import { IncomingEmailsComponent } from './components/emails/incoming-emails/incoming-emails.component';
import { AuthGuard } from './guards/auth.guard';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'auth/google/callback', component: GoogleCallbackComponent },
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'template-recruiters',
    component: TemplateRecruiterViewComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'templates',
    component: EmailTemplatesComponent,
    canActivate: [AuthGuard]
  },
  {
    path: 'inbox',
    component: IncomingEmailsComponent,
    canActivate: [AuthGuard]
  },
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }