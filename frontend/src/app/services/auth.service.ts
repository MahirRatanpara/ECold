import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';

export interface User {
  id: number;
  email: string;
  name: string;
  profilePicture?: string;
  provider: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: User;
  expiresIn: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    this.loadStoredUser();
  }

  get currentUser(): User | null {
    return this.currentUserSubject.value;
  }

  get isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getGoogleAuthUrl(): Observable<string> {
    return this.http.get(`${environment.apiUrl}/auth/google`, { responseType: 'text' });
  }

  getMicrosoftAuthUrl(): Observable<string> {
    return this.http.get(`${environment.apiUrl}/auth/microsoft`, { responseType: 'text' });
  }

  processGoogleCallback(code: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/google/callback`, { code })
      .pipe(tap(response => this.handleLoginSuccess(response)));
  }

  processMicrosoftCallback(code: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/microsoft/callback`, { code })
      .pipe(tap(response => this.handleLoginSuccess(response)));
  }

  login(credentials: { email: string; password: string; rememberMe: boolean }): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, credentials)
      .pipe(tap(response => this.handleLoginSuccess(response)));
  }

  signup(userData: { firstName: string; lastName: string; email: string; password: string }): Observable<any> {
    return this.http.post(`${environment.apiUrl}/auth/signup`, userData);
  }

  refreshToken(): Observable<LoginResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/refresh`, {}, {
      headers: { 'Authorization': `Bearer ${refreshToken}` }
    }).pipe(tap(response => this.handleLoginSuccess(response)));
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  private handleLoginSuccess(response: LoginResponse): void {
    localStorage.setItem('token', response.token);
    localStorage.setItem('refreshToken', response.refreshToken);
    localStorage.setItem('user', JSON.stringify(response.user));
    this.currentUserSubject.next(response.user);
  }

  private loadStoredUser(): void {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      this.currentUserSubject.next(JSON.parse(userStr));
    }
  }
}