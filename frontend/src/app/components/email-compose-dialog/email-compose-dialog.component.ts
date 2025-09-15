import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EmailTemplateService, EmailTemplate } from '../../services/email-template.service';
import { EmailSendService, EmailSendRequest, EmailSendResponse } from '../../services/email-send.service';
import { RecruiterContact } from '../../services/recruiter.service';

export interface EmailComposeData {
  recruiter: RecruiterContact;
  defaultTemplate?: EmailTemplate;
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

  constructor(
    public dialogRef: MatDialogRef<EmailComposeDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: EmailComposeData,
    private fb: FormBuilder,
    private templateService: EmailTemplateService,
    private emailSendService: EmailSendService,
    private snackBar: MatSnackBar
  ) {
    this.recruiter = data.recruiter;
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadTemplates();
    this.setupDefaultEmail();
  }

  private initializeForm(): void {
    this.emailForm = this.fb.group({
      to: [this.recruiter.email, [Validators.required, Validators.email]],
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
          if (defaultTemplate) {
            this.emailForm.patchValue({ templateId: defaultTemplate.id });
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
    
    this.emailForm.patchValue({
      subject: processedSubject,
      body: processedBody
    });

    // Track template usage
    this.templateService.useTemplate(templateId).subscribe({
      next: () => {
        console.log('Template usage tracked');
      },
      error: (error) => {
        console.error('Failed to track template usage:', error);
      }
    });
  }

  private processPlaceholders(text: string): string {
    if (!text) return '';
    
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

    this.sending = true;
    const formData = this.emailForm.value;
    
    // Check if we should send using template API or direct email API
    if (this.selectedTemplate && this.selectedTemplate.id) {
      // Send using template API
      this.emailSendService.sendTemplateEmail(
        this.selectedTemplate.id, 
        this.recruiter.id,
        { MyName: '[Your Name]' } // Additional placeholder data
      ).subscribe({
        next: (response: EmailSendResponse) => {
          this.handleEmailResponse(response, 'template');
        },
        error: (error) => {
          this.handleEmailError(error, 'template');
        }
      });
    } else {
      // Send using direct email API
      const emailRequest: EmailSendRequest = {
        to: formData.to,
        subject: formData.subject,
        body: formData.body,
        isHtml: false,
        recruiterId: this.recruiter.id,
        priority: 'NORMAL'
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
      this.snackBar.open(
        `Email sent successfully via ${response.provider || 'SMTP'}!`, 
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
  
  private handleEmailError(error: any, type: string): void {
    this.sending = false;
    
    console.error(`Error sending ${type} email:`, error);
    
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
}
