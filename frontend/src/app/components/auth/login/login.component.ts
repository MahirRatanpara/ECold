import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  isLoading = false;
  errorMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // If user is already authenticated, redirect to dashboard
    if (this.authService.isAuthenticated) {
      this.router.navigate(['/dashboard']);
      return;
    }
  }

  loginWithGoogle(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.authService.getGoogleAuthUrl().subscribe({
      next: (authUrl: string) => {
        // Redirect to Google OAuth
        window.location.href = authUrl;
      },
      error: (error: HttpErrorResponse) => {
        this.isLoading = false;
        this.errorMessage = this.getErrorMessage(error);
      }
    });
  }


  private getErrorMessage(error: HttpErrorResponse): string {
    // Handle different error scenarios with user-friendly messages
    if (error.status === 0) {
      return 'Unable to connect to the server. Please check your internet connection.';
    }
    
    switch (error.status) {
      case 400:
        return 'Invalid email or password format. Please check your input.';
      case 401:
        return 'Invalid email or password. Please try again.';
      case 403:
        return 'Your account has been temporarily disabled. Please contact support.';
      case 404:
        return 'Login service is currently unavailable. Please try again later.';
      case 429:
        return 'Too many login attempts. Please wait a few minutes before trying again.';
      case 500:
      case 502:
      case 503:
      case 504:
        return 'Server is temporarily unavailable. Please try again in a few minutes.';
      default:
        // Check if error has a specific message from backend
        if (error.error && error.error.message) {
          return error.error.message;
        }
        if (error.error && typeof error.error === 'string') {
          return error.error;
        }
        if (error.message) {
          return error.message;
        }
        return 'Login failed. Please check your credentials and try again.';
    }
  }
}