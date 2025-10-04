# ECold Email Configuration Guide

## 📧 Email Service Setup & Configuration

This guide will help you set up email sending capabilities in your ECold application. You can choose from multiple email providers.

---

## 🚀 Quick Setup Options

### Option 1: Gmail SMTP (Recommended for Personal Use)
### Option 2: Gmail API (Advanced - Better for Production)
### Option 3: Microsoft Outlook/Office 365
### Option 4: Custom SMTP Server

---

## 📋 Prerequisites

1. **ECold Application** running (Backend + Frontend)
2. **Email Provider Account** (Gmail, Outlook, etc.)
3. **Application-specific passwords** or **OAuth setup**

---

## 🔧 Setup Instructions

### 1. Gmail SMTP Configuration

#### Step 1: Enable 2-Factor Authentication
1. Go to [Google Account Settings](https://myaccount.google.com/)
2. Click **Security** → **2-Step Verification**
3. Follow the setup process

#### Step 2: Generate App Password
1. Go to **Security** → **App passwords**
2. Select **Mail** and **Other (Custom name)**
3. Enter "ECold Application"
4. **Copy the generated 16-character password**

#### Step 3: Update Application Properties
Add these configurations to `backend/src/main/resources/application.properties`:

```properties
# Email Configuration
app.email.enabled=true
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password-here
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=10000
spring.mail.properties.mail.smtp.timeout=10000
spring.mail.properties.mail.smtp.writetimeout=10000

# Email Display Settings
app.email.from-name=ECold Application
```

#### Step 4: Environment Variables (Recommended)
For better security, use environment variables instead:

```bash
# Windows
set SPRING_MAIL_USERNAME=your-email@gmail.com
set SPRING_MAIL_PASSWORD=your-app-password-here

# Linux/Mac
export SPRING_MAIL_USERNAME=your-email@gmail.com
export SPRING_MAIL_PASSWORD=your-app-password-here
```

Update `application.properties`:
```properties
spring.mail.username=${SPRING_MAIL_USERNAME}
spring.mail.password=${SPRING_MAIL_PASSWORD}
```

---

### 2. Gmail API Configuration (Advanced)

#### Step 1: Create Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable **Gmail API**

#### Step 2: Create OAuth 2.0 Credentials
1. Go to **APIs & Services** → **Credentials**
2. Click **Create Credentials** → **OAuth 2.0 Client IDs**
3. Application type: **Web application**
4. Add authorized redirect URIs:
   - `http://localhost:8080/oauth2/callback/google`
   - `https://yourdomain.com/oauth2/callback/google`

#### Step 3: Configure Application
```properties
# Gmail API Configuration
spring.security.oauth2.client.registration.google.client-id=your-client-id
spring.security.oauth2.client.registration.google.client-secret=your-client-secret
spring.security.oauth2.client.registration.google.scope=openid,profile,email,https://www.googleapis.com/auth/gmail.send
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/oauth2/callback/{registrationId}
spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.google.com/o/oauth2/auth
spring.security.oauth2.client.provider.google.token-uri=https://oauth2.googleapis.com/token
```

---

### 3. Microsoft Outlook/Office 365 Configuration

#### Step 1: Get Outlook Credentials
1. Go to [Microsoft 365 Admin Center](https://admin.microsoft.com/)
2. Or use personal Outlook.com account

#### Step 2: Configure SMTP Settings
```properties
# Outlook SMTP Configuration
app.email.enabled=true
spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587
spring.mail.username=your-email@outlook.com
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.email.from-name=ECold Application
```

---

### 4. Custom SMTP Server Configuration

```properties
# Custom SMTP Configuration
app.email.enabled=true
spring.mail.host=your-smtp-server.com
spring.mail.port=587
spring.mail.username=your-username
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.email.from-name=Your Company Name
```

---

## 🧪 Testing Email Configuration

### 1. Backend API Testing

#### Test Email Configuration Status
```bash
curl -X GET "http://localhost:8080/api/emails/config/status" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Send Test Email
```bash
curl -X POST "http://localhost:8080/api/emails/test?toEmail=test@example.com" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

#### Test Email Configuration
```bash
curl -X GET "http://localhost:8080/api/emails/config/test" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 2. Frontend Testing

1. **Open ECold Application** in browser
2. **Login** to your account
3. Go to **Settings** or **Email Configuration** (if implemented)
4. Click **"Test Email Configuration"**
5. Check your email inbox for test message

### 3. Template Email Testing

1. **Create an active email template**
2. **Go to Recruiters page**
3. **Click "Send Email"** for any recruiter
4. **Select your template**
5. **Click "Send Email"**
6. **Verify email was sent** (check sent folder or recipient)

---

## 🔍 API Reference

### Send Direct Email
```typescript
POST /api/emails/send
Content-Type: application/json
Authorization: Bearer YOUR_TOKEN

{
  "to": "recipient@example.com",
  "subject": "Subject Here",
  "body": "Email body content",
  "isHtml": false,
  "priority": "NORMAL"
}
```

### Send Template Email
```typescript
POST /api/emails/send-template?templateId=1&recruiterId=1
Content-Type: application/json
Authorization: Bearer YOUR_TOKEN

{
  "MyName": "John Doe",
  "customField": "Custom Value"
}
```

---

## 🛠️ Using the Email Feature

### 1. From Recruiter List
1. Navigate to **Recruiters** page
2. Click **"More Actions"** (⋮) for any recruiter
3. Select **"Send Email"**
4. Choose template or write custom email
5. Click **"Send Email"** button
6. Email will be sent directly from the application

### 2. From Recruiter Details
1. Click **"View Details"** for any recruiter
2. Click **"Send Email"** in quick actions
3. Compose your email
4. Send directly from the app

### 3. Using Templates
1. **Create templates** in Templates section
2. **Set status to "Active"**
3. When composing emails, **select template**
4. **Placeholders will auto-populate**:
   - `{Company}` → Recruiter's company
   - `{Role}` → Job role
   - `{RecruiterName}` → Recruiter's name
   - `{MyName}` → Your name

---

## 🔧 Troubleshooting

### Common Issues & Solutions

#### 1. "Email configuration not set up"
**Solution**: Check `application.properties` has correct email settings

#### 2. "Authentication failed"
**Solutions**:
- **Gmail**: Use App Password, not regular password
- **Outlook**: Enable "Less secure app access"
- Check username/password are correct

#### 3. "Connection timeout"
**Solutions**:
- Check internet connection
- Verify SMTP server and port
- Check firewall settings

#### 4. "Email sending disabled"
**Solution**: Set `app.email.enabled=true` in properties

#### 5. "SSL/TLS errors"
**Solutions**:
```properties
spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com
spring.mail.properties.mail.smtp.ssl.enable=true
```

### Debug Mode
Enable debug logging:
```properties
logging.level.org.springframework.mail=DEBUG
logging.level.com.ecold.service.EmailService=DEBUG
```

---

## ⚡ Performance Optimization

### 1. Async Email Sending
```properties
# Enable async processing
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=4
```

### 2. Connection Pooling
```properties
spring.mail.properties.mail.smtp.connectionpoolsize=10
spring.mail.properties.mail.smtp.connectionpooltimeout=300000
```

### 3. Rate Limiting
```properties
# Limit emails per minute
app.email.rate-limit.emails-per-minute=30
```

---

## 🔐 Security Best Practices

### 1. Use Environment Variables
```bash
# Never commit passwords to code
export SPRING_MAIL_PASSWORD=your-secure-password
```

### 2. Enable OAuth (Recommended)
- Use OAuth 2.0 instead of passwords
- More secure and doesn't expose credentials

### 3. Use HTTPS in Production
```properties
server.ssl.enabled=true
server.port=8443
```

### 4. Monitor Email Usage
- Track email sending patterns
- Implement rate limiting
- Monitor for abuse

---

## 📊 Monitoring & Analytics

### Email Metrics Available
- **Emails sent per day**
- **Success/failure rates**
- **Template usage statistics**
- **Response tracking** (if implemented)
- **Recruiter contact status**

### Logging
Check application logs for:
```
INFO  - Email sent successfully to john@company.com with messageId: ecold-uuid
ERROR - Failed to send email: Authentication failed
```

---

## 🚀 Production Deployment

### 1. Environment Configuration
```properties
# Production settings
app.email.enabled=true
spring.profiles.active=production
server.port=8080

# Use external configuration
spring.config.import=file:/opt/ecold/application-production.properties
```

### 2. Docker Configuration
```dockerfile
# In Dockerfile
ENV SPRING_MAIL_USERNAME=${MAIL_USERNAME}
ENV SPRING_MAIL_PASSWORD=${MAIL_PASSWORD}
ENV APP_EMAIL_ENABLED=true
```

### 3. Kubernetes Secrets
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: email-config
type: Opaque
stringData:
  mail-username: your-email@gmail.com
  mail-password: your-app-password
```

---

## 📞 Support

If you encounter issues:

1. **Check application logs** in `logs/` directory
2. **Verify email provider settings**
3. **Test with simple SMTP tool** first
4. **Check firewall/network settings**
5. **Review this documentation** for common solutions

### Testing Commands Summary
```bash
# Test configuration
curl -X GET "http://localhost:8080/api/emails/config/status"

# Send test email
curl -X POST "http://localhost:8080/api/emails/test?toEmail=test@example.com"

# Check application health
curl -X GET "http://localhost:8080/actuator/health"
```

---

🎉 **Congratulations!** Your ECold application can now send emails directly without opening external email clients, providing a seamless user experience for your job search outreach campaigns.
