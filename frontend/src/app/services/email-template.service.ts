import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface EmailTemplate {
  id?: number;
  name: string;
  subject: string;
  body: string;
  category: 'OUTREACH' | 'FOLLOW_UP' | 'REFERRAL' | 'INTERVIEW' | 'THANK_YOU';
  status: 'ACTIVE' | 'DRAFT' | 'ARCHIVED';
  usageCount?: number;
  emailsSent?: number;
  responseRate?: number;
  tags?: string[];
  createdAt?: Date;
  lastUsed?: Date;
  updatedAt?: Date;
}

export interface TemplateStats {
  totalTemplates: number;
  activeTemplates: number;
  draftTemplates: number;
  archivedTemplates: number;
  avgResponseRate: number;
  totalUsage: number;
}

@Injectable({
  providedIn: 'root'
})
export class EmailTemplateService {

  constructor(private http: HttpClient) { }

  getAllTemplates(): Observable<EmailTemplate[]> {
    return this.http.get<EmailTemplate[]>(`${environment.apiUrl}/email-templates`);
  }

  searchTemplates(query?: string, category?: string, status?: string): Observable<EmailTemplate[]> {
    let params = new URLSearchParams();
    if (query) params.append('query', query);
    if (category) params.append('category', category);
    if (status) params.append('status', status);
    
    return this.http.get<EmailTemplate[]>(`${environment.apiUrl}/email-templates/search?${params.toString()}`);
  }

  getTemplatesByCategory(category: string): Observable<EmailTemplate[]> {
    return this.http.get<EmailTemplate[]>(`${environment.apiUrl}/email-templates/category/${category}`);
  }

  getTemplatesByStatus(status: string): Observable<EmailTemplate[]> {
    return this.http.get<EmailTemplate[]>(`${environment.apiUrl}/email-templates/status/${status}`);
  }

  getTemplateById(id: number): Observable<EmailTemplate> {
    return this.http.get<EmailTemplate>(`${environment.apiUrl}/email-templates/${id}`);
  }

  createTemplate(template: EmailTemplate): Observable<EmailTemplate> {
    return this.http.post<EmailTemplate>(`${environment.apiUrl}/email-templates`, template);
  }

  updateTemplate(id: number, template: EmailTemplate): Observable<EmailTemplate> {
    return this.http.put<EmailTemplate>(`${environment.apiUrl}/email-templates/${id}`, template);
  }

  deleteTemplate(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/email-templates/${id}`);
  }

  duplicateTemplate(id: number): Observable<EmailTemplate> {
    return this.http.post<EmailTemplate>(`${environment.apiUrl}/email-templates/${id}/duplicate`, {});
  }

  archiveTemplate(id: number): Observable<void> {
    return this.http.put<void>(`${environment.apiUrl}/email-templates/${id}/archive`, {});
  }

  activateTemplate(id: number): Observable<void> {
    return this.http.put<void>(`${environment.apiUrl}/email-templates/${id}/activate`, {});
  }

  useTemplate(id: number): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/email-templates/${id}/use`, {});
  }

  getTemplateStats(): Observable<TemplateStats> {
    return this.http.get<TemplateStats>(`${environment.apiUrl}/email-templates/stats`);
  }

  clearAllTemplates(): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/email-templates/clear-all`);
  }

  // Get active templates for email composition
  getActiveTemplates(): Observable<EmailTemplate[]> {
    return this.getTemplatesByStatus('ACTIVE');
  }
}