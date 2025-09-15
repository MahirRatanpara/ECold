import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService, LoginResponse } from '../../../services/auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-google-callback',
  template: `
    <div class="callback-container">
      <mat-card class="callback-card">
        <mat-card-content>
          <div class="loading-content">
            <mat-spinner diameter="50"></mat-spinner>
            <h2 class="loading-title">Completing Google Sign-In</h2>
            <p class="loading-message">{{ loadingMessage }}</p>
          </div>

          <div *ngIf="errorMessage" class="error-content">
            <mat-icon color="warn" class="error-icon">error</mat-icon>
            <h2 class="error-title">Authentication Failed</h2>
            <p class="error-message">{{ errorMessage }}</p>
            <button mat-raised-button color="primary" (click)="goToLogin()">
              Back to Login
            </button>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .callback-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: 20px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    .callback-card {
      width: 100%;
      max-width: 400px;
      text-align: center;
    }

    .loading-content, .error-content {
      padding: 40px 20px;
    }

    .loading-title, .error-title {
      margin: 20px 0 10px 0;
      color: #333;
    }

    .loading-message, .error-message {
      color: #666;
      margin-bottom: 20px;
    }

    .error-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
    }

    mat-spinner {
      margin: 0 auto;
    }
  `]
})
export class GoogleCallbackComponent implements OnInit {
  loadingMessage = 'Please wait while we complete your sign-in...';
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const code = params['code'];
      const error = params['error'];

      if (error) {
        this.handleError(error);
        return;
      }

      if (code) {
        this.processGoogleCallback(code);
      } else {
        this.handleError('No authorization code received');
      }
    });
  }

  private processGoogleCallback(code: string): void {
    this.loadingMessage = 'Exchanging authorization code...';

    this.authService.processGoogleCallback(code).subscribe({
      next: (response: LoginResponse) => {
        this.loadingMessage = 'Sign-in successful! Redirecting...';

        this.snackBar.open('Successfully signed in with Google!', 'Close', {
          duration: 3000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });

        // Small delay to show success message
        setTimeout(() => {
          this.router.navigate(['/dashboard']);
        }, 1000);
      },
      error: (error: HttpErrorResponse) => {
        this.handleError(this.getErrorMessage(error));
      }
    });
  }

  private handleError(error: string): void {
    this.errorMessage = error;
  }

  private getErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'Unable to connect to the server. Please check your internet connection.';
    }

    switch (error.status) {
      case 400:
        return 'Invalid authorization code. Please try signing in again.';
      case 401:
        return 'Authentication failed. Please try signing in again.';
      case 500:
      case 502:
      case 503:
      case 504:
        return 'Server is temporarily unavailable. Please try again in a few minutes.';
      default:
        if (error.error && error.error.message) {
          return error.error.message;
        }
        if (error.error && typeof error.error === 'string') {
          return error.error;
        }
        return 'Google sign-in failed. Please try again.';
    }
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}