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

CREATE TABLE IF NOT EXISTS email_campaigns (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    template_id BIGINT REFERENCES email_templates(id),
    resume_id BIGINT REFERENCES resumes(id),
    status VARCHAR(50) DEFAULT 'DRAFT',
    schedule_type VARCHAR(50) DEFAULT 'ONE_TIME',
    scheduled_at TIMESTAMP,
    batch_size INTEGER DEFAULT 5,
    daily_limit INTEGER DEFAULT 50,
    cc_emails TEXT,
    bcc_emails TEXT,
    total_recipients INTEGER DEFAULT 0,
    sent_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_logs (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT REFERENCES email_campaigns(id),
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
CREATE INDEX IF NOT EXISTS idx_email_logs_campaign ON email_logs(campaign_id);
CREATE INDEX IF NOT EXISTS idx_incoming_emails_user_id ON incoming_emails(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_incoming_emails_unique ON incoming_emails(user_id, message_id);

-- Quartz Scheduler Tables
CREATE TABLE IF NOT EXISTS qrtz_job_details
(
    sched_name varchar(120) not null,
    job_name varchar(80) not null,
    job_group varchar(80) not null,
    description varchar(250) null,
    job_class_name varchar(250) not null,
    is_durable bool not null,
    is_nonconcurrent bool not null,
    is_update_data bool not null,
    requests_recovery bool not null,
    job_data bytea null,
    primary key (sched_name,job_name,job_group)
);

CREATE TABLE IF NOT EXISTS qrtz_triggers
(
    sched_name varchar(120) not null,
    trigger_name varchar(80) not null,
    trigger_group varchar(80) not null,
    job_name varchar(80) not null,
    job_group varchar(80) not null,
    description varchar(250) null,
    next_fire_time bigint null,
    prev_fire_time bigint null,
    priority integer null,
    execution_group varchar(80) null,
    trigger_state varchar(16) not null,
    trigger_type varchar(8) not null,
    start_time bigint not null,
    end_time bigint null,
    calendar_name varchar(80) null,
    misfire_instr smallint null,
    job_data bytea null,
    primary key (sched_name,trigger_name,trigger_group),
    foreign key (sched_name,job_name,job_group) references qrtz_job_details(sched_name,job_name,job_group)
);

CREATE TABLE IF NOT EXISTS qrtz_simple_triggers
(
    sched_name varchar(120) not null,
    trigger_name varchar(80) not null,
    trigger_group varchar(80) not null,
    repeat_count bigint not null,
    repeat_interval bigint not null,
    times_triggered bigint not null,
    primary key (sched_name,trigger_name,trigger_group),
    foreign key (sched_name,trigger_name,trigger_group) references qrtz_triggers(sched_name,trigger_name,trigger_group)
);

CREATE TABLE IF NOT EXISTS qrtz_cron_triggers
(
    sched_name varchar(120) not null,
    trigger_name varchar(80) not null,
    trigger_group varchar(80) not null,
    cron_expression varchar(120) not null,
    time_zone_id varchar(80),
    primary key (sched_name,trigger_name,trigger_group),
    foreign key (sched_name,trigger_name,trigger_group) references qrtz_triggers(sched_name,trigger_name,trigger_group)
);

CREATE TABLE IF NOT EXISTS qrtz_simprop_triggers
(
    sched_name varchar(120) not null,
    trigger_name varchar(80) not null,
    trigger_group varchar(80) not null,
    str_prop_1 varchar(512) null,
    str_prop_2 varchar(512) null,
    str_prop_3 varchar(512) null,
    int_prop_1 int null,
    int_prop_2 int null,
    long_prop_1 bigint null,
    long_prop_2 bigint null,
    dec_prop_1 numeric(13,4) null,
    dec_prop_2 numeric(13,4) null,
    bool_prop_1 bool null,
    bool_prop_2 bool null,
    primary key (sched_name,trigger_name,trigger_group),
    foreign key (sched_name,trigger_name,trigger_group) references qrtz_triggers(sched_name,trigger_name,trigger_group)
);

CREATE TABLE IF NOT EXISTS qrtz_blob_triggers
(
    sched_name varchar(120) not null,
    trigger_name varchar(80) not null,
    trigger_group varchar(80) not null,
    blob_data bytea null,
    primary key (sched_name,trigger_name,trigger_group),
    foreign key (sched_name,trigger_name,trigger_group) references qrtz_triggers(sched_name,trigger_name,trigger_group)
);

CREATE TABLE IF NOT EXISTS qrtz_calendars
(
    sched_name varchar(120) not null,
    calendar_name varchar(80) not null,
    calendar bytea not null,
    primary key (sched_name,calendar_name)
);

CREATE TABLE IF NOT EXISTS qrtz_paused_trigger_grps
(
    sched_name varchar(120) not null,
    trigger_group varchar(80) not null,
    primary key (sched_name,trigger_group)
);

CREATE TABLE IF NOT EXISTS qrtz_fired_triggers
(
    sched_name varchar(120) not null,
    entry_id varchar(95) not null,
    trigger_name varchar(80) not null,
    trigger_group varchar(80) not null,
    instance_name varchar(80) not null,
    fired_time bigint not null,
    sched_time bigint not null,
    priority integer not null,
    execution_group varchar(80) null,
    state varchar(16) not null,
    job_name varchar(80) null,
    job_group varchar(80) null,
    is_nonconcurrent bool null,
    requests_recovery bool null,
    primary key (sched_name,entry_id)
);

CREATE TABLE IF NOT EXISTS qrtz_scheduler_state
(
    sched_name varchar(120) not null,
    instance_name varchar(80) not null,
    last_checkin_time bigint not null,
    checkin_interval bigint not null,
    primary key (sched_name,instance_name)
);

CREATE TABLE IF NOT EXISTS qrtz_locks
(
    sched_name varchar(120) not null,
    lock_name varchar(40) not null,
    primary key (sched_name,lock_name)
);

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