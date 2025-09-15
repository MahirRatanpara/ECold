import { Component, OnInit, ViewChild, AfterViewInit, OnDestroy } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SelectionModel } from '@angular/cdk/collections';
import { RecruiterService, RecruiterContact, PagedResponse } from '../../../services/recruiter.service';
import { RecruiterEditDialogComponent } from '../recruiter-edit-dialog/recruiter-edit-dialog.component';
import { RecruiterViewDialogComponent } from '../recruiter-view-dialog/recruiter-view-dialog.component';
import { BulkStatusDialogComponent } from '../bulk-status-dialog/bulk-status-dialog.component';
import { EmailComposeDialogComponent, EmailComposeResult } from '../../email-compose-dialog/email-compose-dialog.component';

@Component({
  selector: 'app-recruiter-list',
  templateUrl: './recruiter-list.component.html',
  styleUrls: ['./recruiter-list.component.scss']
})
export class RecruiterListComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MatSort) sort!: MatSort;

  recruiters: RecruiterContact[] = [];
  dataSource = new MatTableDataSource<RecruiterContact>();
  selection = new SelectionModel<RecruiterContact>(true, []);
  
  loading = false;
  loadingMore = false;
  error: string | null = null;
  
  // Infinite scrolling
  currentPage = 0;
  pageSize = 20;
  hasMore = true;
  totalElements = 0;
  
  // Filtering
  searchQuery = '';
  selectedStatus = '';
  selectedCompany = '';
  uniqueCompanies: string[] = [];
  
  // Debounce timer for search
  private searchTimeout: any;
  
  // Cleanup references
  private intersectionObserver?: IntersectionObserver;
  private scrollHandler?: () => void;
  
  displayedColumns: string[] = [
    'select', 'recruiterName', 'companyName', 'status', 
    'lastContactedAt', 'linkedinProfile', 'actions'
  ];

  constructor(
    private recruiterService: RecruiterService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.loadRecruiters();
  }

  ngAfterViewInit(): void {
    // Setup infinite scroll detection
    this.setupInfiniteScroll();
    
    // Handle sorting changes
    this.sort.sortChange.subscribe(() => {
      this.resetAndReload();
    });
  }

  ngOnDestroy(): void {
    // Clean up intersection observer
    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }
    
    // Clean up scroll event listener
    if (this.scrollHandler) {
      window.removeEventListener('scroll', this.scrollHandler);
    }
    
    // Clear search timeout
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
  }

  private setupInfiniteScroll(): void {
    // Use Intersection Observer API for better performance
    this.setupIntersectionObserver();
    
    // Fallback to scroll events
    this.setupScrollEvents();
  }

  private setupIntersectionObserver(): void {
    // Clean up existing observer first
    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }
    
    // Wait for the DOM to be ready and try multiple times if needed
    this.retrySetupObserver(0);
  }

  private retrySetupObserver(attempt: number): void {
    const maxAttempts = 5;
    const delay = Math.min(100 * Math.pow(2, attempt), 2000); // Exponential backoff, max 2s
    
    setTimeout(() => {
      const sentinel = document.getElementById('scroll-sentinel');
      if (sentinel) {
        this.intersectionObserver = new IntersectionObserver((entries) => {
          entries.forEach(entry => {
            console.log('Intersection entry:', {
              isIntersecting: entry.isIntersecting,
              hasMore: this.hasMore,
              loadingMore: this.loadingMore,
              loading: this.loading,
              sentinelVisible: sentinel.offsetParent !== null
            });
            if (entry.isIntersecting && this.hasMore && !this.loadingMore && !this.loading) {
              console.log('Sentinel intersecting, loading more...');
              this.loadMore();
            }
          });
        }, {
          root: null, // Use viewport as root
          rootMargin: '100px', // Start loading 100px before sentinel is visible
          threshold: 0.01 // Lower threshold for better detection
        });
        
        this.intersectionObserver.observe(sentinel);
        console.log('Intersection observer set up successfully on attempt', attempt + 1);
      } else if (attempt < maxAttempts - 1) {
        console.warn(`Scroll sentinel element not found, retrying... (attempt ${attempt + 1}/${maxAttempts})`);
        this.retrySetupObserver(attempt + 1);
      } else {
        console.error('Failed to set up intersection observer after', maxAttempts, 'attempts');
      }
    }, delay);
  }

  private setupScrollEvents(): void {
    // Window scroll event with throttling
    let scrollTimeout: any;
    
    this.scrollHandler = () => {
      if (scrollTimeout) {
        clearTimeout(scrollTimeout);
      }
      
      scrollTimeout = setTimeout(() => {
        if (this.shouldLoadMoreWindow()) {
          this.loadMore();
        }
      }, 100); // Throttle scroll events
    };

    window.addEventListener('scroll', this.scrollHandler, { passive: true });
    
    // Also listen to table container scroll
    setTimeout(() => {
      const tableContainer = document.querySelector('.table-wrapper');
      if (tableContainer) {
        tableContainer.addEventListener('scroll', () => {
          if (this.shouldLoadMore(tableContainer)) {
            this.loadMore();
          }
        }, { passive: true });
      }
    }, 100);
  }

  private shouldLoadMore(container: Element): boolean {
    if (this.loadingMore || !this.hasMore || this.loading) {
      return false;
    }
    
    const threshold = 200; // Load more when 200px from bottom
    return container.scrollTop + container.clientHeight >= container.scrollHeight - threshold;
  }

  private shouldLoadMoreWindow(): boolean {
    if (this.loadingMore || !this.hasMore || this.loading) {
      return false;
    }
    
    const threshold = 300; // Increased threshold for better UX
    return window.innerHeight + window.scrollY >= document.body.offsetHeight - threshold;
  }

  loadRecruiters(append: boolean = false): void {
    if (append) {
      this.loadingMore = true;
    } else {
      this.loading = true;
      this.error = null;
    }
    
    // Try new paginated approach first, fallback to old approach if needed
    this.recruiterService.getRecruiters(
      this.currentPage, 
      this.pageSize, 
      this.selectedStatus || undefined, 
      this.searchQuery.trim() || undefined,
      this.selectedCompany || undefined
    ).subscribe({
      next: (response: PagedResponse<RecruiterContact>) => {
        if (append) {
          // Append new data for infinite scroll
          this.recruiters = [...this.recruiters, ...response.content];
        } else {
          // Replace data for fresh load
          this.recruiters = response.content;
        }
        
        this.totalElements = response.totalElements;
        this.hasMore = !response.last;
        
        // Update data source directly since all filtering is now server-side
        this.dataSource.data = this.recruiters;
        this.selection.clear();
        this.updateUniqueCompanies();
        
        this.loading = false;
        this.loadingMore = false;
        
        // Re-setup intersection observer after data changes
        if (!append) {
          setTimeout(() => this.setupIntersectionObserver(), 100);
        }
      },
      error: (err) => {
        console.error('Pagination failed, trying fallback:', err);
        // Fallback to old approach
        this.loadRecruitersOldWay();
      }
    });
  }

  private loadRecruitersOldWay(): void {
    this.recruiterService.getAllRecruiters().subscribe({
      next: (recruiters) => {
        console.log('Fallback loaded recruiters:', recruiters);
        this.recruiters = recruiters;
        this.totalElements = recruiters.length;
        this.hasMore = false; // No pagination in old approach
        
        // Update data source directly
        this.dataSource.data = this.recruiters;
        this.selection.clear();
        this.updateUniqueCompanies();
        this.loading = false;
        this.loadingMore = false;
        
        // Re-setup intersection observer after data changes
        setTimeout(() => this.setupIntersectionObserver(), 100);
      },
      error: (err) => {
        this.error = 'Failed to load recruiters. Please try again.';
        this.loading = false;
        this.loadingMore = false;
        console.error('Fallback also failed:', err);
      }
    });
  }

  loadMore(): void {
    if (this.loadingMore || !this.hasMore) {
      return;
    }
    
    this.currentPage++;
    this.loadRecruiters(true);
  }

  resetAndReload(): void {
    this.currentPage = 0;
    this.hasMore = true;
    this.recruiters = [];
    
    // Clean up intersection observer before reloading
    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }
    
    this.loadRecruiters(false);
  }

  applyFilter(): void {
    // Clear any existing timeout
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }

    // Debounce search to avoid too many API calls
    this.searchTimeout = setTimeout(() => {
      this.resetAndReload();
    }, 300);
  }


  clearFilters(): void {
    this.searchQuery = '';
    this.selectedStatus = '';
    this.selectedCompany = '';
    this.resetAndReload();
  }

  updateUniqueCompanies(): void {
    const companies = [...new Set(this.recruiters.map(r => r.companyName))];
    this.uniqueCompanies = companies.sort();
  }

  // Selection methods
  isAllSelected(): boolean {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  masterToggle(): void {
    this.isAllSelected() ?
      this.selection.clear() :
      this.dataSource.data.forEach(row => this.selection.select(row));
  }

  // Action methods
  openAddDialog(): void {
    const dialogRef = this.dialog.open(RecruiterEditDialogComponent, {
      width: '600px',
      disableClose: true,
      data: { recruiter: null }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.snackBar.open('Recruiter added successfully!', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        // Reset and reload to show new item
        this.resetAndReload();
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
    this.loading = true;
    this.recruiterService.importRecruiters(file).subscribe({
      next: (response: any) => {
        this.loading = false;
        const count = response.count || response.length || 0;
        this.snackBar.open(`Successfully imported ${count} recruiters!`, 'Close', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.resetAndReload(); // Reload the list
      },
      error: (error) => {
        this.loading = false;
        let errorMessage = 'Failed to import CSV file. Please check the format and try again.';
        
        if (error.error && error.error.message) {
          errorMessage = error.error.message;
        } else if (error.status === 400) {
          errorMessage = 'Invalid CSV format. Please ensure the file has columns: email, company, role, recruiterName';
        }
        
        this.snackBar.open(errorMessage, 'Close', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        console.error('Import error:', error);
      }
    });
  }

  editRecruiter(recruiter: RecruiterContact): void {
    const dialogRef = this.dialog.open(RecruiterEditDialogComponent, {
      width: '600px',
      disableClose: true,
      data: { recruiter: recruiter }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.snackBar.open('Recruiter updated successfully!', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.resetAndReload();
      }
    });
  }

  viewDetails(recruiter: RecruiterContact): void {
    const dialogRef = this.dialog.open(RecruiterViewDialogComponent, {
      width: '700px',
      data: { recruiter: recruiter }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && result.action === 'edit') {
        this.editRecruiter(result.recruiter);
      } else if (result && result.action === 'email_sent') {
        // The email was sent through the compose dialog, no need to call sendEmail again
        // The recruiter status should already be updated by the dialog
        this.snackBar.open('Email sent successfully', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });
        this.resetAndReload(); // Refresh the list to show updated status
      }
    });
  }  

  bulkDelete(): void {
    if (this.selection.selected.length === 0) return;
    
    if (confirm(`Delete ${this.selection.selected.length} selected recruiters?`)) {
      const idsToDelete = this.selection.selected.map(r => r.id);
      
      this.recruiterService.bulkDeleteRecruiters(idsToDelete).subscribe({
        next: () => {
          this.snackBar.open(`Successfully deleted ${idsToDelete.length} recruiters`, 'Close', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
          this.selection.clear();
          this.resetAndReload();
        },
        error: (error) => {
          console.error('Bulk delete error:', error);
          this.snackBar.open('Failed to delete selected recruiters', 'Close', {
            duration: 5000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }
      });
    }
  }

  bulkStatusUpdate(): void {
    if (this.selection.selected.length === 0) return;
    
    const dialogRef = this.dialog.open(BulkStatusDialogComponent, {
      width: '400px',
      data: { count: this.selection.selected.length }
    });
    
    dialogRef.afterClosed().subscribe(selectedStatus => {
      if (selectedStatus) {
        const idsToUpdate = this.selection.selected.map(r => r.id);
        
        this.recruiterService.bulkUpdateStatus(idsToUpdate, selectedStatus).subscribe({
          next: (updatedRecruiters) => {
            this.snackBar.open(`Successfully updated ${updatedRecruiters.length} recruiters to ${selectedStatus}`, 'Close', {
              duration: 3000,
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
            this.selection.clear();
            this.resetAndReload();
          },
          error: (error) => {
            console.error('Bulk status update error:', error);
            this.snackBar.open('Failed to update status for selected recruiters', 'Close', {
              duration: 5000,
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
          }
        });
      }
    });
  }

  deleteRecruiter(id: number): void {
    if (confirm('Are you sure you want to delete this recruiter?')) {
      this.recruiterService.deleteRecruiter(id).subscribe({
        next: () => {
          this.snackBar.open('Recruiter deleted successfully', 'Close', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
          // Reload current page
          this.resetAndReload();
        },
        error: (err) => {
          this.error = 'Failed to delete recruiter. Please try again.';
          console.error('Error deleting recruiter:', err);
          this.snackBar.open('Failed to delete recruiter', 'Close', {
            duration: 5000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }
      });
    }
  }


  sendEmail(recruiter: RecruiterContact): void {
    const dialogRef = this.dialog.open(EmailComposeDialogComponent, {
      width: '800px',
      maxWidth: '90vw',
      disableClose: true,
      data: { recruiter: recruiter }
    });

    dialogRef.afterClosed().subscribe((result: EmailComposeResult) => {
      if (result && result.action === 'sent') {
        // Mark recruiter as contacted
        this.recruiterService.markAsContacted(recruiter.id).subscribe({
          next: (updated) => {
            // Update the local data
            const index = this.recruiters.findIndex(r => r.id === recruiter.id);
            if (index !== -1) {
              this.recruiters[index] = updated;
              this.dataSource.data = this.recruiters;
            }
            this.snackBar.open('Email sent and recruiter marked as contacted', 'Close', {
              duration: 3000,
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
          },
          error: (error) => {
            console.error('Error marking recruiter as contacted:', error);
            this.snackBar.open('Email sent but failed to update contact status', 'Close', {
              duration: 5000,
              horizontalPosition: 'right',
              verticalPosition: 'top'
            });
          }
        });
      }
    });
  }

  // Status helper methods
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
}