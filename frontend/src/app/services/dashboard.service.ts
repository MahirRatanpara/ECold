import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  constructor(private http: HttpClient) { }

  getDashboardStats(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/dashboard/stats`);
  }

  getRecentEmails(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/email-logs/recent`);
  }

  getInboxHighlights(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/incoming-emails/highlights`);
  }

  getEmailCounts(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/dashboard/email-counts`);
  }
}