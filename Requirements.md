# ECold - System Requirements & Specifications

## 1. User Management & Authentication

### 1.1 OAuth Integration ✅
- Google OAuth 2.0 authentication with Gmail API access
- Microsoft OAuth 2.0 authentication with Outlook/Graph API access
- Secure token management with automatic refresh
- JWT-based session management
- User profile synchronization (name, email, profile picture)

### 1.2 User Profile
- View and manage user profile information
- Link multiple email accounts (Gmail, Outlook)
- Manage OAuth tokens and permissions
- Account settings and preferences

## 2. Recruiter Management

### 2.1 Recruiter Data ✅
- Import recruiters from CSV/Excel files
- Manual recruiter addition
- Bulk recruiter operations
- Required fields:
  - Email address (validated)
  - Company name
  - Job role/position
  - Recruiter name (optional)
  - LinkedIn profile (optional)
  - Custom notes

### 2.2 Template-Based Organization ✅
- Assign recruiters to email templates
- Filter recruiters by template
- View "All Recruiters" or template-specific views
- Template assignment during import or individual addition
- Automatic template progression (follow-up flow)

### 2.3 Week-wise Organization ✅
- Automatic grouping by assignment week
- Week-based subsections within templates
- Infinite scroll pagination
- Server-side pagination for performance
- Weekly analytics and tracking

### 2.4 Contact Status Tracking ✅
- Status types:
  - PENDING: Not yet contacted
  - CONTACTED: Email sent
  - RESPONDED: Received response
  - INTERESTED: Positive response
  - NOT_INTERESTED: Negative response
  - MOVED_TO_FOLLOWUP: Progressed to follow-up template
- Last contacted timestamp
- Response tracking and history

## 3. Email Template Management

### 3.1 Template Creation ✅
- Create and manage multiple email templates
- Template components:
  - Subject line with dynamic variables
  - Body content with rich text formatting
  - Dynamic variable placeholders
  - Resume attachment settings
  - Follow-up template linking

### 3.2 Template Variables ✅
- Supported placeholders:
  - `{RecruiterName}` - Recruiter's name
  - `{Company}` - Company name
  - `{Role}` - Job role/position
  - `{MyName}` - User's name
  - `{Date}` - Current date
  - Custom variables support

### 3.3 Template Types
- Initial outreach templates
- Follow-up templates (linked chain)
- Rejection response templates
- Thank you templates

## 4. Email Campaign Management

### 4.1 Bulk Email Sending ✅
- Week-based bulk email sending
- Send to all recruiters in specific week/template
- Automatic variable substitution
- Resume attachment support (PDF, DOC, DOCX)
- Email validation before sending
- Rate limiting and throttling

### 4.2 Scheduling & Automation ✅
- Gmail API scheduled send integration
- Batch email scheduling
- Daily/weekly send limits
- Time zone support
- Scheduled campaign management
- Automatic retry on failure

### 4.3 Email Delivery
- Real-time send status tracking
- Delivery confirmation
- Error handling and logging
- Failed email retry mechanism
- Bounce handling

## 5. Incoming Email Management

### 5.1 Email Scanning ✅
- Automatic inbox scanning (hourly)
- OAuth-based email access
- Multi-account support
- Background processing
- Real-time notifications

### 5.2 Email Classification ✅
- AI-powered categorization:
  - 🌟 **Interview Calls**: Shortlist, interview invitations
  - 📋 **Application Updates**: Status updates, acknowledgments
  - 🤝 **Recruiter Outreach**: New opportunities, connections
  - ❌ **Rejections**: Negative responses, closed positions
  - 📧 **General Inquiries**: Other job-related emails
- Confidence score for classifications
- Manual category override
- Keyword-based filtering

### 5.3 Domain Whitelisting ✅
- Trusted domains:
  - @naukri.com
  - @linkedin.com
  - @indeed.com
  - @glassdoor.com
  - Company-specific domains
- Custom domain management
- Spam filtering

### 5.4 Email Timeline ✅
- Chronological email history
- Thread grouping
- Application journey visualization
- Response rate analytics
- Communication history per recruiter

## 6. Dashboard & Analytics

### 6.1 Overview Dashboard ✅
- Total recruiters count
- Emails sent (today/week/month)
- Response rate statistics
- Pending emails count
- Recent activity feed
- Campaign performance metrics

### 6.2 Campaign Analytics
- Email open rates (if tracked)
- Click-through rates
- Response rate by template
- Best performing templates
- Time-based analytics
- Recruiter engagement metrics

