import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageEvent } from '@angular/material/paginator';
import { trigger, state, style, transition, animate } from '@angular/animations';

interface IncomingEmail {
  id: string;
  senderName?: string;
  senderEmail: string;
  subject: string;
  body: string;
  receivedAt: Date;
  isRead: boolean;
  isImportant: boolean;
  hasAttachments: boolean;
  category: 'RESPONSE' | 'REJECTION' | 'INTERVIEW' | 'FOLLOW_UP' | 'OTHER';
  attachments?: { name: string; url: string }[];
}

interface EmailStats {
  unreadCount: number;
  responseCount: number;
  importantCount: number;
  totalCount: number;
}

@Component({
  selector: 'app-incoming-emails',
  templateUrl: './incoming-emails.component.html',
  styleUrls: ['./incoming-emails.component.scss'],
  animations: [
    trigger('slideIn', [
      transition(':enter', [
        style({ transform: 'translateX(100%)' }),
        animate('300ms ease-in', style({ transform: 'translateX(0%)' }))
      ])
    ])
  ]
})
export class IncomingEmailsComponent implements OnInit {
  emails: IncomingEmail[] = [];
  filteredEmails: IncomingEmail[] = [];
  paginatedEmails: IncomingEmail[] = [];
  emailStats: EmailStats | null = null;
  selectedEmail: IncomingEmail | null = null;
  
  loading = false;
  error = '';
  isScanning = false;
  searchQuery = '';
  selectedCategory = '';
  selectedDateRange = '';
  sortBy = 'date';
  
  // Pagination
  pageSize = 25;
  pageIndex = 0;
  
  unreadCount = 0;

  constructor(private snackBar: MatSnackBar) {}

  ngOnInit(): void {
    this.loadEmails();
    this.loadStats();
  }

  loadEmails(): void {
    this.loading = true;
    this.error = '';
    
    // Simulate API call with mock data
    setTimeout(() => {
      this.emails = [
        {
          id: '1',
          senderName: 'Sarah Johnson',
          senderEmail: 'sarah.johnson@techcorp.com',
          subject: 'Re: Software Engineer Position - Next Steps',
          body: 'Hi John, Thank you for your interest in the Software Engineer position at TechCorp. We were impressed with your application and would like to schedule a technical interview...',
          receivedAt: new Date('2024-01-25T14:30:00'),
          isRead: false,
          isImportant: true,
          hasAttachments: false,
          category: 'INTERVIEW'
        },
        {
          id: '2',
          senderName: 'Mike Chen',
          senderEmail: 'mike.chen@startup.io',
          subject: 'Thank you for your application',
          body: 'Dear John, Thank you for your interest in the Frontend Developer role at StartupIO. We have received your application and will review it carefully...',
          receivedAt: new Date('2024-01-24T11:15:00'),
          isRead: true,
          isImportant: false,
          hasAttachments: false,
          category: 'RESPONSE'
        },
        {
          id: '3',
          senderName: 'Lisa Rodriguez',
          senderEmail: 'l.rodriguez@bigtech.com',
          subject: 'Application Status Update - Senior Developer',
          body: 'Hello John, We wanted to provide you with an update on your application for the Senior Developer position. Unfortunately, we have decided to move forward with other candidates...',
          receivedAt: new Date('2024-01-23T16:45:00'),
          isRead: false,
          isImportant: false,
          hasAttachments: false,
          category: 'REJECTION'
        },
        {
          id: '4',
          senderName: 'David Kim',
          senderEmail: 'david.kim@innovatetech.com',
          subject: 'Follow-up on our conversation',
          body: 'Hi John, It was great talking with you last week about the Full Stack Developer opportunity. I wanted to follow up and see if you had any questions...',
          receivedAt: new Date('2024-01-22T09:20:00'),
          isRead: true,
          isImportant: false,
          hasAttachments: true,
          category: 'FOLLOW_UP',
          attachments: [{ name: 'job_description.pdf', url: '#' }]
        },
        {
          id: '5',
          senderName: 'Emma Wilson',
          senderEmail: 'emma@remotefirst.com',
          subject: 'Remote Developer Position - Quick Question',
          body: 'Hello John, I came across your profile and was impressed by your experience. We have a remote developer position that might be a perfect fit...',
          receivedAt: new Date('2024-01-21T13:10:00'),
          isRead: false,
          isImportant: false,
          hasAttachments: false,
          category: 'OTHER'
        }
      ];
      
      this.filteredEmails = [...this.emails];
      this.updateUnreadCount();
      this.applyPagination();
      this.loading = false;
    }, 1000);
  }

  loadStats(): void {
    this.emailStats = {
      unreadCount: 3,
      responseCount: 2,
      importantCount: 1,
      totalCount: 5
    };
  }

  scanInbox(): void {
    this.isScanning = true;
    
    setTimeout(() => {
      this.isScanning = false;
      this.snackBar.open('Inbox scan completed - 2 new emails found', 'Close', {
        duration: 3000,
        horizontalPosition: 'right',
        verticalPosition: 'top'
      });
    }, 2000);
  }

