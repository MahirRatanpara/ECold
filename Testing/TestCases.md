# ECold Application Test Cases Document

## Application Changes Notice

**Removed Features (as of latest version):**

- Email Campaign Management (bulk email campaigns)
- Export CSV functionality from recruiters page
- Add Note functionality for recruiters
- Campaign-related test cases have been removed from this document

**Updated Features:**

- Recruiters are now user-specific (each user sees only their own recruiters)
- Templates are now user-specific (each user sees only their own templates)
- Dashboard statistics are now user-specific
- Enhanced infinite scrolling with automatic loading
- Backend search filtering across entire dataset

## Test Environment Setup

- **Backend**: Spring Boot application running on default port (8080)
- **Frontend**: Angular application running on default port (4200)
- **Database**: H2/PostgreSQL with test data
- **Email Service**: Gmail OAuth integration configured

## [passed] 1. Authentication & User Management Test Cases

### [passed] TC-AUTH-001: User Registration

**Objective**: Verify user can successfully register with valid credentials
**Prerequisites**: Application is running, no existing user with test email
**Test Data**:

- Name: "Test User"
- Email: "testuser@example.com"
- Password: "Password123!"

**Test Steps**:

1. Navigate to signup page
2. Enter valid name, email, and password
3. Click "Sign Up" button
4. Verify success message appears
5. Verify user is redirected to login page

**Expected Result**: User account created successfully, redirect to login

### [passed] TC-AUTH-002: User Login with Valid Credentials

**Objective**: Verify user can login with correct credentials
**Prerequisites**: User account exists in system
**Test Data**: Registered user credentials

**Test Steps**:

1. Navigate to login page
2. Enter valid email and password
3. Click "Login" button
4. Verify user is redirected to dashboard
5. Verify that you are able to logout using "Logout Button"

**Expected Result**: Successful login, redirect to dashboard with JWT token

### [passes] TC-AUTH-003: User Login with Invalid Credentials

**Objective**: Verify login fails with incorrect credentials
**Test Data**:

- Email: "testuser@example.com"
- Password: "WrongPassword"

**Test Steps**:

1. Navigate to login page
2. Enter invalid credentials
3. Click "Login" button
4. Verify invalid password message displayed

**Expected Result**: Login fails, error message shown

### [passed] TC-AUTH-004: Protected Route Access Without Authentication

**Objective**: Verify unauthenticated users cannot access protected routes
**Test Steps**:

1. Clear browser storage/cookies
2. Navigate directly to "/dashboard"
3. Verify redirect to login page

**Expected Result**: Redirect to login page

## [passed] 2. Recruiter Management Test Cases

### [passed] TC-REC-001: Upload Recruiter CSV File

**Objective**: Verify successful upload and parsing of recruiter data
**Prerequisites**: User logged in, CSV file prepared
**Test Data**: CSV with columns: email, company, role, recruiterName

**Test Steps**:

1. Navigate to recruiter management page
2. Click "import CSV" button
3. Select valid CSV file
4. Click "Import" button
5. Verify success message and data appears in list

**Expected Result**: CSV data imported successfully, recruiters visible in list

### [passed] TC-REC-002: Upload Invalid CSV Format

**Objective**: Verify system handles invalid CSV format gracefully
**Test Data**: CSV with missing required columns

**Test Steps**:

1. Navigate to recruiter management page
2. Upload invalid CSV file
3. Verify error message displayed

**Expected Result**: Error message indicating invalid format

### [passed] TC-REC-003: View Recruiter List

**Objective**: Verify recruiter list displays correctly
**Prerequisites**: Recruiters exist in database

**Test Steps**:

1. Navigate to recruiter list page
2. Verify all recruiters are displayed
3. Verify pagination works (if implemented)
4. Verify search functionality (if implemented)

**Expected Result**: All recruiters displayed with correct information

### [passes] TC-REC-004: Edit Recruiter Information

**Objective**: Verify recruiter details can be modified
**Prerequisites**: Recruiter exists in system

**Test Steps**:

1. Navigate to recruiter list
2. Click view on a recruiter to open view dialog
3. Click edit button in view dialog
4. Modify details in edit dialog
5. Save changes
6. Verify updates are reflected

**Expected Result**: Recruiter information updated successfully

### [passed] TC-REC-005: Delete Recruiter

**Objective**: Verify recruiter can be removed from system
**Prerequisites**: Recruiter exists in system

**Test Steps**:

1. Navigate to recruiter list
2. Click delete on a recruiter
3. Confirm deletion
4. Verify recruiter removed from list

**Expected Result**: Recruiter deleted successfully

## 3. Email Template Management Test Cases

### [passed] TC-TEMP-001: Create Email Template

**Objective**: Verify user can create custom email templates
**Test Data**:

- Subject: "Application for {Role} at {Company}"
- Body: "Dear {RecruiterName}, I am interested in the {Role} position..."

**Test Steps**:

1. Navigate to email templates page
2. Click "Create Template"
3. Enter subject with placeholders
4. Enter body with placeholders
5. Save template
6. Verify template appears in list

**Expected Result**: Template created successfully with placeholders

### [passed] TC-TEMP-002: Edit Email Template

**Objective**: Verify existing templates can be modified
**Prerequisites**: Template exists in system

**Test Steps**:

1. Navigate to templates list
2. Select template to edit
3. Modify subject/body
4. Save changes
5. Verify updates reflected

**Expected Result**: Template updated successfully

### [passed] TC-TEMP-003: Delete Email Template

**Objective**: Verify templates can be removed
**Prerequisites**: Template exists in system

**Test Steps**:

1. Navigate to templates list
2. Click delete on template
3. Confirm deletion
4. Verify template removed

**Expected Result**: Template deleted successfully

### [failed] TC-TEMP-004: Template Placeholder Validation

**Objective**: Verify placeholders are properly validated
**Test Data**: Template with invalid placeholders

**Test Steps**:

1. Create template with invalid placeholder syntax
2. Attempt to save
3. Verify validation error

**Expected Result**: Validation error for invalid placeholders

## 4. Direct Email Communication Test Cases

### ☐ TC-EMAIL-001: Send Email to Individual Recruiter

**Objective**: Verify user can send email directly to a recruiter
**Prerequisites**: Recruiter exists in system

**Test Steps**:

1. Navigate to recruiter list
2. Click on a recruiter's "More Actions" menu
3. Select "Send Email"
4. Verify email client opens with pre-filled data
5. Send email manually
6. Verify recruiter status updates to "CONTACTED"

**Expected Result**: Email client opens correctly, status updates properly

### ☐ TC-EMAIL-002: Email Auto-population

**Objective**: Verify email pre-fills with recruiter and company information
**Prerequisites**: Recruiter with complete information

**Test Steps**:

1. Select recruiter with company name and role
2. Click "Send Email"
3. Verify subject line includes company name
4. Verify body includes recruiter name and greeting

**Expected Result**: Email is properly pre-populated with dynamic content

## 5. User Data Isolation Test Cases

### ☐ TC-USER-001: User-Specific Recruiter Data

**Objective**: Verify each user only sees their own recruiters
**Prerequisites**: Multiple users with recruiter data

**Test Steps**:

1. User A logs in and adds recruiters
2. User B logs in with different account
3. Navigate to recruiter list
4. Verify User B cannot see User A's recruiters
5. User B adds their own recruiters
6. User A logs back in
7. Verify User A only sees their recruiters

**Expected Result**: Complete data isolation between users

### ☐ TC-USER-002: User-Specific Templates

**Objective**: Verify each user only sees their own email templates
**Prerequisites**: Multiple users with template data

**Test Steps**:

1. User A creates email templates
2. User B logs in with different account
3. Navigate to templates page
4. Verify User B cannot see User A's templates
5. User B creates their own templates
6. User A logs back in
7. Verify User A only sees their templates

**Expected Result**: Template data is isolated per user

### ☐ TC-USER-003: User-Specific Statistics

**Objective**: Verify dashboard statistics are user-specific
**Prerequisites**: Multiple users with different data

**Test Steps**:

1. User A has 10 recruiters with various statuses
2. User B has 5 recruiters with different statuses
3. User A logs in and checks dashboard stats
4. Note statistics (total recruiters, pending, contacted, etc.)
5. User B logs in and checks dashboard stats
6. Verify User B's stats don't include User A's data

**Expected Result**: Dashboard shows user-specific statistics only

## 6. Resume Management Test Cases

### ☐ TC-RES-001: Upload Resume File

**Objective**: Verify resume can be uploaded and stored
**Test Data**: Valid PDF/DOC resume file

**Test Steps**:

1. Navigate to resume management
2. Click "Upload Resume"
3. Select valid resume file
4. Upload file
5. Verify file appears in list

**Expected Result**: Resume uploaded and stored successfully

