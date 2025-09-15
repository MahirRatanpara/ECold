import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { RecruiterContact } from '../../../services/recruiter.service';
import { EmailComposeDialogComponent, EmailComposeResult } from '../../email-compose-dialog/email-compose-dialog.component';

@Component({
  selector: 'app-recruiter-view-dialog',
  templateUrl: './recruiter-view-dialog.component.html',
  styleUrls: ['./recruiter-view-dialog.component.scss']
})
export class RecruiterViewDialogComponent implements OnInit {
  recruiter: RecruiterContact;

  constructor(
    public dialogRef: MatDialogRef<RecruiterViewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { recruiter: RecruiterContact },
    private dialog: MatDialog
  ) {
    this.recruiter = data.recruiter;
  }

  ngOnInit(): void {
    // Component initialized with data
  }

  onClose(): void {
    this.dialogRef.close();
  }

  onEdit(): void {
    this.dialogRef.close({ action: 'edit', recruiter: this.recruiter });
  }

  getStatusIcon(status: string): string {
    switch (status) {
      case 'PENDING': return 'schedule';
      case 'CONTACTED': return 'send';
      case 'RESPONDED': return 'reply';
      case 'REJECTED': return 'cancel';
      case 'HIRED': return 'check_circle';
      default: return 'help';
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING': return 'status-pending';
      case 'CONTACTED': return 'status-contacted';
      case 'RESPONDED': return 'status-responded';
      case 'REJECTED': return 'status-rejected';
      case 'HIRED': return 'status-hired';
      default: return 'status-unknown';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PENDING': return 'accent';
      case 'CONTACTED': return 'primary';
      case 'RESPONDED': return '';
      case 'REJECTED': return 'warn';
      case 'HIRED': return '';
      default: return '';
    }
  }

  openLinkedIn(): void {
    if (this.recruiter.linkedinProfile) {
      window.open(this.recruiter.linkedinProfile, '_blank');
    }
  }

  sendEmail(): void {
    const emailDialogRef = this.dialog.open(EmailComposeDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      disableClose: true,
      data: { recruiter: this.recruiter }
    });

    emailDialogRef.afterClosed().subscribe((result: EmailComposeResult) => {
      if (result && result.action === 'sent') {
        // Close the view dialog and pass the email sent action to parent
        this.dialogRef.close({ action: 'email_sent', recruiter: this.recruiter });
      }
    });
  }
}