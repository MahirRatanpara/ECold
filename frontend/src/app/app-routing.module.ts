import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { LoginComponent } from './components/auth/login/login.component';
import { SignupComponent } from './components/auth/signup/signup.component';
import { GoogleCallbackComponent } from './components/auth/google-callback/google-callback.component';
import { RecruiterListComponent } from './components/recruiters/recruiter-list/recruiter-list.component';
import { EmailTemplatesComponent } from './components/templates/email-templates/email-templates.component';
import { IncomingEmailsComponent } from './components/emails/incoming-emails/incoming-emails.component';
import { AuthGuard } from './guards/auth.guard';

const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent },
  { path: 'auth/google/callback', component: GoogleCallbackComponent },
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },
  { 
    path: 'dashboard', 
    component: DashboardComponent, 
    canActivate: [AuthGuard] 
  },
  { 
    path: 'recruiters', 
    component: RecruiterListComponent, 
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
  { path: '**', redirectTo: '/dashboard' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }