-- ECold Database Initialization Script
-- Note: This script runs in the context of the 'ecold' database
-- which is already created by the Docker environment variable

-- Create all tables
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    profile_picture TEXT,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255),
    access_token TEXT,
    refresh_token TEXT,
    token_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_templates (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    type VARCHAR(50) DEFAULT 'OUTREACH',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS recruiter_contacts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    recruiter_name VARCHAR(255),
    company_name VARCHAR(255) NOT NULL,
    job_role VARCHAR(255) NOT NULL,
    linkedin_profile TEXT,
    notes TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    last_contacted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS recruiter_template_assignments (
    id BIGSERIAL PRIMARY KEY,
    recruiter_id BIGINT NOT NULL REFERENCES recruiter_contacts(id) ON DELETE CASCADE,
    template_id BIGINT NOT NULL REFERENCES email_templates(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    week_assigned INTEGER NOT NULL,
    year_assigned INTEGER NOT NULL,
    assignment_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    emails_sent INTEGER DEFAULT 0,
    last_email_sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scheduled_emails (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    schedule_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    template_id BIGINT,
    recruiter_id BIGINT,
    error_message TEXT,
    sent_at TIMESTAMP,
    message_id VARCHAR(255),
    is_html BOOLEAN DEFAULT FALSE,
    priority VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS email_logs (
    id BIGSERIAL PRIMARY KEY,
    recruiter_contact_id BIGINT REFERENCES recruiter_contacts(id),
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    message_id VARCHAR(255),
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    opened_at TIMESTAMP,
    clicked_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS incoming_emails (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_id VARCHAR(255) NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    sender_name VARCHAR(255),
    subject VARCHAR(500) NOT NULL,
    body TEXT,
    html_body TEXT,
    category VARCHAR(50),
    priority VARCHAR(20) DEFAULT 'NORMAL',
    is_read BOOLEAN DEFAULT FALSE,
    is_processed BOOLEAN DEFAULT FALSE,
    received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    thread_id VARCHAR(255),
    keywords TEXT,
    confidence_score DECIMAL(3,2)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_recruiters_user_id ON recruiter_contacts(user_id);
CREATE INDEX IF NOT EXISTS idx_recruiter_template_assignments_user ON recruiter_template_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_recruiter_template_assignments_recruiter ON recruiter_template_assignments(recruiter_id);
CREATE INDEX IF NOT EXISTS idx_recruiter_template_assignments_template ON recruiter_template_assignments(template_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_emails_user ON scheduled_emails(user_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_emails_status ON scheduled_emails(status);
CREATE INDEX IF NOT EXISTS idx_scheduled_emails_schedule_time ON scheduled_emails(schedule_time);
CREATE INDEX IF NOT EXISTS idx_email_logs_recruiter ON email_logs(recruiter_contact_id);
CREATE INDEX IF NOT EXISTS idx_incoming_emails_user_id ON incoming_emails(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_incoming_emails_unique ON incoming_emails(user_id, message_id);

-- Insert sample data
INSERT INTO users (email, name, provider, provider_id, created_at) VALUES 
('demo@ecold.com', 'Demo User', 'GOOGLE', 'demo_123', CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

DO $$
DECLARE
    demo_user_id BIGINT;
BEGIN
    SELECT id INTO demo_user_id FROM users WHERE email = 'demo@ecold.com';
    
    -- Sample email templates
    INSERT INTO email_templates (user_id, name, subject, body, is_default, type) VALUES 
    (demo_user_id, 'Default Outreach', 'Exploring Opportunities at {Company}', 
     'Dear {RecruiterName},

I hope this email finds you well. I am writing to express my interest in the {Role} position at {Company}.

With my background in software development and passion for innovative solutions, I believe I would be a valuable addition to your team. I have attached my resume for your review.

I would welcome the opportunity to discuss how my skills and experience align with your requirements.

Best regards,
{MyName}', true, 'OUTREACH')
    ON CONFLICT DO NOTHING;

    -- Sample recruiters
    INSERT INTO recruiter_contacts (user_id, email, recruiter_name, company_name, job_role, status) VALUES 
    (demo_user_id, 'john.doe@techcorp.com', 'John Doe', 'TechCorp', 'Senior Software Engineer', 'PENDING'),
    (demo_user_id, 'sarah.smith@innovate.com', 'Sarah Smith', 'Innovate Solutions', 'Full Stack Developer', 'CONTACTED'),
    (demo_user_id, 'mike.johnson@startupxyz.com', 'Mike Johnson', 'StartupXYZ', 'Frontend Developer', 'RESPONDED')
    ON CONFLICT DO NOTHING;

    -- Sample incoming emails
    INSERT INTO incoming_emails (user_id, message_id, sender_email, sender_name, subject, body, category, priority, received_at, is_read) VALUES 
    (demo_user_id, 'msg_001', 'hr@techcorp.com', 'TechCorp HR', 'Interview Invitation - Senior Software Engineer', 'We would like to invite you for an interview...', 'SHORTLIST_INTERVIEW', 'HIGH', CURRENT_TIMESTAMP - INTERVAL '2 days', false),
    (demo_user_id, 'msg_002', 'recruiter@innovate.com', 'Innovate Recruiter', 'Application Status Update', 'Thank you for your application. We are reviewing...', 'APPLICATION_UPDATE', 'NORMAL', CURRENT_TIMESTAMP - INTERVAL '5 days', true)
    ON CONFLICT DO NOTHING;

END $$;