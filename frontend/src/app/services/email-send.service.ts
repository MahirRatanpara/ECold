import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface EmailSendRequest {
  to: string;
  cc?: string[];
  bcc?: string[];
  subject: string;
  body: string;
  isHtml?: boolean;
  templateId?: number;
  recruiterId?: number;
  placeholderData?: { [key: string]: string };
  priority?: 'LOW' | 'NORMAL' | 'HIGH';
  isFollowUp?: boolean;
}

export interface EmailSendResponse {
  success: boolean;
  message: string;
  messageId?: string;
  sentAt?: Date;
  errorCode?: string;
  errorDetail?: string;
  provider?: 'SMTP' | 'GMAIL_API' | 'SENDGRID' | 'MAILGUN';
}

export interface EmailConfigStatus {
  configured: boolean;
  message: string;
}

export interface EmailConfigTest {
  configValid: boolean;
  testPassed: boolean;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class EmailSendService {

  constructor(private http: HttpClient) { }

  sendEmail(emailRequest: EmailSendRequest): Observable<EmailSendResponse> {
    return this.http.post<EmailSendResponse>(`${environment.apiUrl}/emails/send`, emailRequest);
  }

  sendTemplateEmail(templateId: number, recruiterId: number, additionalData?: { [key: string]: string }): Observable<EmailSendResponse> {
    const params = new URLSearchParams();
    params.append('templateId', templateId.toString());
    params.append('recruiterId', recruiterId.toString());
    
    const url = `${environment.apiUrl}/emails/send-template?${params.toString()}`;
    return this.http.post<EmailSendResponse>(url, additionalData || {});
  }

  sendTestEmail(toEmail: string): Observable<EmailSendResponse> {
    const params = new URLSearchParams();
    params.append('toEmail', toEmail);
    
    return this.http.post<EmailSendResponse>(`${environment.apiUrl}/emails/test?${params.toString()}`, {});
  }

  testEmailConfiguration(): Observable<EmailConfigTest> {
    return this.http.get<EmailConfigTest>(`${environment.apiUrl}/emails/config/test`);
  }

  getEmailConfigurationStatus(): Observable<EmailConfigStatus> {
    return this.http.get<EmailConfigStatus>(`${environment.apiUrl}/emails/config/status`);
  }
}
