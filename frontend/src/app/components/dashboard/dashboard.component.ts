import { Component, OnInit } from '@angular/core';
import { AuthService, User } from '../../services/auth.service';
import { DashboardService } from '../../services/dashboard.service';
import { IncomingEmailService } from '../../services/incoming-email.service';
import { ToastrService } from 'ngx-toastr';

interface DashboardStats {
  totalRecruiters: number;
  emailsSent: number;
  responseRate: number;
  activeCampaigns: number;
}

interface RecentEmail {
  id: number;
  recipientEmail: string;
  subject: string;
  status: string;
  sentAt: string;
}

interface InboxHighlight {
  id: number;
  senderEmail: string;
  subject: string;
  category: string;
  receivedAt: string;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  currentUser: User | null = null;
  dashboardStats: DashboardStats | null = null;
  recentEmails: RecentEmail[] = [];
  inboxHighlights: InboxHighlight[] = [];
  unreadCount = 0;

  constructor(
    private authService: AuthService,
    private dashboardService: DashboardService,
    private incomingEmailService: IncomingEmailService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.currentUser;
    this.loadDashboardData();
    this.loadUnreadCount();
  }

  loadDashboardData(): void {
    this.dashboardService.getDashboardStats().subscribe({
      next: (stats) => {
        this.dashboardStats = stats;
      },
      error: (error) => {
        console.error('Error loading dashboard stats:', error);
        this.toastr.error('Failed to load dashboard statistics');
      }
    });

    this.dashboardService.getRecentEmails().subscribe({
      next: (emails) => {
        this.recentEmails = emails;
      },
      error: (error) => {
        console.error('Error loading recent emails:', error);
      }
    });

    this.dashboardService.getInboxHighlights().subscribe({
      next: (highlights) => {
        this.inboxHighlights = highlights;
      },
      error: (error) => {
        console.error('Error loading inbox highlights:', error);
      }
    });
  }

  loadUnreadCount(): void {
    this.incomingEmailService.getUnreadCounts().subscribe({
      next: (counts) => {
        this.unreadCount = counts['TOTAL'] || 0;
      },
      error: (error) => {
        console.error('Error loading unread count:', error);
      }
    });
  }

  refreshData(): void {
    this.loadDashboardData();
    this.loadUnreadCount();
    this.toastr.success('Dashboard data refreshed');
  }

  scanInbox(): void {
    this.incomingEmailService.scanIncomingEmails().subscribe({
      next: () => {
        this.toastr.success('Inbox scan completed');
        this.loadUnreadCount();
      },
      error: (error) => {
        console.error('Error scanning inbox:', error);
        this.toastr.error('Failed to scan inbox');
      }
    });
  }


  getEmailStatusIcon(status: string): string {
    switch (status) {
      case 'SENT': return 'send';
      case 'DELIVERED': return 'done';
      case 'OPENED': return 'mark_email_read';
      case 'FAILED': return 'error';
      default: return 'schedule';
    }
  }

  getEmailStatusClass(status: string): string {
    switch (status) {
      case 'SENT': return 'status-sent';
      case 'DELIVERED': return 'status-delivered';
      case 'OPENED': return 'status-opened';
      case 'FAILED': return 'status-failed';
      default: return 'status-pending';
    }
  }

  getCategoryIcon(category: string): string {
    switch (category) {
      case 'SHORTLIST_INTERVIEW': return 'star';
      case 'APPLICATION_UPDATE': return 'update';
      case 'RECRUITER_OUTREACH': return 'person_add';
      case 'REJECTION_CLOSED': return 'cancel';
      default: return 'email';
    }
  }

  getCategoryClass(category: string): string {
    switch (category) {
      case 'SHORTLIST_INTERVIEW': return 'category-interview';
      case 'APPLICATION_UPDATE': return 'category-update';
      case 'RECRUITER_OUTREACH': return 'category-outreach';
      case 'REJECTION_CLOSED': return 'category-rejection';
      default: return 'category-general';
    }
  }
}