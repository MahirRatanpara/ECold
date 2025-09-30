import { Component, Inject, OnInit } from '@angular/core';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EmailTemplateService, EmailTemplate } from '../../services/email-template.service';
import { EmailSendService, EmailSendRequest, EmailSendResponse } from '../../services/email-send.service';
import { RecruiterContact } from '../../services/recruiter.service';
import { RecruiterTemplateAssignmentService } from '../../services/recruiter-template-assignment.service';
import { ScheduleSendDialogComponent } from '../schedule-send-dialog/schedule-send-dialog.component';

export interface EmailComposeData {
  recruiter: RecruiterContact;
  defaultTemplate?: EmailTemplate;
  assignmentId?: number; // For template-based recruiters
  isBulkMode?: boolean; // For bulk sending
  bulkRecruiters?: any[]; // List of recruiters for bulk sending
}

export interface EmailComposeResult {
  action: 'sent' | 'saved_draft' | 'cancelled';
  emailData?: {
    to: string;
    subject: string;
    body: string;
    templateId?: number;
  };
}

@Component({
  selector: 'app-email-compose-dialog',
  templateUrl: './email-compose-dialog.component.html',
  styleUrls: ['./email-compose-dialog.component.scss']
})
export class EmailComposeDialogComponent implements OnInit {
  emailForm!: FormGroup;
  recruiter: RecruiterContact;

  // Template management
  templates: EmailTemplate[] = [];
  selectedTemplate: EmailTemplate | null = null;
  loadingTemplates = false;

  // Email composition
  originalSubject = '';
  originalBody = '';
  previewMode = false;

  // Loading states
  sending = false;
  savingDraft = false;

  // File attachment
  selectedFile: File | null = null;

  // Schedule send
  scheduledDateTime: Date | null = null;

  // Bulk mode
  isBulkMode = false;
  bulkRecruiters: any[] = [];

  constructor(
    public dialogRef: MatDialogRef<EmailComposeDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: EmailComposeData,
    private fb: FormBuilder,
    private templateService: EmailTemplateService,
    private emailSendService: EmailSendService,
    private templateAssignmentService: RecruiterTemplateAssignmentService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
    this.recruiter = data.recruiter;
    this.isBulkMode = data.isBulkMode || false;
    this.bulkRecruiters = data.bulkRecruiters || [];
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadTemplates();

    // Only setup default email if no default template is provided
    if (!this.data.defaultTemplate) {
      this.setupDefaultEmail();
    }
  }

  private initializeForm(): void {
    // In bulk mode, use a valid dummy email for validation, but it won't be used
    const toValue = this.isBulkMode ? 'bulk@recipients.local' : this.recruiter.email;
    const toValidators = this.isBulkMode ? [Validators.required] : [Validators.required, Validators.email];

    this.emailForm = this.fb.group({
      to: [toValue, toValidators],
      subject: ['', Validators.required],
      body: ['', Validators.required],
      templateId: [null]
    });

    // Watch for template changes
    this.emailForm.get('templateId')?.valueChanges.subscribe(templateId => {
      if (templateId) {
        this.applyTemplate(templateId);
      }
    });
  }

