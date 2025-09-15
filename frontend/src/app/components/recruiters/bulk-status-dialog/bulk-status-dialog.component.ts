import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

export interface BulkStatusDialogData {
  count: number;
}

@Component({
  selector: 'app-bulk-status-dialog',
  template: `
    <h2 mat-dialog-title>Update Status for {{ data.count }} Recruiters</h2>
    
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>New Status</mat-label>
        <mat-select [(value)]="selectedStatus" required>
          <mat-option value="PENDING">
            <mat-icon class="status-icon status-pending">schedule</mat-icon>
            Pending
          </mat-option>
          <mat-option value="CONTACTED">
            <mat-icon class="status-icon status-contacted">send</mat-icon>
            Contacted
          </mat-option>
          <mat-option value="RESPONDED">
            <mat-icon class="status-icon status-responded">reply</mat-icon>
            Responded
          </mat-option>
          <mat-option value="REJECTED">
            <mat-icon class="status-icon status-rejected">cancel</mat-icon>
            Rejected
          </mat-option>
          <mat-option value="HIRED">
            <mat-icon class="status-icon status-hired">check_circle</mat-icon>
            Hired
          </mat-option>
        </mat-select>
      </mat-form-field>
    </mat-dialog-content>
    
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="onUpdate()" [disabled]="!selectedStatus">
        Update Status
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width {
      width: 100%;
    }
    
    .status-icon {
      margin-right: 8px;
      font-size: 18px;
      vertical-align: middle;
    }
    
    .status-pending { color: #ff9800; }
    .status-contacted { color: #2196f3; }
    .status-responded { color: #4caf50; }
    .status-rejected { color: #f44336; }
    .status-hired { color: #8bc34a; }
    
    mat-dialog-content {
      min-width: 300px;
      padding-top: 16px;
    }
  `]
})
export class BulkStatusDialogComponent {
  selectedStatus: string = '';
  
  constructor(
    public dialogRef: MatDialogRef<BulkStatusDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: BulkStatusDialogData
  ) {}
  
  onCancel(): void {
    this.dialogRef.close();
  }
  
  onUpdate(): void {
    if (this.selectedStatus) {
      this.dialogRef.close(this.selectedStatus);
    }
  }
}