### ☐ TC-RES-002: Upload Invalid File Format

**Objective**: Verify system rejects invalid file formats
**Test Data**: Image file (.jpg) instead of resume

**Test Steps**:

1. Attempt to upload invalid file format
2. Verify error message displayed

**Expected Result**: Error message about invalid file format

### ☐ TC-RES-003: Set Default Resume

**Objective**: Verify user can set default resume for campaigns
**Prerequisites**: Multiple resumes uploaded

**Test Steps**:

1. Navigate to resume list
2. Select resume to set as default
3. Click "Set as Default"
4. Verify default status updated

**Expected Result**: Default resume set successfully

### ☐ TC-RES-004: Resume Auto-attachment in Emails

**Objective**: Verify resume is automatically attached to campaign emails
**Prerequisites**: Default resume set, campaign created

**Test Steps**:

1. Execute email campaign
2. Check sent emails
3. Verify resume is attached

**Expected Result**: Resume attached to all emails

## 7. Incoming Email Tracking Test Cases

### ☐ TC-IN-001: Gmail OAuth Integration

**Objective**: Verify Gmail OAuth connection works
**Prerequisites**: Gmail account with test emails

**Test Steps**:

1. Navigate to email integration settings
2. Click "Connect Gmail"
3. Complete OAuth flow
4. Verify connection successful

**Expected Result**: Gmail account connected successfully

### ☐ TC-IN-002: Scan Incoming Emails

**Objective**: Verify system scans and categorizes incoming emails
**Prerequisites**: Gmail connected, job-related emails in inbox

**Test Steps**:

1. Trigger email scan manually or wait for scheduled scan
2. Navigate to incoming emails page
3. Verify job-related emails are detected
4. Verify emails are properly categorized

**Expected Result**: Job-related emails detected and categorized

### ☐ TC-IN-003: Email Categorization

**Objective**: Verify emails are correctly categorized
**Test Data**: Emails with keywords: "interview", "shortlisted", "rejected", "application"

**Test Steps**:

1. Prepare test emails with different keywords
2. Run email scan
3. Verify categorization:
   - Application Updates
   - Interview Calls
   - Rejections
   - Recruiter Outreach

**Expected Result**: Emails categorized correctly based on content

### ☐ TC-IN-004: Domain Whitelist

**Objective**: Verify domain filtering works correctly
**Test Data**: Emails from @naukri.com, @linkedin.com, @indeed.com

**Test Steps**:

1. Configure domain whitelist
2. Run email scan
3. Verify only whitelisted domains are processed

**Expected Result**: Only emails from whitelisted domains processed

### ☐ TC-IN-005: Email Timeline View

**Objective**: Verify timeline shows job application progress
**Prerequisites**: Multiple related emails processed

**Test Steps**:

1. Navigate to email timeline
2. Verify chronological order
3. Verify application status progression

**Expected Result**: Timeline shows clear progression of applications

## 8. Dashboard & Analytics Test Cases

### ☐ TC-DASH-001: Dashboard Overview

**Objective**: Verify dashboard displays key metrics
**Prerequisites**: System has campaign and email data

**Test Steps**:

1. Login and navigate to dashboard
2. Verify display of:
   - Total emails sent
   - Pending campaigns
   - Recent incoming emails
   - Success/failure rates

**Expected Result**: Dashboard shows accurate metrics

### ☐ TC-DASH-002: Email Statistics

**Objective**: Verify email statistics are calculated correctly
**Prerequisites**: Campaigns executed with known results

**Test Steps**:

1. Navigate to email statistics
2. Verify statistics match actual sent/failed emails
3. Check date range filters work

**Expected Result**: Statistics are accurate and filterable

### ☐ TC-DASH-003: Export Data Functionality

**Objective**: Verify data can be exported to CSV/Excel
**Prerequisites**: Data exists in system

**Test Steps**:

1. Navigate to export section
2. Select data type and date range
3. Click export
4. Verify file is generated and downloadable

**Expected Result**: Data exported successfully in correct format

## 9. System Integration Test Cases

### ☐ TC-INT-001: End-to-End User Workflow

**Objective**: Verify complete user workflow from registration to email sending
**Prerequisites**: Clean system state

**Test Steps**:

1. Register new user
2. Upload recruiter list via CSV
3. Upload resume (if feature exists)
4. Create email template
5. Send emails to individual recruiters
6. Verify recruiters' status updates
7. Check incoming email processing

**Expected Result**: Complete workflow executes successfully for user

