import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface IncomingEmail {
  id: number;
  messageId: string;
  senderEmail: string;
  senderName?: string;
  subject: string;
  body?: string;
  htmlBody?: string;
  category?: string;
  priority: string;
  isRead: boolean;
  isProcessed: boolean;
  receivedAt: Date;
  createdAt: Date;
  threadId?: string;
  keywords?: string;
  confidenceScore?: number;
}

@Injectable({
  providedIn: 'root'
})
export class IncomingEmailService {

  constructor(private http: HttpClient) { }

  getAllEmails(): Observable<IncomingEmail[]> {
    return this.http.get<IncomingEmail[]>(`${environment.apiUrl}/incoming-emails`);
  }

  getEmailById(id: number): Observable<IncomingEmail> {
    return this.http.get<IncomingEmail>(`${environment.apiUrl}/incoming-emails/${id}`);
  }

  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${environment.apiUrl}/incoming-emails/${id}/read`, {});
  }

  markAsProcessed(id: number): Observable<void> {
    return this.http.patch<void>(`${environment.apiUrl}/incoming-emails/${id}/processed`, {});
  }

  categorizeEmail(id: number, category: string): Observable<void> {
    return this.http.patch<void>(`${environment.apiUrl}/incoming-emails/${id}/categorize`, { category });
  }

  getEmailsByCategory(category: string): Observable<IncomingEmail[]> {
    return this.http.get<IncomingEmail[]>(`${environment.apiUrl}/incoming-emails/category/${category}`);
  }

  searchEmails(query: string): Observable<IncomingEmail[]> {
    return this.http.get<IncomingEmail[]>(`${environment.apiUrl}/incoming-emails/search?q=${encodeURIComponent(query)}`);
  }

  getHighlights(): Observable<IncomingEmail[]> {
    return this.http.get<IncomingEmail[]>(`${environment.apiUrl}/incoming-emails/highlights`);
  }

  getRecentEmails(): Observable<IncomingEmail[]> {
    return this.http.get<IncomingEmail[]>(`${environment.apiUrl}/incoming-emails/recent`);
  }

  getUnreadCounts(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/incoming-emails/unread-counts`);
  }

  scanIncomingEmails(): Observable<any> {
    return this.http.post(`${environment.apiUrl}/incoming-emails/scan`, {});
  }
}