  applyFilter(): void {
    let filtered = this.emails;

    if (this.selectedCategory) {
      filtered = filtered.filter(email => email.category === this.selectedCategory);
    }

    if (this.selectedDateRange) {
      const now = new Date();
      filtered = filtered.filter(email => {
        const emailDate = email.receivedAt;
        switch (this.selectedDateRange) {
          case 'today':
            return emailDate.toDateString() === now.toDateString();
          case 'week':
            const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
            return emailDate >= weekAgo;
          case 'month':
            const monthAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
            return emailDate >= monthAgo;
          case 'quarter':
            const quarterAgo = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000);
            return emailDate >= quarterAgo;
          default:
            return true;
        }
      });
    }

    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(email => 
        email.subject.toLowerCase().includes(query) ||
        email.senderEmail.toLowerCase().includes(query) ||
        email.senderName?.toLowerCase().includes(query) ||
        email.body.toLowerCase().includes(query)
      );
    }

    this.filteredEmails = filtered;
    this.pageIndex = 0;
    this.applyPagination();
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.selectedCategory = '';
    this.selectedDateRange = '';
    this.applyFilter();
  }

  filterByStatus(status: 'unread' | 'important' | 'responses'): void {
    switch (status) {
      case 'unread':
        this.filteredEmails = this.emails.filter(email => !email.isRead);
        break;
      case 'important':
        this.filteredEmails = this.emails.filter(email => email.isImportant);
        break;
      case 'responses':
        this.filteredEmails = this.emails.filter(email => email.category === 'RESPONSE' || email.category === 'INTERVIEW');
        break;
    }
    this.pageIndex = 0;
    this.applyPagination();
  }

  sortEmails(): void {
    switch (this.sortBy) {
      case 'date':
        this.filteredEmails.sort((a, b) => b.receivedAt.getTime() - a.receivedAt.getTime());
        break;
      case 'sender':
        this.filteredEmails.sort((a, b) => (a.senderName || a.senderEmail).localeCompare(b.senderName || b.senderEmail));
        break;
      case 'subject':
        this.filteredEmails.sort((a, b) => a.subject.localeCompare(b.subject));
        break;
      case 'category':
        this.filteredEmails.sort((a, b) => a.category.localeCompare(b.category));
        break;
    }
    this.applyPagination();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.applyPagination();
  }

  applyPagination(): void {
    const startIndex = this.pageIndex * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    this.paginatedEmails = this.filteredEmails.slice(startIndex, endIndex);
  }

  selectEmail(email: IncomingEmail): void {
    this.selectedEmail = email;
    if (!email.isRead) {
      this.toggleRead(email);
    }
  }

  closeEmailDetail(): void {
    this.selectedEmail = null;
  }

  toggleRead(email: IncomingEmail): void {
    email.isRead = !email.isRead;
    this.updateUnreadCount();
    this.snackBar.open(`Email marked as ${email.isRead ? 'read' : 'unread'}`, 'Close', {
      duration: 2000
    });
  }

  toggleImportant(email: IncomingEmail): void {
    email.isImportant = !email.isImportant;
    this.snackBar.open(`Email ${email.isImportant ? 'marked as important' : 'removed from important'}`, 'Close', {
      duration: 2000
    });
  }

  markAllAsRead(): void {
    this.emails.forEach(email => email.isRead = true);
    this.updateUnreadCount();
    this.snackBar.open('All emails marked as read', 'Close', {
      duration: 3000
    });
  }

  updateUnreadCount(): void {
    this.unreadCount = this.emails.filter(email => !email.isRead).length;
  }

  replyToEmail(email: IncomingEmail): void {
    this.snackBar.open(`Replying to email from ${email.senderName || email.senderEmail}`, 'Close', {
      duration: 3000
    });
  }

  forwardEmail(email: IncomingEmail): void {
    this.snackBar.open(`Forwarding email: ${email.subject}`, 'Close', {
      duration: 3000
    });
  }

  addToRecruiter(email: IncomingEmail): void {
    this.snackBar.open(`Adding ${email.senderEmail} to recruiter database`, 'Close', {
      duration: 3000
    });
  }

  archiveEmail(email: IncomingEmail): void {
    this.snackBar.open(`Archived email: ${email.subject}`, 'Undo', {
      duration: 5000
    });
  }

  deleteEmail(emailId: string): void {
    const email = this.emails.find(e => e.id === emailId);
    if (email) {
      this.snackBar.open(`Deleted email: ${email.subject}`, 'Undo', {
        duration: 5000
      });
    }
  }

  downloadAttachment(attachment: { name: string; url: string }): void {
    this.snackBar.open(`Downloading ${attachment.name}`, 'Close', {
      duration: 3000
    });
  }

  getCategoryClass(category: string): string {
    switch (category) {
      case 'RESPONSE': return 'category-response';
      case 'REJECTION': return 'category-rejection';
      case 'INTERVIEW': return 'category-interview';
      case 'FOLLOW_UP': return 'category-follow-up';
      case 'OTHER': return 'category-other';
      default: return 'category-default';
    }
  }

  getCategoryIcon(category: string): string {
    switch (category) {
      case 'RESPONSE': return 'reply';
      case 'REJECTION': return 'cancel';
      case 'INTERVIEW': return 'event';
      case 'FOLLOW_UP': return 'follow_the_signs';
      case 'OTHER': return 'mail';
      default: return 'email';
    }
  }

  getEmailItemClass(email: IncomingEmail): string {
    let classes = [];
    if (!email.isRead) classes.push('unread');
    if (email.isImportant) classes.push('important');
    return classes.join(' ');
  }

  getEmailPreview(body: string): string {
    return body.length > 100 ? body.substring(0, 100) + '...' : body;
  }
}