  private loadTemplates(): void {
    this.loadingTemplates = true;
    this.templateService.getActiveTemplates().subscribe({
      next: (templates) => {
        this.templates = templates;
        this.loadingTemplates = false;

        // Apply default template if provided
        if (this.data.defaultTemplate) {
          const defaultTemplate = this.templates.find(t => t.id === this.data.defaultTemplate?.id);
          if (defaultTemplate && defaultTemplate.id) {
            // Set both the form control and directly apply the template
            this.emailForm.patchValue({ templateId: defaultTemplate.id });
            this.applyTemplate(defaultTemplate.id);
          } else {
            // Default template not found in active templates, set up default email
            this.setupDefaultEmail();
          }
        }
      },
      error: (error) => {
        console.error('Failed to load templates:', error);
        this.loadingTemplates = false;
        this.snackBar.open('Failed to load email templates', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });

        // Set up default email if templates failed to load
        this.setupDefaultEmail();
      }
    });
  }

  private setupDefaultEmail(): void {
    // Set default subject and body if no template is selected
    const defaultSubject = `Following up on opportunities at ${this.recruiter.companyName}`;
    const defaultBody = this.getDefaultEmailBody();

    this.originalSubject = defaultSubject;
    this.originalBody = defaultBody;

    this.emailForm.patchValue({
      subject: defaultSubject,
      body: defaultBody
    });
  }

  private getDefaultEmailBody(): string {
    const recruiterName = this.recruiter.recruiterName || 'there';
    const companyName = this.recruiter.companyName;
    const jobRole = this.recruiter.jobRole;
    
    return `Hi ${recruiterName},

I hope this message finds you well. I am reaching out regarding potential opportunities at ${companyName}${jobRole ? ` for ${jobRole} positions` : ''}.

I am actively seeking new challenges and would appreciate the opportunity to discuss how my skills and experience might be a good fit for your team.

I have attached my resume for your review and would be happy to provide any additional information you might need.

Thank you for your time and consideration. I look forward to hearing from you.

Best regards,
[Your Name]`;
  }

  applyTemplate(templateId: number): void {
    const template = this.templates.find(t => t.id === templateId);
    if (!template) return;

    this.selectedTemplate = template;

    // Process placeholders in subject and body
    const processedSubject = this.processPlaceholders(template.subject);
    const processedBody = this.processPlaceholders(template.body);

    // Set these as the original values for fallback purposes
    this.originalSubject = processedSubject;
    this.originalBody = processedBody;

    this.emailForm.patchValue({
      subject: processedSubject,
      body: processedBody
    });

    // Track template usage
    this.templateService.useTemplate(templateId).subscribe({
      next: () => {
        // Template usage tracked successfully
      },
      error: (error) => {
        // Failed to track template usage - continue silently
      }
    });
  }

  private processPlaceholders(text: string): string {
    if (!text) return '';

    // In bulk mode, don't replace placeholders - keep them as-is
    if (this.isBulkMode) {
      return text;
    }

    const placeholders = {
      'Company': this.recruiter.companyName || '[Company]',
      'Role': this.recruiter.jobRole || '[Role]',
      'RecruiterName': this.recruiter.recruiterName || '[Recruiter Name]',
      'MyName': '[Your Name]' // This should be replaced with actual user name
    };

    let processedText = text;
    Object.entries(placeholders).forEach(([placeholder, value]) => {
      const regex = new RegExp(`\\{${placeholder}\\}`, 'g');
      processedText = processedText.replace(regex, value);
    });

    return processedText;
  }

  removeTemplate(): void {
    this.selectedTemplate = null;
    this.emailForm.patchValue({
      templateId: null,
      subject: this.originalSubject,
      body: this.originalBody
    });
  }

  clearPresetTemplate(): void {
    // Clear the preset template and allow manual template selection
    this.selectedTemplate = null;
    this.data.defaultTemplate = undefined;
    this.emailForm.patchValue({
      templateId: null,
      subject: this.originalSubject,
      body: this.originalBody
    });
  }

  togglePreview(): void {
    this.previewMode = !this.previewMode;
  }

  getPreviewSubject(): string {
    return this.emailForm.get('subject')?.value || '';
  }

  getPreviewBody(): string {
    const body = this.emailForm.get('body')?.value || '';
    // Convert line breaks to HTML for preview
    return body.replace(/\n/g, '<br>');
  }

  onSendEmail(): void {
    if (!this.emailForm.valid) {
      this.snackBar.open('Please fill in all required fields', 'Close', {
        duration: 3000,
        horizontalPosition: 'right',
        verticalPosition: 'top'
      });
      return;
    }

    // Handle bulk mode
    if (this.isBulkMode) {
      this.sendBulkEmails(false);
      return;
    }

    this.sending = true;
    const formData = this.emailForm.value;

    // Check if we should send using template API or direct email API
    if (this.selectedTemplate && this.selectedTemplate.id) {
      // Send using template API immediately
      this.emailSendService.sendTemplateEmail(
        this.selectedTemplate.id,
        this.recruiter.id,
        { MyName: '[Your Name]' }, // Additional placeholder data
        null // Send immediately
      ).subscribe({
        next: (response: EmailSendResponse) => {
          this.handleEmailResponse(response, 'template');
        },
        error: (error) => {
          this.handleEmailError(error, 'template');
        }
      });
    } else {
      // Send using direct email API immediately
      const emailRequest: EmailSendRequest = {
        to: formData.to,
        subject: formData.subject,
        body: formData.body,
        isHtml: false,
        recruiterId: this.recruiter.id,
        templateId: this.selectedTemplate?.id || undefined,
        priority: 'NORMAL',
        scheduleTime: undefined // Send immediately
      };

      this.emailSendService.sendEmail(emailRequest).subscribe({
        next: (response: EmailSendResponse) => {
          this.handleEmailResponse(response, 'direct');
        },
        error: (error) => {
          this.handleEmailError(error, 'direct');
        }
      });
    }
  }

  
  private handleEmailResponse(response: EmailSendResponse, type: string): void {
    this.sending = false;

    if (response.success) {
      // Handle follow-up template flow for immediate sends
      this.handleFollowUpFlow();

      const message = this.scheduledDateTime
        ? `Email scheduled successfully for ${this.scheduledDateTime.toLocaleString()}!`
        : `Email sent successfully via ${response.provider || 'SMTP'}!`;

      this.snackBar.open(
        message,
        'Close',
        {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        }
      );

      // Close dialog with success result
      this.dialogRef.close({
        action: 'sent',
        emailData: {
          to: this.emailForm.get('to')?.value,
          subject: this.emailForm.get('subject')?.value,
          body: this.emailForm.get('body')?.value,
          templateId: this.emailForm.get('templateId')?.value,
          messageId: response.messageId
        }
      } as EmailComposeResult);

    } else {
      this.snackBar.open(
        `Failed to send email: ${response.message}`,
        'Close',
        {
          duration: 7000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        }
      );
    }
  }

  private handleFollowUpFlow(): void {
    // If this is a template-based email and there's an assignment ID
    if (this.data.assignmentId) {
      // Email count is already updated by backend, just move to follow-up template
      this.templateAssignmentService.moveToFollowup(this.data.assignmentId!).subscribe({
        next: () => {
          this.snackBar.open(
            'Recruiter automatically moved to follow-up template',
            'Close',
            {
              duration: 3000,
              horizontalPosition: 'right',
              verticalPosition: 'top'
            }
          );
        },
        error: (error) => {
          // Don't show error to user as this is automatic behavior
        }
      });
    }
  }
  
  private handleEmailError(error: any, type: string): void {
    this.sending = false;
    
    // Error sending email
    
    let errorMessage = 'Failed to send email';
    if (error.error && error.error.message) {
      errorMessage = error.error.message;
    } else if (error.status === 0) {
      errorMessage = 'Unable to connect to email service. Please check your internet connection.';
    } else if (error.status === 401) {
      errorMessage = 'Authentication failed. Please log in again.';
    } else if (error.status === 403) {
      errorMessage = 'You do not have permission to send emails.';
    } else if (error.status >= 500) {
      errorMessage = 'Server error occurred. Please try again later.';
    }
    
    this.snackBar.open(errorMessage, 'Close', {
      duration: 7000,
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }

  onSaveDraft(): void {
    if (!this.emailForm.get('subject')?.value && !this.emailForm.get('body')?.value) {
      this.snackBar.open('Nothing to save as draft', 'Close', {
        duration: 3000,
        horizontalPosition: 'right',
        verticalPosition: 'top'
      });
      return;
    }

    this.savingDraft = true;
    
    // TODO: Implement draft saving to backend
    // For now, we'll just simulate saving
    setTimeout(() => {
      this.savingDraft = false;
      this.snackBar.open('Draft saved successfully', 'Close', {
        duration: 3000,
        horizontalPosition: 'right',
        verticalPosition: 'top'
      });
    }, 1000);
  }

  onCancel(): void {
    this.dialogRef.close({
      action: 'cancelled'
    } as EmailComposeResult);
  }

  // Helper methods
  getTemplateCategories(): string[] {
    const categories = [...new Set(this.templates.map(t => t.category))];
    return categories;
  }

  getTemplatesByCategory(category: string): EmailTemplate[] {
    return this.templates.filter(t => t.category === category);
  }

  getCategoryIcon(category: string): string {
    const icons: { [key: string]: string } = {
      'OUTREACH': 'send',
      'FOLLOW_UP': 'reply',
      'REFERRAL': 'group',
      'INTERVIEW': 'event',
      'THANK_YOU': 'thumb_up'
    };
    return icons[category] || 'email';
  }

  formatCategory(category: string): string {
    return category.toLowerCase().replace('_', ' ').replace(/\b\w/g, l => l.toUpperCase());
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      const allowedTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
      if (!allowedTypes.includes(file.type)) {
        this.snackBar.open('Please select a PDF, DOC, or DOCX file', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        return;
      }

      // Validate file size (5MB limit)
      const maxSizeInBytes = 5 * 1024 * 1024; // 5MB
      if (file.size > maxSizeInBytes) {
        this.snackBar.open('File size must be less than 5MB', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        return;
      }

      this.selectedFile = file;
    }
  }

  removeFile(): void {
    this.selectedFile = null;
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  openScheduleDialog(): void {
    const dialogRef = this.dialog.open(ScheduleSendDialogComponent, {
      width: '600px',
      maxWidth: '90vw',
      disableClose: false,
      data: {}
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && result.scheduled) {
        this.scheduledDateTime = result.dateTime;
        this.snackBar.open(
          `Email scheduled for ${result.dateTime.toLocaleString()}`,
          'Close',
          {
            duration: 5000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          }
        );
        // Automatically send the scheduled email
        this.sendScheduledEmail();
      }
    });
  }

  private sendScheduledEmail(): void {
    if (!this.emailForm.valid || !this.scheduledDateTime) {
      return;
    }

    // Handle bulk mode
    if (this.isBulkMode) {
      this.sendBulkEmails(true);
      return;
    }

    this.sending = true;
    const formData = this.emailForm.value;

    console.log('=== SCHEDULING EMAIL ===');
    console.log('scheduledDateTime (Date object):', this.scheduledDateTime);
    console.log('scheduledDateTime ISO:', this.scheduledDateTime.toISOString());
    console.log('scheduledDateTime locale:', this.scheduledDateTime.toLocaleString());

    // Check if we should send using template API or direct email API
    if (this.selectedTemplate && this.selectedTemplate.id) {
      // Send using template API with scheduling
      this.emailSendService.sendTemplateEmail(
        this.selectedTemplate.id,
        this.recruiter.id,
        { MyName: '[Your Name]' },
        this.scheduledDateTime
      ).subscribe({
        next: (response: EmailSendResponse) => {
          this.handleEmailResponse(response, 'template');
        },
        error: (error) => {
          this.handleEmailError(error, 'template');
        }
      });
    } else {
      // Send using direct email API with scheduling
      const emailRequest: EmailSendRequest = {
        to: formData.to,
        subject: formData.subject,
        body: formData.body,
        recruiterId: this.recruiter.id,
        placeholderData: {
          RecruiterName: this.recruiter.recruiterName || 'Recruiter',
          Company: this.recruiter.companyName || 'Company'
        },
        scheduleTime: this.scheduledDateTime
      };

      console.log('=== EMAIL REQUEST ===', JSON.stringify(emailRequest, null, 2));

      this.emailSendService.sendEmail(emailRequest).subscribe({
        next: (response: EmailSendResponse) => {
          this.handleEmailResponse(response, 'direct');
        },
        error: (error: any) => {
          this.handleEmailError(error, 'direct');
        }
      });
    }
  }

  private sendBulkEmails(isScheduled: boolean): void {
    this.sending = true;
    let successCount = 0;
    let failureCount = 0;

    // Simple loop through each recruiter
    const sendNext = (index: number) => {
      if (index >= this.bulkRecruiters.length) {
        this.sending = false;
        this.snackBar.open(
          `${successCount} emails ${isScheduled ? 'scheduled' : 'sent'}${failureCount > 0 ? `, ${failureCount} failed` : ''}!`,
          'Close',
          { duration: 5000, horizontalPosition: 'right', verticalPosition: 'top' }
        );
        this.dialogRef.close({ action: 'sent' } as EmailComposeResult);
        return;
      }

      const assignment = this.bulkRecruiters[index];
      const recruiter = assignment.recruiterContact;

      // Send to this recruiter using template API
      this.emailSendService.sendTemplateEmail(
        this.selectedTemplate!.id!,
        recruiter.id,
        { MyName: '[Your Name]' },
        isScheduled ? this.scheduledDateTime : null
      ).subscribe({
        next: () => {
          successCount++;
          sendNext(index + 1);
        },
        error: () => {
          failureCount++;
          sendNext(index + 1);
        }
      });
    };

    sendNext(0);
  }
}
