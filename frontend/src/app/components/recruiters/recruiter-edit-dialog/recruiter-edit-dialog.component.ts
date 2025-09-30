import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RecruiterService, RecruiterContact } from '../../../services/recruiter.service';

@Component({
  selector: 'app-recruiter-edit-dialog',
  templateUrl: './recruiter-edit-dialog.component.html',
  styleUrls: ['./recruiter-edit-dialog.component.scss']
})
export class RecruiterEditDialogComponent implements OnInit {
  editForm: FormGroup;
  loading = false;
  isEditMode = false;

  statuses = [
    { value: 'PENDING', label: 'Pending' },
    { value: 'CONTACTED', label: 'Contacted' },
    { value: 'RESPONDED', label: 'Responded' },
    { value: 'REJECTED', label: 'Rejected' },
    { value: 'HIRED', label: 'Hired' }
  ];

  constructor(
    private fb: FormBuilder,
    private recruiterService: RecruiterService,
    private snackBar: MatSnackBar,
    public dialogRef: MatDialogRef<RecruiterEditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { recruiter?: RecruiterContact; templateId?: number }
  ) {
    this.isEditMode = !!data?.recruiter;
    this.editForm = this.createForm();
  }

  ngOnInit(): void {
    if (this.isEditMode && this.data.recruiter) {
      this.populateForm(this.data.recruiter);
    }
  }

  private createForm(): FormGroup {
    return this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      recruiterName: ['', Validators.required],
      companyName: ['', Validators.required],
      jobRole: ['', Validators.required],
      linkedinProfile: [''],
      notes: [''],
      status: ['PENDING', Validators.required]
    });
  }

  private populateForm(recruiter: RecruiterContact): void {
    this.editForm.patchValue({
      email: recruiter.email,
      recruiterName: recruiter.recruiterName,
      companyName: recruiter.companyName,
      jobRole: recruiter.jobRole,
      linkedinProfile: recruiter.linkedinProfile || '',
      notes: recruiter.notes || '',
      status: recruiter.status
    });
  }

  onSubmit(): void {
    if (this.editForm.valid) {
      this.loading = true;
      const formData = this.editForm.value;

      const request = this.isEditMode
        ? this.recruiterService.updateRecruiter(this.data.recruiter!.id, formData)
        : this.recruiterService.createRecruiter(formData, this.data.templateId);

      request.subscribe({
        next: (result) => {
          this.loading = false;
          const message = this.isEditMode ? 'Recruiter updated successfully!' : 'Recruiter created successfully!';
          this.snackBar.open(message, 'Close', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
          this.dialogRef.close(result);
        },
        error: (error) => {
          this.loading = false;
          const message = this.isEditMode ? 'Failed to update recruiter' : 'Failed to create recruiter';
          this.snackBar.open(message, 'Close', {
            duration: 5000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
          console.error('Error saving recruiter:', error);
        }
      });
    } else {
      this.markFormGroupTouched();
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.editForm.controls).forEach(key => {
      this.editForm.get(key)?.markAsTouched();
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  getErrorMessage(fieldName: string): string {
    const field = this.editForm.get(fieldName);
    if (field?.hasError('required')) {
      return `${fieldName.charAt(0).toUpperCase() + fieldName.slice(1)} is required`;
    }
    if (field?.hasError('email')) {
      return 'Please enter a valid email address';
    }
    return '';
  }
}