## Email Template Integration - Testing Guide

### Overview
The ECold application now supports template selection when sending emails to recruiters. This integration allows users to:

1. **Select from existing active email templates** when composing emails to recruiters
2. **Preview how templates will look** with actual recruiter data
3. **Customize templates** before sending
4. **Track template usage** automatically

### How to Test

#### 1. Prerequisites
Make sure you have:
- At least one **Active** email template created in the Templates section
- At least one recruiter in your database
- Angular frontend running on port 4200
- Spring Boot backend running on port 8080

#### 2. Testing the Email Compose Dialog

**From Recruiter List:**
1. Go to the Recruiters page
2. Click the "More Actions" (3 dots) menu for any recruiter
3. Select "Send Email"
4. The new Email Compose Dialog should open

**From Recruiter Details:**
1. Click "View Details" for any recruiter
2. Click the "Send Email" button in the quick actions
3. The Email Compose Dialog should open

#### 3. Template Features to Test

**Template Selection:**
- Select different templates from the dropdown
- Verify placeholders are automatically replaced with recruiter data
- Test template removal and switching between templates

**Placeholder Replacement:**
- `{Company}` → Should show recruiter's company name
- `{Role}` → Should show job role if available
- `{RecruiterName}` → Should show recruiter's name
- `{MyName}` → Should show "[Your Name]" (to be replaced manually)

**Preview Mode:**
- Toggle preview mode to see formatted email
- Verify HTML line breaks are displayed correctly

**Template Stats:**
- Usage count should increment when template is used
- Response rate should display if available

#### 4. Integration Points

**Template Management:**
- Only **Active** templates appear in the email composer
- Templates are grouped by category (Outreach, Follow-up, etc.)
- Template usage is tracked when applied

**Recruiter Status:**
- When email is sent, recruiter status should update to "CONTACTED"
- Last contacted date should be updated
- Status change should be visible immediately in the recruiter list

#### 5. Example Email Templates

Create these test templates to fully test the system:

**Template 1: Basic Outreach**
```
Name: "Tech Company Outreach"
Category: OUTREACH
Status: ACTIVE
Subject: "Software Developer Opportunity Inquiry - {Company}"
Body: "Hi {RecruiterName},

I hope this email finds you well. I noticed {Company} has been growing rapidly in the tech space, and I'm very interested in potential {Role} opportunities.

I have [X years] of experience in software development and would love to contribute to your team's success.

Could we schedule a brief call to discuss potential opportunities?

Best regards,
{MyName}"
```

**Template 2: Follow-up**
```
Name: "Follow-up After Application"
Category: FOLLOW_UP  
Status: ACTIVE
Subject: "Following up on {Role} application at {Company}"
Body: "Hi {RecruiterName},

I recently applied for the {Role} position at {Company} and wanted to follow up on my application status.

I'm very excited about the opportunity to contribute to {Company}'s mission and would appreciate any updates you might have.

Thank you for your time and consideration.

Best regards,
{MyName}"
```

#### 6. Troubleshooting

**Common Issues:**

1. **Dialog doesn't open:** Check browser console for errors, ensure EmailComposeDialogComponent is properly imported
2. **Templates not loading:** Verify templates exist and have "ACTIVE" status
3. **Placeholders not replaced:** Check template contains valid placeholder syntax: `{PlaceholderName}`
4. **Recruiter status not updating:** Check network tab for API errors when marking as contacted

**Browser Console Commands for Testing:**
```javascript
// Check if templates are loaded
console.log('Available templates:', localStorage.getItem('ecold-templates'));

// Check current user session
console.log('Auth token:', localStorage.getItem('authToken'));

// Test template service
// (Run this in browser console on the app page)
const templateService = angular.element(document.body).injector().get('EmailTemplateService');
templateService.getActiveTemplates().subscribe(templates => console.log('Active templates:', templates));
```

#### 7. Expected User Flow

1. User navigates to Recruiters page
2. User clicks "Send Email" for a recruiter
3. Email Compose Dialog opens with empty form
4. User selects a template from dropdown
5. Template content populates with placeholders replaced
6. User can preview, edit, or directly send
7. User clicks "Send Email"
8. Default email client opens with populated content
9. Dialog closes and recruiter status updates to "CONTACTED"
10. Success message appears confirming the action

### Backend API Endpoints Used

- `GET /email-templates/status/ACTIVE` - Get active templates
- `POST /email-templates/{id}/use` - Track template usage
- `PUT /recruiters/{id}/mark-contacted` - Update recruiter status

### File Structure

```
frontend/src/app/
├── components/
│   ├── email-compose-dialog/
│   │   ├── email-compose-dialog.component.ts
│   │   ├── email-compose-dialog.component.html
│   │   └── email-compose-dialog.component.scss
│   └── recruiters/
│       ├── recruiter-list/
│       │   └── recruiter-list.component.ts (updated)
│       └── recruiter-view-dialog/
│           └── recruiter-view-dialog.component.ts (updated)
└── services/
    └── email-template.service.ts (updated)
```

This integration provides a seamless way for users to leverage their email templates when reaching out to recruiters, making the outreach process more efficient and consistent.
