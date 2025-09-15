import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface RecruiterContact {
  id: number;
  email: string;
  recruiterName: string;
  companyName: string;
  jobRole: string;
  linkedinProfile?: string;
  notes?: string;
  status: string;
  lastContactedAt?: Date;
  createdAt: Date;
  updatedAt: Date;
}

export interface RecruiterImportRequest {
  csvContent: string;
}

export interface PagedResponse<T> {
  content: T[];
  pageable: {
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    pageSize: number;
    pageNumber: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class RecruiterService {

  constructor(private http: HttpClient) { }

  getAllRecruiters(): Observable<RecruiterContact[]> {
    return this.http.get<any>(`${environment.apiUrl}/recruiters?size=1000`).pipe(
      map(response => response.content || response)
    );
  }

  getRecruiters(page: number = 0, size: number = 20, status?: string, search?: string, company?: string): Observable<PagedResponse<RecruiterContact>> {
    let params = `page=${page}&size=${size}`;
    if (status) {
      params += `&status=${status}`;
    }
    if (search) {
      params += `&search=${encodeURIComponent(search)}`;
    }
    if (company) {
      params += `&company=${encodeURIComponent(company)}`;
    }
    return this.http.get<PagedResponse<RecruiterContact>>(`${environment.apiUrl}/recruiters?${params}`);
  }

  getRecruiterById(id: number): Observable<RecruiterContact> {
    return this.http.get<RecruiterContact>(`${environment.apiUrl}/recruiters/${id}`);
  }

  createRecruiter(recruiter: Partial<RecruiterContact>): Observable<RecruiterContact> {
    return this.http.post<RecruiterContact>(`${environment.apiUrl}/recruiters`, recruiter);
  }

  updateRecruiter(id: number, recruiter: Partial<RecruiterContact>): Observable<RecruiterContact> {
    return this.http.put<RecruiterContact>(`${environment.apiUrl}/recruiters/${id}`, recruiter);
  }

  deleteRecruiter(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/recruiters/${id}`);
  }

  importRecruiters(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post(`${environment.apiUrl}/recruiters/import/csv`, formData);
  }

  importRecruitersFromContent(csvContent: string): Observable<any> {
    const request: RecruiterImportRequest = { csvContent };
    return this.http.post(`${environment.apiUrl}/recruiters/import/manual`, request);
  }


  markAsContacted(id: number): Observable<RecruiterContact> {
    return this.http.put<RecruiterContact>(`${environment.apiUrl}/recruiters/${id}/mark-contacted`, {});
  }

  bulkDeleteRecruiters(ids: number[]): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/recruiters/bulk`, { body: ids });
  }

  bulkUpdateStatus(ids: number[], status: string): Observable<RecruiterContact[]> {
    return this.http.put<RecruiterContact[]>(`${environment.apiUrl}/recruiters/bulk/status`, { ids, status });
  }
}