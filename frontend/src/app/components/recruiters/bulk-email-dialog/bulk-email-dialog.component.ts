import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { EmailTemplate, EmailTemplateService } from '../../../services/email-template.service';
import { RecruiterTemplateAssignmentService, TemplateWeekSummary } from '../../../services/recruiter-template-assignment.service';
import { MatSnackBar } from '@angular/material/snack-bar';

export interface BulkEmailDialogData {
  templateId: number;
  templateName: string;
  dateRange: TemplateWeekSummary;
  recruitersCount: number;
}

export interface BulkEmailResult {
  subject: string;
  body: string;
  useScheduledSend: boolean;
  scheduleTime?: string;
}

@Component({
  selector: 'app-bulk-email-dialog',
  templateUrl: './bulk-email-dialog.component.html',
  styleUrls: ['./bulk-email-dialog.component.scss']
})
export class BulkEmailDialogComponent implements OnInit {
  form: FormGroup;
  previewMode = false;
  loading = false;
  template: EmailTemplate | null = null;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<BulkEmailDialogComponent>,
    private emailTemplateService: EmailTemplateService,
    private templateAssignmentService: RecruiterTemplateAssignmentService,
    private snackBar: MatSnackBar,
    @Inject(MAT_DIALOG_DATA) public data: BulkEmailDialogData
  ) {
    this.form = this.fb.group({
      subject: ['', [Validators.required]],
      body: ['', [Validators.required]],
      useScheduledSend: [false],
      scheduleTime: [''],
    });
  }

  ngOnInit(): void {
    // Load template data
    this.loadTemplate();

    // Set up validators for schedule time when scheduled send is enabled
    this.form.get('useScheduledSend')?.valueChanges.subscribe(useScheduled => {
      const scheduleTimeControl = this.form.get('scheduleTime');
      if (useScheduled) {
        scheduleTimeControl?.setValidators([Validators.required]);
      } else {
        scheduleTimeControl?.clearValidators();
      }
      scheduleTimeControl?.updateValueAndValidity();
    });
  }

  loadTemplate(): void {
    this.emailTemplateService.getTemplateById(this.data.templateId).subscribe({
      next: (template) => {
        this.template = template;
        this.form.patchValue({
          subject: template.subject,
          body: template.body
        });
      },
      error: (error) => {
        console.error('Error loading template:', error);
        this.snackBar.open('Failed to load template data', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      }
    });
  }

  togglePreview(): void {
    this.previewMode = !this.previewMode;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSend(): void {
    if (this.form.valid) {
      this.loading = true;

      const emailData = {
        subject: this.form.get('subject')?.value,
        body: this.form.get('body')?.value,
        useScheduledSend: this.form.get('useScheduledSend')?.value,
        scheduleTime: this.form.get('scheduleTime')?.value
      };

      this.templateAssignmentService.sendBulkEmailToDateRange(
        this.data.templateId,
        this.data.dateRange.startDate,
        this.data.dateRange.endDate,
        emailData
      ).subscribe({
        next: () => {
          this.loading = false;
          this.snackBar.open(
            `Bulk email sent to ${this.data.recruitersCount} recruiters in ${this.data.dateRange.dateRangeLabel}!`,
            'Close',
            {
              duration: 5000,
              horizontalPosition: 'right',
              verticalPosition: 'top'
            }
          );
          this.dialogRef.close(true);
        },
        error: (error: any) => {
          this.loading = false;
          console.error('Error sending bulk email:', error);
          this.snackBar.open('Failed to send bulk email. Please try again.', 'Close', {
            duration: 5000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }
      });
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.form.controls).forEach(key => {
        this.form.get(key)?.markAsTouched();
      });
    }
  }

  // Template variable substitutions
  getSubjectPreview(): string {
    return this.form.get('subject')?.value || '';
  }

  getBodyPreview(): string {
    let body = this.form.get('body')?.value || '';

    // Replace common template variables with sample values
    body = body.replace(/\{recruiterName\}/g, 'John Doe');
    body = body.replace(/\{companyName\}/g, 'Sample Company');
    body = body.replace(/\{jobTitle\}/g, 'Software Engineer');
    body = body.replace(/\{senderName\}/g, 'Your Name');

    return body;
  }

  // Utility methods
  getScheduleDateTime(): string {
    const scheduleTime = this.form.get('scheduleTime')?.value;
    if (scheduleTime) {
      return new Date(scheduleTime).toLocaleString();
    }
    return '';
  }

  getMinDateTime(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() + 5); // Minimum 5 minutes from now
    return now.toISOString().slice(0, 16);
  }

}