### ☐ TC-INT-002: Gmail Integration End-to-End

**Objective**: Verify Gmail integration works end-to-end
**Prerequisites**: Gmail account setup

**Test Steps**:

1. Connect Gmail account
2. Send test email to recruiter directly
3. Reply to recruiter email from Gmail
4. Verify reply is detected and categorized

**Expected Result**: Full Gmail integration works correctly

### ☐ TC-INT-003: Error Handling and Recovery

**Objective**: Verify system handles errors gracefully
**Test Scenarios**:

- Network interruption during email sending
- Gmail API rate limits
- Invalid email addresses
- File upload failures

**Test Steps**:

1. Simulate various error conditions
2. Verify appropriate error messages
3. Verify system recovery
4. Check error logging

**Expected Result**: System handles errors gracefully with proper logging

## 10. Performance Test Cases

### ☐ TC-PERF-001: Bulk Data Import Performance

**Objective**: Verify system can handle large CSV imports
**Test Data**: CSV with 1000+ recruiter records

**Test Steps**:

1. Create CSV with large number of recruiters
2. Import CSV file
3. Monitor performance metrics
4. Verify all records are processed correctly

**Expected Result**: System processes large imports within acceptable time

### ☐ TC-PERF-002: Concurrent User Testing

**Objective**: Verify system handles multiple users
**Test Setup**: 10+ concurrent users

**Test Steps**:

1. Have multiple users perform various operations simultaneously
2. Monitor system performance
3. Verify no data corruption or errors

**Expected Result**: System handles concurrent users without issues

### ☐ TC-PERF-003: Database Performance

**Objective**: Verify database performance with large datasets
**Test Data**: 10,000+ recruiter records, 1,000+ templates per user

**Test Steps**:

1. Load large datasets across multiple users
2. Perform various queries and operations
3. Monitor response times
4. Verify functionality remains intact

**Expected Result**: Database performs adequately with large datasets

## 11. Security Test Cases

### ☐ TC-SEC-001: JWT Token Validation

**Objective**: Verify JWT tokens are properly validated
**Test Steps**:

1. Obtain valid JWT token
2. Modify token content
3. Attempt API calls with modified token
4. Verify access denied

**Expected Result**: Modified tokens rejected, access denied

### ☐ TC-SEC-002: SQL Injection Prevention

**Objective**: Verify system is protected against SQL injection
**Test Data**: Malicious SQL strings in input fields

**Test Steps**:

1. Enter SQL injection attempts in various forms
2. Submit forms
3. Verify no database access or errors

**Expected Result**: SQL injection attempts blocked

### ☐ TC-SEC-003: File Upload Security

**Objective**: Verify file uploads are secure
**Test Data**: Malicious files (scripts, executables)

**Test Steps**:

1. Attempt to upload malicious files
2. Verify files are rejected or sanitized
3. Check no code execution occurs

**Expected Result**: Malicious files rejected, no security compromise

### ☐ TC-SEC-004: OAuth Token Security

**Objective**: Verify OAuth tokens are handled securely
**Test Steps**:

1. Complete OAuth flow
2. Verify tokens are encrypted in storage
3. Test token refresh mechanism
4. Verify token revocation works

**Expected Result**: OAuth tokens handled securely throughout lifecycle

## Test Execution Guidelines

### Pre-Test Setup

1. Ensure test environment is clean
2. Prepare test data files (CSV, resume files)
3. Configure Gmail test account
4. Set up monitoring/logging

### Test Execution Order

1. Authentication tests first
2. Basic CRUD operations
3. Integration tests
4. Performance tests
5. Security tests last

### Test Data Management

- Use consistent test data across test cases
- Clean up test data after each test run
- Maintain separate test and production environments

### Defect Reporting

- Document all failures with screenshots
- Include steps to reproduce
- Specify expected vs actual results
- Assign severity levels

### Test Coverage Metrics

- Aim for 90%+ functional coverage
- Include edge cases and error conditions
- Verify all user stories from requirements
- Test both positive and negative scenarios

## Automation Considerations

### Candidates for Automation

- Authentication flows
- CRUD operations for recruiters and templates
- Data import via CSV
- API endpoint testing
- User data isolation verification
- Infinite scroll functionality
- Regression tests

### Manual Testing Focus

- UI/UX validation
- Complex user workflows
- Exploratory testing
- Usability testing

This test case document covers all major requirements from the Requirements.md file and should provide comprehensive coverage for testing the ECold application.
