import { Component, OnInit, ViewChild, AfterViewInit, OnDestroy } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SelectionModel } from '@angular/cdk/collections';
import { ActivatedRoute, Router } from '@angular/router';
import { EmailTemplateService, EmailTemplate } from '../../../services/email-template.service';
import { RecruiterTemplateAssignmentService, RecruiterTemplateAssignment, TemplateWeekSummary, PagedAssignmentResponse } from '../../../services/recruiter-template-assignment.service';
import { RecruiterService, RecruiterContact } from '../../../services/recruiter.service';
import { RecruiterEditDialogComponent } from '../recruiter-edit-dialog/recruiter-edit-dialog.component';
import { BulkEmailDialogComponent } from '../bulk-email-dialog/bulk-email-dialog.component';
import { EmailComposeDialogComponent } from '../../email-compose-dialog/email-compose-dialog.component';

@Component({
  selector: 'app-template-recruiter-view',
  templateUrl: './template-recruiter-view.component.html',
  styleUrls: ['./template-recruiter-view.component.scss']
})
export class TemplateRecruiterViewComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MatSort) sort!: MatSort;

  // Template Management
  templates: EmailTemplate[] = [];
  selectedTemplate: EmailTemplate | null = null;
  selectedTemplateId: number | null = null;

  // Date Range Management
  dateRangeSummaries: TemplateWeekSummary[] = [];
  selectedDateRange: TemplateWeekSummary | null = null;
  showingAllDateRanges = true;

  // Recruiter Data
  recruiters: RecruiterTemplateAssignment[] = [];
  dataSource = new MatTableDataSource<RecruiterTemplateAssignment>();
  selection = new SelectionModel<RecruiterTemplateAssignment>(true, []);

  // Loading States
  loading = false;
  loadingMore = false;
  templatesLoading = false;
  dateRangesLoading = false;
  error: string | null = null;

  // Infinite Scrolling
  currentPage = 0;
  pageSize = 20;
  hasMore = true;
  totalElements = 0;

  // Search and Filter
  searchQuery = '';

  // Cleanup references
  private intersectionObserver?: IntersectionObserver;
  private scrollHandler?: () => void;
  private searchTimeout: any;

  displayedColumns: string[] = [
    'select', 'recruiterName', 'companyName', 'templateName',
    'dateAssigned', 'emailsSent', 'lastEmailSentAt', 'actions'
  ];

  constructor(
    private templateService: EmailTemplateService,
    private assignmentService: RecruiterTemplateAssignmentService,
    private recruiterService: RecruiterService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private route: ActivatedRoute,
    private router: Router
  ) { }

  ngOnInit(): void {
    console.log('TemplateRecruiterViewComponent: ngOnInit called');
    this.loadTemplates();

    // Check for template ID in route params
    this.route.paramMap.subscribe(params => {
      const templateId = params.get('templateId');
      if (templateId) {
        this.selectedTemplateId = parseInt(templateId, 10);
        this.onTemplateSelected();
      }
    });
  }

  testFunction(): void {
    console.log('Test button clicked');
    alert('Component is working!');
  }

  ngAfterViewInit(): void {
    this.setupInfiniteScroll();

    if (this.sort) {
      this.sort.sortChange.subscribe(() => {
        this.resetAndReloadRecruiters();
      });
    }
  }

  ngOnDestroy(): void {
    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }

    if (this.scrollHandler) {
      window.removeEventListener('scroll', this.scrollHandler);
    }

    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
  }

  loadTemplates(): void {
    console.log('Loading templates...');
    this.templatesLoading = true;
    this.templateService.getAllTemplates().subscribe({
      next: (templates) => {
        console.log('Templates loaded:', templates);
        console.log('Templates count:', templates.length);
        console.log('Active templates:', templates.filter(t => t.status === 'ACTIVE').length);
        this.templates = templates.filter(t => t.status === 'ACTIVE');
        console.log('Final templates array:', this.templates);
        this.templatesLoading = false;

        // Auto-select first template if none selected
        if (!this.selectedTemplateId && this.templates.length > 0) {
          console.log('Auto-selecting first template:', this.templates[0]);
          this.selectedTemplateId = this.templates[0].id!;
          this.selectedTemplate = this.templates[0];
          this.onTemplateSelected();
        } else {
          console.log('No templates found or template already selected. Templates:', this.templates.length, 'Selected ID:', this.selectedTemplateId);
        }
      },
      error: (error) => {
        console.error('Error loading templates:', error);
        this.templatesLoading = false;
        this.error = 'Failed to load templates: ' + error.message;
      }
    });
  }

  onTemplateSelected(): void {
    console.log('onTemplateSelected called with ID:', this.selectedTemplateId);
    if (!this.selectedTemplateId) return;

    this.selectedTemplate = this.templates.find(t => t.id === this.selectedTemplateId) || null;
    console.log('Selected template:', this.selectedTemplate);

    console.log('Loading date range summaries...');
    this.loadDateRangeSummaries();

    console.log('Resetting and reloading recruiters...');
    this.resetAndReloadRecruiters();

    // Update URL without triggering navigation
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { templateId: this.selectedTemplateId },
      queryParamsHandling: 'merge'
    });
  }

  loadDateRangeSummaries(): void {
    if (!this.selectedTemplateId) return;

    console.log('Loading date range summaries for template:', this.selectedTemplateId);
    this.dateRangesLoading = true;
    this.assignmentService.getDateRangeSummariesForTemplate(this.selectedTemplateId).subscribe({
      next: (summaries) => {
        console.log('Date range summaries loaded:', summaries);
        console.log('Number of summaries:', summaries.length);
        console.log('dateRangeSummaries.length > 0:', summaries.length > 0);
        this.dateRangeSummaries = summaries;
        this.dateRangesLoading = false;
      },
      error: (error) => {
        console.error('Error loading date range summaries:', error);
        this.dateRangesLoading = false;
      }
    });
  }

  onDateRangeSelected(dateRange: TemplateWeekSummary | null): void {
    this.selectedDateRange = dateRange;
    this.showingAllDateRanges = dateRange === null;
    this.resetAndReloadRecruiters();
  }

  resetAndReloadRecruiters(): void {
    console.log('resetAndReloadRecruiters called');
    this.currentPage = 0;
    this.hasMore = true;
    this.recruiters = [];
    this.selection.clear();

    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }

    this.loadRecruiters(false);
  }

  loadRecruiters(append: boolean = false): void {
    console.log('loadRecruiters called with append:', append, 'templateId:', this.selectedTemplateId);
    if (!this.selectedTemplateId) {
      console.log('No template selected, returning');
      return;
    }

    if (append) {
      this.loadingMore = true;
    } else {
      this.loading = true;
      this.error = null;
    }

    let observable;
    if (this.showingAllDateRanges) {
      console.log('Loading all recruiters for template:', this.selectedTemplateId);
      observable = this.assignmentService.getRecruitersForTemplate(
        this.selectedTemplateId, this.currentPage, this.pageSize
      );
    } else if (this.selectedDateRange) {
      console.log('Loading recruiters for template:', this.selectedTemplateId, 'date range:', this.selectedDateRange);
      observable = this.assignmentService.getRecruitersForTemplateAndDateRange(
        this.selectedTemplateId, this.selectedDateRange.startDate, this.selectedDateRange.endDate,
        this.currentPage, this.pageSize
      );
    } else {
      console.log('No date range selected and not showing all ranges, returning');
      this.loading = false;
      this.loadingMore = false;
      return;
    }

    console.log('Making API call...', observable);
    observable.subscribe({
      next: (response: PagedAssignmentResponse) => {
        if (append) {
          this.recruiters = [...this.recruiters, ...response.content];
        } else {
          this.recruiters = response.content;
        }

        this.totalElements = response.totalElements;
        this.hasMore = !response.last;

        this.dataSource.data = this.recruiters;
        this.loading = false;
        this.loadingMore = false;

        if (!append) {
          setTimeout(() => this.setupInfiniteScroll(), 100);
        }
      },
      error: (error) => {
        console.error('Error loading recruiters:', error);
        this.error = 'Failed to load recruiters';
        this.loading = false;
        this.loadingMore = false;
      }
    });
  }

  loadMore(): void {
    if (this.loadingMore || !this.hasMore) return;

    this.currentPage++;
    this.loadRecruiters(true);
  }

  setupInfiniteScroll(): void {
    this.retrySetupObserver(0);
  }

  private retrySetupObserver(attempt: number): void {
    const maxAttempts = 5;
    const delay = Math.min(100 * Math.pow(2, attempt), 2000);

    setTimeout(() => {
      const sentinel = document.getElementById('scroll-sentinel-template');
      if (sentinel) {
        this.intersectionObserver = new IntersectionObserver((entries) => {
          entries.forEach(entry => {
            if (entry.isIntersecting && this.hasMore && !this.loadingMore && !this.loading) {
              this.loadMore();
            }
          });
        }, {
          root: null,
          rootMargin: '100px',
          threshold: 0.01
        });

        this.intersectionObserver.observe(sentinel);
      } else if (attempt < maxAttempts - 1) {
        this.retrySetupObserver(attempt + 1);
      }
    }, delay);
  }

  // Selection methods
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows && numRows > 0;
  }

  masterToggle(): void {
    this.isAllSelected() ?
      this.selection.clear() :
      this.dataSource.data.forEach(row => this.selection.select(row));
  }

  // Actions
  openAddRecruiterDialog(): void {
    const dialogRef = this.dialog.open(RecruiterEditDialogComponent, {
      width: '600px',
      disableClose: true,
      data: { recruiter: null, templateId: this.selectedTemplateId }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.snackBar.open('Recruiter added and assigned to template!', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.resetAndReloadRecruiters();
        this.loadDateRangeSummaries(); // Refresh date range summaries
      }
    });
  }

  openImportDialog(): void {
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = '.csv';
    fileInput.style.display = 'none';

    fileInput.onchange = (event: any) => {
      const file = event.target.files[0];
      if (file) {
        this.importRecruitersFromFile(file);
      }
    };

    document.body.appendChild(fileInput);
    fileInput.click();
    document.body.removeChild(fileInput);
  }

  private importRecruitersFromFile(file: File): void {
    if (!this.selectedTemplateId) {
      this.snackBar.open('Please select a template first', 'Close', {
        duration: 3000,
        horizontalPosition: 'right',
        verticalPosition: 'top'
      });
      return;
    }

    this.loading = true;

    // Create FormData with template ID
    const formData = new FormData();
    formData.append('file', file);
    formData.append('templateId', this.selectedTemplateId.toString());

    // Use the updated import endpoint
    this.recruiterService.importRecruiters(file).subscribe({
      next: (response: any) => {
        this.loading = false;
        const count = response.count || response.length || 0;
        this.snackBar.open(`Successfully imported ${count} recruiters and assigned to template!`, 'Close', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.resetAndReloadRecruiters();
        this.loadDateRangeSummaries(); // Refresh date range summaries
      },
      error: (error) => {
        this.loading = false;
        let errorMessage = 'Failed to import CSV file. Please check the format and try again.';

        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        }

        this.snackBar.open(errorMessage, 'Close', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      }
    });
  }

  sendBulkEmailToDateRange(): void {
    if (!this.selectedDateRange || !this.selectedTemplateId) return;

    const dialogRef = this.dialog.open(BulkEmailDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      disableClose: true,
      data: {
        templateId: this.selectedTemplateId,
        templateName: this.selectedTemplate?.name,
        dateRange: this.selectedDateRange,
        recruitersCount: this.selectedDateRange.recruitersCount
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.assignmentService.sendBulkEmailToDateRange(
          this.selectedTemplateId!,
          this.selectedDateRange!.startDate,
          this.selectedDateRange!.endDate,
          {
            subject: result.subject,
            body: result.body,
            useScheduledSend: result.useScheduledSend,
            scheduleTime: result.scheduleTime
          }
        ).subscribe({
          next: () => {
            this.snackBar.open('Bulk emails sent successfully!', 'Close', {
              duration: 3000,
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
            this.resetAndReloadRecruiters();
            this.loadDateRangeSummaries();
          },
          error: (error) => {
            console.error('Error sending bulk emails:', error);
            this.snackBar.open('Failed to send bulk emails', 'Close', {
              duration: 5000,
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
          }
        });
      }
    });
  }

  selectAllInDateRange(): void {
    if (!this.selectedDateRange || !this.selectedTemplateId) return;

    this.assignmentService.getAllRecruitersForDateRange(
      this.selectedTemplateId, this.selectedDateRange.startDate, this.selectedDateRange.endDate
    ).subscribe({
      next: (assignments) => {
        // Clear current selection and select all from this date range
        this.selection.clear();
        assignments.forEach(assignment => {
          const item = this.dataSource.data.find(d => d.id === assignment.id);
          if (item) {
            this.selection.select(item);
          }
        });
      },
      error: (error) => {
        console.error('Error selecting all in date range:', error);
      }
    });
  }

  bulkDeleteSelected(): void {
    if (this.selection.selected.length === 0) return;

    if (confirm(`Remove ${this.selection.selected.length} selected recruiters from this template?`)) {
      const assignmentIds = this.selection.selected.map(a => a.id!);

      this.assignmentService.bulkDeleteAssignments(assignmentIds).subscribe({
        next: () => {
          this.snackBar.open(`Successfully removed ${assignmentIds.length} recruiters from template`, 'Close', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
          this.selection.clear();
          this.resetAndReloadRecruiters();
          this.loadDateRangeSummaries();
        },
        error: (error) => {
          console.error('Bulk delete error:', error);
          this.snackBar.open('Failed to remove selected recruiters', 'Close', {
            duration: 5000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }
      });
    }
  }

  deleteAssignment(assignmentId: number): void {
    if (confirm('Remove this recruiter from the template?')) {
      this.assignmentService.deleteAssignment(assignmentId).subscribe({
        next: () => {
          this.snackBar.open('Recruiter removed from template', 'Close', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
          this.resetAndReloadRecruiters();
          this.loadDateRangeSummaries();
        },
        error: (error) => {
          console.error('Error removing recruiter:', error);
          this.snackBar.open('Failed to remove recruiter', 'Close', {
            duration: 5000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }
      });
    }
  }

  moveToFollowup(assignmentId: number): void {
    this.assignmentService.moveToFollowup(assignmentId).subscribe({
      next: () => {
        this.snackBar.open('Recruiter moved to follow-up template', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.resetAndReloadRecruiters();
        this.loadDateRangeSummaries();
      },
      error: (error) => {
        console.error('Error moving to follow-up:', error);
        this.snackBar.open('Failed to move recruiter to follow-up', 'Close', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
      }
    });
  }

  applyFilter(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }

    this.searchTimeout = setTimeout(() => {
      // Implement client-side filtering for now
      if (!this.searchQuery.trim()) {
        this.dataSource.filter = '';
      } else {
        this.dataSource.filter = this.searchQuery.trim().toLowerCase();
      }
    }, 300);
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.dataSource.filter = '';
  }

  // Utility methods

  formatDate(date: Date | string | undefined): string {
    if (!date) return 'Never';
    return new Date(date).toLocaleDateString();
  }

  // Additional method needed for the new UI
  sendEmail(assignment: RecruiterTemplateAssignment): void {
    const dialogRef = this.dialog.open(EmailComposeDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      disableClose: true,
      data: {
        recruiter: assignment.recruiterContact,
        defaultTemplate: this.selectedTemplate,
        assignmentId: assignment.id
      }
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result && result.action === 'sent') {
        this.snackBar.open('Email sent successfully', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.resetAndReloadRecruiters();
      }
    });
  }
}