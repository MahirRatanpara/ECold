# Google OAuth Setup Guide for ECold Application

This guide will walk you through setting up Google OAuth authentication for the ECold application, enabling users to sign in with their Google accounts and send emails through their Gmail accounts.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Google Cloud Console Setup](#google-cloud-console-setup)
3. [Backend Configuration](#backend-configuration)
4. [Frontend Configuration](#frontend-configuration)
5. [Environment Variables](#environment-variables)
6. [Testing the Setup](#testing-the-setup)
7. [Troubleshooting](#troubleshooting)
8. [Security Considerations](#security-considerations)

## Prerequisites

Before starting, ensure you have:

- A Google account with access to Google Cloud Console
- Admin access to your ECold application
- Basic understanding of OAuth 2.0 flow
- Node.js and Angular CLI installed for frontend development
- Java 17+ and Maven for backend development

## Google Cloud Console Setup

### Step 1: Create a Google Cloud Project

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Click on the project dropdown at the top
3. Click "New Project"
4. Enter project name: `ECold-OAuth`
5. Click "Create"

### Step 2: Enable Required APIs

1. Navigate to "APIs & Services" > "Library"
2. Search for and enable the following APIs:
   - **Gmail API** - For sending emails through user's Gmail
   - **Google+ API** - For user profile information
   - **People API** - For accessing user profile data

### Step 3: Configure OAuth Consent Screen

1. Go to "APIs & Services" > "OAuth consent screen"
2. Choose "External" user type (unless you have a Google Workspace)
3. Fill in the required information:
   - **App name**: ECold Application
   - **User support email**: Your email address
   - **App logo**: Upload your app logo (optional)
   - **App domain**: Your domain (e.g., `localhost:4200` for development)
   - **Authorized domains**: Add your domains:
     - `localhost` (for development)
     - Your production domain
   - **Developer contact information**: Your email address

4. **Scopes**: Add the following scopes:
   - `openid`
   - `profile`
   - `email`
   - `https://www.googleapis.com/auth/gmail.readonly`
   - `https://www.googleapis.com/auth/gmail.send`

5. **Test users** (for development): Add email addresses that can test the app

### Step 4: Create OAuth 2.0 Credentials

1. Go to "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "OAuth 2.0 Client IDs"
3. Choose "Web application"
4. Configure the client:
   - **Name**: ECold Web Client
   - **Authorized JavaScript origins**:
     - `http://localhost:4200` (development)
     - `https://yourdomain.com` (production)
   - **Authorized redirect URIs**:
     - `http://localhost:4200/auth/google/callback` (development)
     - `https://yourdomain.com/auth/google/callback` (production)

5. Click "Create"
6. **Important**: Save the Client ID and Client Secret securely

## Backend Configuration

### Step 1: Update Application Configuration

The OAuth configuration is already set up in `backend/src/main/resources/application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:}
            client-secret: ${GOOGLE_CLIENT_SECRET:}
            scope:
              - openid
              - profile
              - email
              - https://www.googleapis.com/auth/gmail.readonly
              - https://www.googleapis.com/auth/gmail.send
            redirect-uri: ${GOOGLE_REDIRECT_URI:http://localhost:4200/auth/google/callback}
```

### Step 2: Key Backend Components

The following components have been implemented:

#### OAuth Configuration (`OAuthConfig.java`)
- Configures Spring Security OAuth2 client registration
- Sets up Google OAuth2 provider details

#### Google OAuth Service (`GoogleOAuthServiceImpl.java`)
- Handles OAuth authorization URL generation
- Processes OAuth callback with authorization code
- Manages token refresh functionality

#### Gmail OAuth Email Service (`GmailOAuthServiceImpl.java`)
- Sends emails using authenticated user's Gmail account
- Handles Gmail API integration
- Manages token expiration and refresh

#### Enhanced Email Service (`EmailServiceImpl.java`)
- Automatically detects if user is authenticated with Google
- Routes emails through Gmail OAuth when available
- Falls back to SMTP for non-OAuth users

## Frontend Configuration

### Step 1: OAuth Components

#### Login Component Updates
- Added "Continue with Google" button
- Redirects to OAuth authorization URL
- Handles loading states

#### Signup Component Updates
- Added "Sign up with Google" button
- Shares OAuth flow with login

#### Google Callback Component
- Handles OAuth callback from Google
- Processes authorization code
- Manages error states and user feedback
- Redirects to dashboard on success

### Step 2: Auth Service Updates

The `AuthService` includes methods for:
- `getGoogleAuthUrl()` - Gets OAuth authorization URL
- `processGoogleCallback(code)` - Exchanges code for tokens

### Step 3: Routing Configuration

Added OAuth callback route in `app-routing.module.ts`:
```typescript
{ path: 'auth/google/callback', component: GoogleCallbackComponent }
```

## Environment Variables

### Backend Environment Variables

Create a `.env` file in the backend directory or set system environment variables:

```bash
# Google OAuth Configuration
GOOGLE_CLIENT_ID=your_google_client_id_here
GOOGLE_CLIENT_SECRET=your_google_client_secret_here
GOOGLE_REDIRECT_URI=http://localhost:4200/auth/google/callback

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/ecold
DATABASE_USERNAME=ecold_user
DATABASE_PASSWORD=ecold_pass123

# JWT Configuration
JWT_SECRET=your_jwt_secret_here
JWT_EXPIRATION=86400000

# Email Configuration (fallback SMTP)
EMAIL_ENABLED=true
EMAIL_FROM_NAME=ECold Application
```

### Frontend Environment Variables

Update `frontend/src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

For production, update `environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://yourapi.com/api'
};
```

## Testing the Setup

### Step 1: Start the Application

1. **Backend**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **Frontend**:
   ```bash
   cd frontend
   npm install
   ng serve
   ```

### Step 2: Test OAuth Flow

1. Navigate to `http://localhost:4200/login`
2. Click "Continue with Google"
3. Should redirect to Google OAuth consent screen
4. Grant permissions for:
   - View your email address
   - View your basic profile info
   - Read your Gmail messages
   - Send email on your behalf
5. Should redirect back to application dashboard

### Step 3: Test Email Functionality

1. Go to the email templates or recruiter sections
2. Try sending an email
3. Verify the email is sent from your Gmail account
4. Check Gmail "Sent" folder for confirmation

## Troubleshooting

### Common Issues and Solutions

#### 1. "Error 400: redirect_uri_mismatch"
- **Cause**: Redirect URI in Google Console doesn't match the one in your app
- **Solution**: Ensure exact match including protocol (http/https) and port

#### 2. "Error 403: access_denied"
- **Cause**: User denied permissions or app not verified
- **Solution**: Ensure all required scopes are properly configured in Google Console

#### 3. "Invalid client_id or client_secret"
- **Cause**: Incorrect credentials in environment variables
- **Solution**: Double-check credentials from Google Console

#### 4. "Gmail API quota exceeded"
- **Cause**: Too many API calls
- **Solution**: Check Google Console quotas and implement rate limiting

#### 5. "Token expired" errors
- **Cause**: Access token has expired
- **Solution**: The app automatically refreshes tokens, but check refresh token logic

### Debug Mode

Enable debug logging in `application.yml`:

```yaml
logging:
  level:
    com.ecold: DEBUG
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: DEBUG
```

## Security Considerations

### 1. Client Secret Protection
- Never commit client secrets to version control
- Use environment variables or secure secret management
- Rotate secrets periodically

### 2. Scope Limitation
- Only request necessary OAuth scopes
- Review and minimize permissions regularly

### 3. Token Storage
- Access tokens are stored securely in the database
- Refresh tokens are encrypted
- Implement token expiration handling

### 4. HTTPS in Production
- Always use HTTPS in production
- Update redirect URIs to use HTTPS
- Secure cookie settings for token storage

### 5. Rate Limiting
- Implement rate limiting for OAuth endpoints
- Monitor API usage in Google Console
- Set up alerts for unusual activity

## Production Deployment

### 1. Google Console Updates
- Add production domains to authorized origins
- Update redirect URIs for production URLs
- Submit app for verification if needed

### 2. Environment Configuration
- Set production environment variables
- Use secure secret management (AWS Secrets Manager, etc.)
- Configure proper CORS settings

### 3. Monitoring
- Set up logging for OAuth events
- Monitor token refresh rates
- Track email sending metrics

## Support and Resources

### Google Documentation
- [Google OAuth 2.0 Guide](https://developers.google.com/identity/protocols/oauth2)
- [Gmail API Documentation](https://developers.google.com/gmail/api)
- [Google Cloud Console](https://console.cloud.google.com/)

### Spring Security OAuth2
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html)

### ECold Application
- Backend API runs on port 8080
- Frontend runs on port 4200
- Database runs on port 5432 (PostgreSQL)

---

**Note**: This setup enables users to authenticate with Google and send emails through their Gmail accounts. The application automatically detects OAuth-authenticated users and routes their emails through Gmail API instead of SMTP, providing a seamless email sending experience using their own email addresses.