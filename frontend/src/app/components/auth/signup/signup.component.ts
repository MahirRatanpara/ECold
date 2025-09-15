import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../services/auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss']
})
export class SignupComponent implements OnInit {
  signupForm!: FormGroup;
  hidePassword = true;
  hideConfirmPassword = true;
  isLoading = false;
  errorMessage = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  private initializeForm(): void {
    this.signupForm = this.formBuilder.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8), this.passwordValidator]],
      confirmPassword: ['', [Validators.required]],
      agreeToTerms: [false, [Validators.requiredTrue]]
    }, { validators: this.passwordMatchValidator });
  }

  private passwordValidator(control: AbstractControl): { [key: string]: any } | null {
    const password = control.value;
    if (!password) return null;

    const hasUpperCase = /[A-Z]/.test(password);
    const hasLowerCase = /[a-z]/.test(password);
    const hasNumber = /\d/.test(password);
    const hasSpecialChar = /[!@#$%^&*(),.?\":{}|<>]/.test(password);

    const valid = hasUpperCase && hasLowerCase && hasNumber && hasSpecialChar;
    if (!valid) {
      return {
        passwordStrength: {
          hasUpperCase,
          hasLowerCase,
          hasNumber,
          hasSpecialChar
        }
      };
    }
    return null;
  }

  private passwordMatchValidator(form: AbstractControl): { [key: string]: any } | null {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      return { passwordMismatch: true };
    }
    return null;
  }

  onSubmit(): void {
    if (this.signupForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      
      const userData = {
        firstName: this.signupForm.value.firstName,
        lastName: this.signupForm.value.lastName,
        email: this.signupForm.value.email,
        password: this.signupForm.value.password
      };

      this.authService.signup(userData).subscribe({
        next: (response: any) => {
          this.isLoading = false;
          this.snackBar.open('Account created successfully! Please check your email to verify your account.', 'Close', {
            duration: 5000,
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
          this.router.navigate(['/login']);
        },
        error: (error: HttpErrorResponse) => {
          this.isLoading = false;
          this.errorMessage = this.getErrorMessage(error);
        }
      });
    } else {
      this.markFormGroupTouched();
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.signupForm.controls).forEach(key => {
      const control = this.signupForm.get(key);
      control?.markAsTouched();
    });
  }

  getPasswordStrengthErrors(): string[] {
    const passwordControl = this.signupForm.get('password');
    if (passwordControl?.hasError('passwordStrength')) {
      const errors = passwordControl.errors!['passwordStrength'];
      const messages: string[] = [];
      
      if (!errors.hasUpperCase) messages.push('One uppercase letter');
      if (!errors.hasLowerCase) messages.push('One lowercase letter');
      if (!errors.hasNumber) messages.push('One number');
      if (!errors.hasSpecialChar) messages.push('One special character');
      
      return messages;
    }
    return [];
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  private getErrorMessage(error: HttpErrorResponse): string {
    // Handle different error scenarios with user-friendly messages
    if (error.status === 0) {
      return 'Unable to connect to the server. Please check your internet connection.';
    }
    
    switch (error.status) {
      case 400:
        return 'Invalid input data. Please check your information and try again.';
      case 409:
        return 'An account with this email already exists. Please use a different email or try logging in.';
      case 422:
        return 'Please ensure all fields are filled correctly.';
      case 429:
        return 'Too many registration attempts. Please wait a few minutes before trying again.';
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
        return 'Registration failed. Please check your information and try again.';
    }
  }
}