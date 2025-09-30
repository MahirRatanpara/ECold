import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

interface QuickPreset {
  label: string;
  time: string;
  addDays?: number;
}

@Component({
  selector: 'app-schedule-send-dialog',
  templateUrl: './schedule-send-dialog.component.html',
  styleUrls: ['./schedule-send-dialog.component.scss']
})
export class ScheduleSendDialogComponent implements OnInit {
  selectedDate: Date | null = null;
  selectedTime: string = '09:00';
  minDate: Date = new Date();

  quickPresets: QuickPreset[] = [
    { label: 'Tomorrow 9AM', time: '09:00', addDays: 1 },
    { label: 'Tomorrow 2PM', time: '14:00', addDays: 1 },
    { label: 'Next Monday 9AM', time: '09:00', addDays: this.getDaysToNextMonday() },
    { label: 'In 1 Week', time: '09:00', addDays: 7 },
    { label: 'Custom', time: '09:00', addDays: 0 }
  ];

  constructor(
    public dialogRef: MatDialogRef<ScheduleSendDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}

  ngOnInit(): void {
    // Set minimum date to today
    this.minDate = new Date();

    // Set default to tomorrow
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    this.selectedDate = tomorrow;
  }

  private getDaysToNextMonday(): number {
    const today = new Date();
    const dayOfWeek = today.getDay(); // 0 = Sunday, 1 = Monday, etc.
    const daysUntilMonday = dayOfWeek === 0 ? 1 : 8 - dayOfWeek; // If Sunday, next Monday is 1 day. Otherwise, 8 - current day.
    return daysUntilMonday;
  }

  selectPreset(preset: QuickPreset): void {
    const targetDate = new Date();
    targetDate.setDate(targetDate.getDate() + (preset.addDays || 0));

    this.selectedDate = targetDate;
    this.selectedTime = preset.time;
  }

  getFormattedDateTime(): string {
    if (!this.selectedDate || !this.selectedTime) return '';

    const date = new Date(this.selectedDate);
    const [hours, minutes] = this.selectedTime.split(':').map(Number);
    date.setHours(hours, minutes, 0, 0);

    return date.toLocaleString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    });
  }

  isValid(): boolean {
    if (!this.selectedDate || !this.selectedTime) return false;

    const now = new Date();
    const scheduledDateTime = this.getScheduledDateTime();

    return scheduledDateTime > now;
  }

  private getScheduledDateTime(): Date {
    if (!this.selectedDate || !this.selectedTime) return new Date();

    const date = new Date(this.selectedDate);
    const [hours, minutes] = this.selectedTime.split(':').map(Number);
    date.setHours(hours, minutes, 0, 0);

    return date;
  }

  onSchedule(): void {
    if (this.isValid()) {
      const scheduledDateTime = this.getScheduledDateTime();
      this.dialogRef.close({
        scheduled: true,
        dateTime: scheduledDateTime
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close({ scheduled: false });
  }
}