import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { EmailTemplate, EmailTemplateService } from '../../../services/email-template.service';

export interface TemplateEditDialogData {
  template?: EmailTemplate;
  mode: 'create' | 'edit';
}

@Component({
  selector: 'app-template-edit-dialog',
  template: `
    <h2 mat-dialog-title>
      {{ data.mode === 'create' ? 'Create New Template' : 'Edit Template' }}
    </h2>
    
    <mat-dialog-content class="dialog-content">
      <form [formGroup]="templateForm" class="template-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Template Name</mat-label>
          <input matInput formControlName="name" placeholder="Enter template name">
          <mat-error *ngIf="templateForm.get('name')?.hasError('required')">
            Template name is required
          </mat-error>
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline" class="half-width">
            <mat-label>Category</mat-label>
            <mat-select formControlName="category">
              <mat-option value="OUTREACH">
                <mat-icon class="category-icon">mail_outline</mat-icon>
                Outreach
              </mat-option>
              <mat-option value="FOLLOW_UP">
                <mat-icon class="category-icon">reply</mat-icon>
                Follow Up
              </mat-option>
              <mat-option value="REFERRAL">
                <mat-icon class="category-icon">people_outline</mat-icon>
                Referral
              </mat-option>
              <mat-option value="INTERVIEW">
                <mat-icon class="category-icon">event</mat-icon>
                Interview
              </mat-option>
              <mat-option value="THANK_YOU">
                <mat-icon class="category-icon">thumb_up</mat-icon>
                Thank You
              </mat-option>
            </mat-select>
            <mat-error *ngIf="templateForm.get('category')?.hasError('required')">
              Category is required
            </mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" class="half-width">
            <mat-label>Status</mat-label>
            <mat-select formControlName="status">
              <mat-option value="DRAFT">
                <mat-icon class="status-icon status-draft">edit</mat-icon>
                Draft
              </mat-option>
              <mat-option value="ACTIVE">
                <mat-icon class="status-icon status-active">check_circle</mat-icon>
                Active
              </mat-option>
              <mat-option value="ARCHIVED">
                <mat-icon class="status-icon status-archived">archive</mat-icon>
                Archived
              </mat-option>
            </mat-select>
            <mat-error *ngIf="templateForm.get('status')?.hasError('required')">
              Status is required
            </mat-error>
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Subject Line</mat-label>
          <input matInput formControlName="subject" placeholder="Enter email subject">
          <mat-hint>Click on placeholders below to insert into subject or body</mat-hint>
          <mat-error *ngIf="templateForm.get('subject')?.hasError('required')">
            Subject is required
          </mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Email Body</mat-label>
          <textarea matInput formControlName="body" rows="12" 
                    placeholder="Enter your email template content"></textarea>
          <mat-hint>Focus this field or the subject field, then click placeholders below</mat-hint>
          <mat-error *ngIf="templateForm.get('body')?.hasError('required')">
            Email body is required
          </mat-error>
        </mat-form-field>

        <!-- Placeholder Insertion Section -->
        <div class="placeholder-section">
          <h4 class="placeholder-title">
            <mat-icon>add_circle_outline</mat-icon>
            Available Placeholders (Click to Insert):
          </h4>
          <p class="placeholder-instruction">Position your cursor in the Subject or Body field above, then click a placeholder to insert it.</p>
          <div class="placeholder-chips">
            <mat-chip-set>
              <mat-chip *ngFor="let placeholder of availablePlaceholders" 
                        class="clickable-chip"
                        (click)="insertPlaceholderAtFocus(placeholder)"
                        [matTooltip]="placeholder.description">
                <mat-icon matChipAvatar>add</mat-icon>
                {{placeholder.label}}
              </mat-chip>
            </mat-chip-set>
          </div>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Tags (comma separated)</mat-label>
          <input matInput formControlName="tagsInput" placeholder="e.g. software, engineering, cold-outreach">
          <mat-hint>Separate tags with commas</mat-hint>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSave()" 
              [disabled]="!templateForm.valid || saving">
        <mat-icon *ngIf="saving">hourglass_empty</mat-icon>
        {{ data.mode === 'create' ? 'Create Template' : 'Update Template' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-content {
      min-width: 700px;
      max-width: 900px;
      max-height: 80vh;
      overflow-y: auto;
    }
    
    .template-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    
    .full-width {
      width: 100%;
    }
    
    .form-row {
      display: flex;
      gap: 16px;
    }
    
    .half-width {
      flex: 1;
    }
    
    .category-icon, .status-icon {
      margin-right: 8px;
      font-size: 18px;
      vertical-align: middle;
    }
    
    .status-draft { color: #ff9800; }
    .status-active { color: #4caf50; }
    .status-archived { color: #9e9e9e; }
    
    .placeholder-section {
      background: #f8f9fa;
      border: 1px solid #e9ecef;
      border-radius: 8px;
      padding: 16px;
      margin: 8px 0;
    }
    
    .placeholder-title {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      font-size: 14px;
      font-weight: 500;
      color: #495057;
    }
    
    .placeholder-title mat-icon {
      font-size: 18px;
      color: #6c757d;
    }
    
    .placeholder-instruction {
      margin: 0 0 12px 0;
      font-size: 13px;
      color: #6c757d;
      font-style: italic;
    }
    
    .placeholder-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }
    
    .clickable-chip {
      cursor: pointer;
      transition: all 0.2s ease;
      background-color: #e3f2fd !important;
      color: #1976d2 !important;
    }
    
    .clickable-chip:hover {
      background-color: #bbdefb !important;
      transform: translateY(-1px);
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    
    .clickable-chip mat-icon {
      color: #1976d2 !important;
    }
    
    textarea {
      min-height: 150px !important;
      resize: vertical;
    }
    
    mat-chip-set {
      width: 100%;
    }
  `]
})
export class TemplateEditDialogComponent implements OnInit {
  templateForm: FormGroup;
  saving = false;
  
  availablePlaceholders = [
    { key: 'recruiterName', label: 'Recruiter Name', placeholder: '{{recruiterName}}', description: 'Name of the recruiter' },
    { key: 'email', label: 'Recruiter Email', placeholder: '{{recruiterEmail}}', description: 'Email address of the recruiter' },
    { key: 'companyName', label: 'Company Name', placeholder: '{{companyName}}', description: 'Name of the company' },
    { key: 'jobRole', label: 'Job Role', placeholder: '{{jobRole}}', description: 'Position/role being applied for' },
    { key: 'linkedinProfile', label: 'LinkedIn Profile', placeholder: '{{linkedinProfile}}', description: 'LinkedIn profile URL' },
    { key: 'notes', label: 'Notes', placeholder: '{{notes}}', description: 'Additional notes about the recruiter' },
    { key: 'status', label: 'Contact Status', placeholder: '{{contactStatus}}', description: 'Current status of the contact' }
  ];

  constructor(
    public dialogRef: MatDialogRef<TemplateEditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TemplateEditDialogData,
    private formBuilder: FormBuilder,
    private templateService: EmailTemplateService
  ) {
    this.templateForm = this.formBuilder.group({
      name: ['', [Validators.required]],
      subject: ['', [Validators.required]],
      body: ['', [Validators.required]],
      category: ['OUTREACH', [Validators.required]],
      status: ['DRAFT', [Validators.required]],
      tagsInput: ['']
    });
  }

  ngOnInit(): void {
    if (this.data.mode === 'edit' && this.data.template) {
      const template = this.data.template;
      this.templateForm.patchValue({
        name: template.name,
        subject: template.subject,
        body: template.body,
        category: template.category,
        status: template.status,
        tagsInput: template.tags ? template.tags.join(', ') : ''
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  insertPlaceholderAtFocus(placeholder: any): void {
    // Determine which field is currently focused
    const activeElement = document.activeElement as HTMLInputElement | HTMLTextAreaElement;
    let fieldName: 'subject' | 'body' | null = null;
    
    if (activeElement && activeElement.getAttribute('formControlName')) {
      const controlName = activeElement.getAttribute('formControlName');
      if (controlName === 'subject' || controlName === 'body') {
        fieldName = controlName;
      }
    }
    
    // If no field is focused, default to body
    if (!fieldName) {
      fieldName = 'body';
    }
    
    const control = this.templateForm.get(fieldName);
    if (control) {
      const element = document.querySelector(`[formControlName="${fieldName}"]`) as HTMLInputElement | HTMLTextAreaElement;
      
      if (element) {
        const startPos = element.selectionStart || 0;
        const endPos = element.selectionEnd || 0;
        const currentValue = control.value || '';
        
        // Insert placeholder at cursor position
        const newValue = currentValue.substring(0, startPos) + 
                         placeholder.placeholder + 
                         currentValue.substring(endPos);
        
        control.setValue(newValue);
        
        // Focus the field and set cursor position after the inserted placeholder
        setTimeout(() => {
          element.focus();
          const newCursorPos = startPos + placeholder.placeholder.length;
          element.setSelectionRange(newCursorPos, newCursorPos);
        }, 100);
      } else {
        // Fallback: append to body field
        const bodyControl = this.templateForm.get('body');
        if (bodyControl) {
          const currentValue = bodyControl.value || '';
          const newValue = currentValue + ' ' + placeholder.placeholder;
          bodyControl.setValue(newValue);
        }
      }
    }
  }

  onSave(): void {
    if (this.templateForm.valid && !this.saving) {
      this.saving = true;
      
      const formValue = this.templateForm.value;
      const template: EmailTemplate = {
        name: formValue.name,
        subject: formValue.subject,
        body: formValue.body,
        category: formValue.category,
        status: formValue.status,
        tags: formValue.tagsInput ? 
          formValue.tagsInput.split(',').map((tag: string) => tag.trim()).filter((tag: string) => tag) : 
          []
      };

      const saveOperation = this.data.mode === 'create' 
        ? this.templateService.createTemplate(template)
        : this.templateService.updateTemplate(this.data.template!.id!, template);

      saveOperation.subscribe({
        next: (savedTemplate) => {
          this.saving = false;
          this.dialogRef.close(savedTemplate);
        },
        error: (error) => {
          this.saving = false;
          console.error('Error saving template:', error);
          // Error handling will be done by the parent component
        }
      });
    }
  }
}