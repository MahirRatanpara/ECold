# Job Application Automation & Tracking - Requirements

## 1. User Inputs

- Ability to upload a data feed (CSV/Excel/Google Sheet) containing recruiter email ID, company name, job role/position, recruiter name/LinkedIn profile (optional).
- Upload and store my resume (PDF/DOC format).
- Define subject line template with placeholders like {Company}, {Role}.
- Define body email template with placeholders like {RecruiterName}, {Company}, {Role}, {MyName}.
- Maintain multiple templates and select one while scheduling.

## 2. Email Sending

- Integration with Gmail/Outlook/SMTP.
- Personalization: Replace placeholders in template with recruiter/company/job role details.
- Attach resume automatically to each email.
- Option to add CC/BCC (e.g., my secondary email).

## 3. Scheduling

- Schedule sending for specific day/time (e.g., every morning at 9 AM).
- Support one-time or recurring schedules (e.g., 5 emails every weekday).
- Control batch size (limit X emails/day to avoid spam).

## 4. Email Management

- Track which recruiters have already been contacted (avoid duplicate sends).
- Status logs for each email: Sent, Failed (with reason), Scheduled.

## 5. Incoming Email Tracking (NEW FEATURE)

- Connect with Gmail/Outlook inbox via OAuth.
- Automatically scan incoming emails for job-related keywords (application, shortlisted, interview, resume, recruiter, HR).
- Whitelist domains (e.g., @naukri.com, @linkedin.com, @indeed.com, recruiter company domains).
- Categorize incoming job-related emails: Application Updates, Shortlists/Interview Calls, Rejections/Closed Roles, Recruiter Outreach.
- Show timeline of job applications across portals and recruiters.
- Notifications for unread recruiter/job mails in dashboard.
- Option to export application tracking data (CSV/Excel).

## 6. UI/UX Requirements

- Dashboard showing upcoming scheduled emails, past sent emails with details, and categorized job related inbox results.
- Easy upload for recruiter lists and resumes.
- Test mode: send a sample email to myself before scheduling bulk send.

## 7. Technical/Backend Requirements

- Secure storage of resume and recruiter data.
- Support OAuth login (no plain credential storage).
- Error handling for invalid emails or failed SMTP connection.
- Logs and reporting of daily email activity and failures.
- Scalable to handle hundreds of recruiter emails.
- Background jobs to scan inbox periodically (e.g., every hour).

## 8. Nice-to-have Features (Future Enhancements)

- AI-assisted personalization of cover letters.
- Track email opens and link clicks (basic analytics).
- Chrome/Outlook plugin to import recruiter emails directly.
- WhatsApp/LinkedIn auto follow-up reminders.
- AI classifier (NLP) to detect job-related emails with high accuracy.
