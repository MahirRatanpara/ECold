import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { EmailTemplate } from '../../../services/email-template.service';

export interface TemplatePreviewDialogData {
  template: EmailTemplate;
}

@Component({
  selector: 'app-template-preview-dialog',
  template: `
    <h2 mat-dialog-title>
      <mat-icon>preview</mat-icon>
      Template Preview: {{ data.template.name }}
    </h2>
    
    <mat-dialog-content class="preview-content">
      <div class="template-info">
        <div class="info-row">
          <mat-chip class="category-chip" [class]="getCategoryClass(data.template.category)">
            <mat-icon matChipAvatar>{{ getCategoryIcon(data.template.category) }}</mat-icon>
            {{ data.template.category }}
          </mat-chip>
          <mat-chip class="status-chip" [class]="getStatusClass(data.template.status)">
            <mat-icon matChipAvatar>{{ getStatusIcon(data.template.status) }}</mat-icon>
            {{ data.template.status }}
          </mat-chip>
        </div>
      </div>

      <mat-card class="email-preview">
        <mat-card-header>
          <mat-card-title class="email-subject">
            <strong>Subject:</strong> {{ getPreviewText(data.template.subject) }}
          </mat-card-title>
        </mat-card-header>
        
        <mat-card-content>
          <div class="email-body">
            <div class="body-content" [innerHTML]="getFormattedBody(data.template.body)"></div>
          </div>
        </mat-card-content>
      </mat-card>

      <div class="template-stats" *ngIf="data.template.usageCount || data.template.emailsSent">
        <h4>Usage Statistics</h4>
        <div class="stats-grid">
          <div class="stat-item">
            <mat-icon>repeat</mat-icon>
            <span class="stat-value">{{ data.template.usageCount || 0 }}</span>
            <span class="stat-label">Times Used</span>
          </div>
          <div class="stat-item">
            <mat-icon>send</mat-icon>
            <span class="stat-value">{{ data.template.emailsSent || 0 }}</span>
            <span class="stat-label">Emails Sent</span>
          </div>
          <div class="stat-item" *ngIf="data.template.responseRate">
            <mat-icon>trending_up</mat-icon>
            <span class="stat-value">{{ data.template.responseRate | number:'1.1-1' }}%</span>
            <span class="stat-label">Response Rate</span>
          </div>
        </div>
      </div>

      <div class="template-tags" *ngIf="data.template.tags && data.template.tags.length > 0">
        <h4>Tags</h4>
        <mat-chip-set>
          <mat-chip *ngFor="let tag of data.template.tags">{{ tag }}</mat-chip>
        </mat-chip-set>
      </div>

      <div class="placeholder-info">
        <h4>Placeholder Variables Found</h4>
        <mat-chip-set *ngIf="getPlaceholders().length > 0; else noPlaceholders">
          <mat-chip *ngFor="let placeholder of getPlaceholders()" color="accent">
            {{ placeholder }}
          </mat-chip>
        </mat-chip-set>
        <ng-template #noPlaceholders>
          <p class="no-placeholders">No placeholder variables found in this template</p>
        </ng-template>
      </div>
    </mat-dialog-content>
    
    <mat-dialog-actions align="end">
      <button mat-button (click)="onClose()">Close</button>
      <button mat-raised-button color="primary" (click)="onUseTemplate()">
        <mat-icon>send</mat-icon>
        Use Template
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .preview-content {
      min-width: 600px;
      max-width: 800px;
      max-height: 80vh;
      overflow-y: auto;
    }

    .template-info {
      margin-bottom: 16px;
    }

    .info-row {
      display: flex;
      gap: 12px;
      align-items: center;
    }

    .category-chip, .status-chip {
      font-weight: 500;
    }

    .category-outreach { background-color: #e3f2fd; color: #1976d2; }
    .category-follow-up { background-color: #f3e5f5; color: #7b1fa2; }
    .category-referral { background-color: #e8f5e8; color: #388e3c; }
    .category-interview { background-color: #fff3e0; color: #f57c00; }
    .category-thank-you { background-color: #fce4ec; color: #c2185b; }

    .status-active { background-color: #e8f5e8; color: #388e3c; }
    .status-draft { background-color: #fff3e0; color: #f57c00; }
    .status-archived { background-color: #f5f5f5; color: #757575; }

    .email-preview {
      margin: 16px 0;
    }

    .email-subject {
      font-size: 16px;
      color: #333;
    }

    .email-body {
      margin-top: 12px;
    }

    .body-content {
      white-space: pre-wrap;
      line-height: 1.6;
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    }

    .template-stats {
      margin: 20px 0;
    }

    .template-stats h4 {
      margin-bottom: 12px;
      color: #666;
      font-size: 14px;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
      gap: 16px;
    }

    .stat-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 12px;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      background-color: #fafafa;
    }

    .stat-item mat-icon {
      margin-bottom: 4px;
      color: #666;
    }

    .stat-value {
      font-size: 20px;
      font-weight: bold;
      color: #333;
    }

    .stat-label {
      font-size: 12px;
      color: #666;
      text-align: center;
    }

    .template-tags {
      margin: 16px 0;
    }

    .template-tags h4, .placeholder-info h4 {
      margin-bottom: 8px;
      color: #666;
      font-size: 14px;
    }

    .placeholder-info {
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #e0e0e0;
    }

    .no-placeholders {
      color: #999;
      font-style: italic;
      margin: 0;
    }

    .highlight-placeholder {
      background-color: #fff3cd;
      padding: 2px 4px;
      border-radius: 4px;
      font-weight: bold;
      color: #856404;
    }
  `]
})
export class TemplatePreviewDialogComponent {

  constructor(
    public dialogRef: MatDialogRef<TemplatePreviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: TemplatePreviewDialogData
  ) {}

  onClose(): void {
    this.dialogRef.close();
  }

  onUseTemplate(): void {
    this.dialogRef.close('use');
  }

  getPreviewText(text: string): string {
    return this.highlightPlaceholders(text);
  }

  getFormattedBody(body: string): string {
    return this.highlightPlaceholders(body).replace(/\n/g, '<br>');
  }

  private highlightPlaceholders(text: string): string {
    return text.replace(/\{\{[^}]+\}\}/g, (match) => 
      `<span class="highlight-placeholder">${match}</span>`
    );
  }

  getPlaceholders(): string[] {
    const text = this.data.template.subject + ' ' + this.data.template.body;
    const placeholders = text.match(/\{\{[^}]+\}\}/g) || [];
    return [...new Set(placeholders)];
  }

  getCategoryClass(category: string): string {
    return `category-${category.toLowerCase().replace('_', '-')}`;
  }

  getCategoryIcon(category: string): string {
    switch (category) {
      case 'OUTREACH': return 'mail_outline';
      case 'FOLLOW_UP': return 'reply';
      case 'REFERRAL': return 'people_outline';
      case 'INTERVIEW': return 'event';
      case 'THANK_YOU': return 'thumb_up';
      default: return 'email';
    }
  }

  getStatusClass(status: string): string {
    return `status-${status.toLowerCase()}`;
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'check_circle';
      case 'DRAFT': return 'edit';
      case 'ARCHIVED': return 'archive';
      default: return 'help';
    }
  }
}