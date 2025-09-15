import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

// Material imports
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatMenuModule } from '@angular/material/menu';
import { MatSortModule } from '@angular/material/sort';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';

// Third party
import { ToastrModule } from 'ngx-toastr';
import { NgxSpinnerModule } from 'ngx-spinner';
import { NgChartsModule } from 'ng2-charts';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { LoginComponent } from './components/auth/login/login.component';
import { SignupComponent } from './components/auth/signup/signup.component';
import { GoogleCallbackComponent } from './components/auth/google-callback/google-callback.component';
import { RecruiterListComponent } from './components/recruiters/recruiter-list/recruiter-list.component';
import { RecruiterEditDialogComponent } from './components/recruiters/recruiter-edit-dialog/recruiter-edit-dialog.component';
import { RecruiterViewDialogComponent } from './components/recruiters/recruiter-view-dialog/recruiter-view-dialog.component';
import { BulkStatusDialogComponent } from './components/recruiters/bulk-status-dialog/bulk-status-dialog.component';
import { EmailTemplatesComponent } from './components/templates/email-templates/email-templates.component';
import { TemplateEditDialogComponent } from './components/templates/template-edit-dialog/template-edit-dialog.component';
import { TemplatePreviewDialogComponent } from './components/templates/template-preview-dialog/template-preview-dialog.component';
import { EmailComposeDialogComponent } from './components/email-compose-dialog/email-compose-dialog.component';
import { IncomingEmailsComponent } from './components/emails/incoming-emails/incoming-emails.component';
import { AuthInterceptor } from './interceptors/auth.interceptor';

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    LoginComponent,
    SignupComponent,
    GoogleCallbackComponent,
    RecruiterListComponent,
    RecruiterEditDialogComponent,
    RecruiterViewDialogComponent,
    BulkStatusDialogComponent,
    EmailTemplatesComponent,
    TemplateEditDialogComponent,
    TemplatePreviewDialogComponent,
    EmailComposeDialogComponent,
    IncomingEmailsComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    HttpClientModule,
    FormsModule,
    ReactiveFormsModule,
    
    // Material modules
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatChipsModule,
    MatBadgeModule,
    MatDialogModule,
    MatSnackBarModule,
    MatProgressBarModule,
    MatTabsModule,
    MatMenuModule,
    MatSortModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDividerModule,
    MatChipsModule,
    
    // Third party
    ToastrModule.forRoot({
      timeOut: 3000,
      positionClass: 'toast-top-right',
      preventDuplicates: true,
    }),
    NgxSpinnerModule.forRoot(),
    NgChartsModule
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }