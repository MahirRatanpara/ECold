import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RecruiterTemplateAssignment {
  id?: number;
  recruiterId: number;
  templateId: number;
  templateName: string;
  weekAssigned: number;
  yearAssigned: number;
  assignmentStatus: 'ACTIVE' | 'COMPLETED' | 'MOVED_TO_FOLLOWUP' | 'ARCHIVED';
  emailsSent: number;
  lastEmailSentAt?: Date;
  createdAt: Date;
  updatedAt: Date;
  recruiterContact?: any;
  emailTemplate?: any;
}

export interface TemplateWeekSummary {
  startDate: string; // ISO date string format (YYYY-MM-DD)
  endDate: string;   // ISO date string format (YYYY-MM-DD)
  recruitersCount: number;
  dateRangeLabel: string; // e.g., "Sep 16-22, 2024"
}

export interface PagedAssignmentResponse {
  content: RecruiterTemplateAssignment[];
  pageable: any;
  last: boolean;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  sort: any;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class RecruiterTemplateAssignmentService {

  constructor(private http: HttpClient) { }

  getRecruitersForTemplate(templateId: number, page: number = 0, size: number = 20): Observable<PagedAssignmentResponse> {
    return this.http.get<PagedAssignmentResponse>(
      `${environment.apiUrl}/template-assignments/template/${templateId}?page=${page}&size=${size}`
    );
  }

  getRecruitersForTemplateAndDateRange(templateId: number, startDate: string, endDate: string, page: number = 0, size: number = 20): Observable<PagedAssignmentResponse> {
    return this.http.get<PagedAssignmentResponse>(
      `${environment.apiUrl}/template-assignments/template/${templateId}/date-range?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`
    );
  }

  getDateRangeSummariesForTemplate(templateId: number): Observable<TemplateWeekSummary[]> {
    return this.http.get<TemplateWeekSummary[]>(
      `${environment.apiUrl}/template-assignments/template/${templateId}/date-ranges`
    );
  }

  getAllRecruitersForDateRange(templateId: number, startDate: string, endDate: string): Observable<RecruiterTemplateAssignment[]> {
    return this.http.get<RecruiterTemplateAssignment[]>(
      `${environment.apiUrl}/template-assignments/template/${templateId}/date-range/all?startDate=${startDate}&endDate=${endDate}`
    );
  }

  assignRecruiterToTemplate(recruiterId: number, templateId: number): Observable<RecruiterTemplateAssignment> {
    return this.http.post<RecruiterTemplateAssignment>(
      `${environment.apiUrl}/template-assignments/assign`,
      { recruiterId, templateId }
    );
  }

  bulkAssignRecruitersToTemplate(recruiterIds: number[], templateId: number): Observable<RecruiterTemplateAssignment[]> {
    return this.http.post<RecruiterTemplateAssignment[]>(
      `${environment.apiUrl}/template-assignments/bulk-assign`,
      { recruiterIds, templateId }
    );
  }

  sendBulkEmailToDateRange(templateId: number, startDate: string, endDate: string, emailData: {
    subject: string;
    body: string;
    useScheduledSend: boolean;
    scheduleTime?: string;
  }): Observable<void> {
    return this.http.post<void>(
      `${environment.apiUrl}/template-assignments/template/${templateId}/date-range/send-bulk-email?startDate=${startDate}&endDate=${endDate}`,
      emailData
    );
  }

  markEmailSent(assignmentId: number): Observable<void> {
    return this.http.put<void>(
      `${environment.apiUrl}/template-assignments/${assignmentId}/mark-email-sent`,
      {}
    );
  }

  moveToFollowup(assignmentId: number): Observable<void> {
    return this.http.put<void>(
      `${environment.apiUrl}/template-assignments/${assignmentId}/move-to-followup`,
      {}
    );
  }

  deleteAssignment(assignmentId: number): Observable<void> {
    return this.http.delete<void>(
      `${environment.apiUrl}/template-assignments/${assignmentId}`
    );
  }

  bulkDeleteAssignments(assignmentIds: number[]): Observable<RecruiterTemplateAssignment[]> {
    return this.http.delete<RecruiterTemplateAssignment[]>(
      `${environment.apiUrl}/template-assignments/bulk`,
      { body: assignmentIds }
    );
  }

  getActiveAssignments(): Observable<RecruiterTemplateAssignment[]> {
    return this.http.get<RecruiterTemplateAssignment[]>(
      `${environment.apiUrl}/template-assignments/active`
    );
  }
}