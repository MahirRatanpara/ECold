import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    if (this.authService.isAuthenticated) {
      return true;
    } else {
      // Don't redirect if we're currently on the callback page or login page
      const currentUrl = this.router.url;
      if (!currentUrl.includes('/auth/') && !currentUrl.includes('/login')) {
        this.router.navigate(['/login']);
      }
      return false;
    }
  }
}