### 6.3 Recruiter Insights
- Most responsive companies
- Response time analysis
- Success rate by job role
- Geographic distribution (if available)

## 7. File Management

### 7.1 Resume Upload ✅
- Support formats: PDF, DOC, DOCX
- File size limit: 5MB
- Multiple resume versions
- Resume preview
- Default resume selection
- Resume attachment to campaigns

### 7.2 Attachment Management
- Resume attachment to emails
- Automatic attachment to bulk sends
- Attachment validation
- Storage management
- Version control

## 8. Technical Requirements

### 8.1 Backend (Spring Boot) ✅
- Java 17 or higher
- Spring Boot 3.2.0
- PostgreSQL 15 database
- OAuth 2.0 security
- RESTful API design
- Swagger/OpenAPI documentation
- JUnit testing

### 8.2 Frontend (Angular) ✅
- Angular 17
- TypeScript 5.2
- Angular Material 17 UI
- Responsive design
- Progressive Web App (PWA) support
- State management with RxJS
- Lazy loading for performance

### 8.3 Database Schema ✅
- Users table with OAuth tokens
- Recruiter contacts with relationships
- Email templates with follow-up chains
- Email logs with tracking
- Incoming emails with classification
- Campaign management tables
- Analytics and metrics tables

### 8.4 Infrastructure ✅
- Docker containerization
- Docker Compose orchestration
- Development and production configs
- Environment-based configuration
- Health checks and monitoring
- Auto-restart policies
- Volume management for persistence

### 8.5 Security ✅
- OAuth 2.0 authentication
- JWT token-based sessions
- Token encryption at rest
- HTTPS/TLS support
- CORS configuration
- Rate limiting per user
- SQL injection prevention
- XSS protection

### 8.6 Performance ✅
- Database query optimization
- Connection pooling
- Lazy loading (frontend)
- Infinite scroll pagination
- Background job processing
- Async email operations

## 9. Integration Requirements

### 9.1 Gmail Integration ✅
- Gmail API for sending
- OAuth 2.0 authentication
- Scheduled send support
- Inbox scanning
- Thread management
- Label management

### 9.2 Microsoft Outlook Integration ✅
- Microsoft Graph API
- OAuth 2.0 authentication
- Email sending
- Inbox access
- Calendar integration (future)

### 9.3 Export/Import ✅
- CSV/Excel recruiter import
- Bulk data export
- Campaign data export
- Analytics export
- Backup/restore functionality

## 10. Operational Requirements

### 10.1 Logging & Monitoring ✅
- Application logs (SLF4J + Logback)
- Email send/receive logs
- Error tracking and alerts
- Performance metrics (Spring Actuator)
- Database query logging
- API request logging

### 10.2 Error Handling ✅
- Graceful error handling
- User-friendly error messages
- Automatic retry mechanisms
- Failed email queue
- Circuit breaker patterns
- Fallback strategies

### 10.3 Scalability
- Horizontal scaling support
- Load balancing ready
- Database replication support
- Microservices architecture (future)
- Message queue integration (future)

## 11. Future Enhancements (Planned)

### 11.1 AI/ML Features
- Smart email personalization
- Response prediction
- Best time to send analysis
- Template recommendation
- Sentiment analysis

### 11.2 Advanced Features
- WhatsApp/LinkedIn integration
- Chrome/Outlook browser extension
- Mobile applications (iOS/Android)
- Team collaboration features
- Multi-language support
- Voice recording attachments

### 11.3 Enterprise Features
- Multi-tenant support
- Role-based access control
- Team workspaces
- Advanced analytics
- Custom branding
- API marketplace

## 12. Compliance & Data Privacy

### 12.1 Data Protection
- GDPR compliance ready
- Data encryption at rest and in transit
- User data export/delete
- Privacy policy implementation
- Cookie consent management
- Data retention policies

### 12.2 Email Compliance
- CAN-SPAM compliance
- Unsubscribe mechanism
- Email preferences management
- Sender reputation management
- SPF/DKIM/DMARC support

---

## Current Implementation Status

✅ **Completed Features:**
- OAuth authentication (Google & Microsoft)
- Template-based recruiter management
- Week-wise organization with infinite scroll
- Bulk email sending with Gmail API
- Follow-up template automation
- Incoming email classification
- Dashboard with analytics
- Resume attachment support
- Docker containerization
- Production-ready deployment

🚧 **In Progress:**
- Advanced analytics dashboard
- Email open/click tracking
- Multi-language support

📋 **Planned:**
- Mobile applications
- Browser extensions
- WhatsApp/LinkedIn integration
- Team collaboration features
- Enterprise features
