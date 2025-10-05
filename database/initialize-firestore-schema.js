const admin = require('firebase-admin');
const serviceAccount = require('./firestore-key.json');

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function initializeSchema() {
  console.log('🚀 Initializing Firestore schema with comprehensive sample data...\n');

  try {
    // ========================================
    // 1. CREATE DEMO USER
    // ========================================
    const demoUserId = 'demo_user_' + Date.now();
    const demoUserRef = db.collection('users').doc(demoUserId);

    console.log('📦 Creating demo user...');
    await demoUserRef.set({
      email: 'demo@ecold.com',
      name: 'Demo User',
      provider: 'GOOGLE',
      providerId: 'demo_123',
      profilePicture: 'https://lh3.googleusercontent.com/a/default-user',
      accessToken: null,
      refreshToken: null,
      tokenExpiresAt: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('✅ Created demo user:', demoUserId);

    // ========================================
    // 2. CREATE EMAIL TEMPLATES
    // ========================================
    console.log('\n📝 Creating email templates...');

    // Template 1: Default Outreach
    await demoUserRef.collection('templates').doc('template_outreach').set({
      name: 'Default Outreach',
      subject: 'Exploring Opportunities at {Company}',
      body: `Dear {RecruiterName},

I hope this email finds you well. I am writing to express my interest in the {Role} position at {Company}.

With my background in software development and passion for innovative solutions, I believe I would be a valuable addition to your team. I have attached my resume for your review.

I would welcome the opportunity to discuss how my skills and experience align with your requirements.

Best regards,
{MyName}`,
      type: 'OUTREACH',
      isDefault: true,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created template: Default Outreach');

    // Template 2: Follow-up
    await demoUserRef.collection('templates').doc('template_followup').set({
      name: 'Follow-up Template',
      subject: 'Following up on my application for {Role}',
      body: `Dear {RecruiterName},

I wanted to follow up on my recent application for the {Role} position at {Company}.

I remain very interested in this opportunity and would appreciate any updates you might have regarding my application status.

Thank you for your time and consideration.

Best regards,
{MyName}`,
      type: 'FOLLOWUP',
      isDefault: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created template: Follow-up');

    // Template 3: Thank You
    await demoUserRef.collection('templates').doc('template_thankyou').set({
      name: 'Thank You Template',
      subject: 'Thank you for the opportunity',
      body: `Dear {RecruiterName},

Thank you for taking the time to speak with me about the {Role} position at {Company}.

I enjoyed learning more about the role and your team. I am excited about the possibility of contributing to {Company}'s success.

Please let me know if you need any additional information.

Best regards,
{MyName}`,
      type: 'THANKYOU',
      isDefault: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created template: Thank You');

    // ========================================
    // 3. CREATE RECRUITERS
    // ========================================
    console.log('\n👥 Creating recruiter contacts...');

    // Recruiter 1: John Doe
    await demoUserRef.collection('recruiters').doc('recruiter_john').set({
      email: 'john.doe@techcorp.com',
      recruiterName: 'John Doe',
      companyName: 'TechCorp',
      jobRole: 'Senior Software Engineer',
      linkedinProfile: 'https://linkedin.com/in/johndoe',
      notes: 'Found on LinkedIn - very responsive',
      status: 'PENDING',
      lastContactedAt: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created recruiter: John Doe (TechCorp)');

    // Recruiter 2: Sarah Smith
    await demoUserRef.collection('recruiters').doc('recruiter_sarah').set({
      email: 'sarah.smith@innovate.com',
      recruiterName: 'Sarah Smith',
      companyName: 'Innovate Solutions',
      jobRole: 'Full Stack Developer',
      linkedinProfile: 'https://linkedin.com/in/sarahsmith',
      notes: 'Met at tech conference - interested in React developers',
      status: 'CONTACTED',
      lastContactedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 3 * 86400000)), // 3 days ago
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created recruiter: Sarah Smith (Innovate Solutions)');

    // Recruiter 3: Mike Johnson
    await demoUserRef.collection('recruiters').doc('recruiter_mike').set({
      email: 'mike.johnson@startupxyz.com',
      recruiterName: 'Mike Johnson',
      companyName: 'StartupXYZ',
      jobRole: 'Frontend Developer',
      linkedinProfile: 'https://linkedin.com/in/mikejohnson',
      notes: 'Early-stage startup - equity options available',
      status: 'RESPONDED',
      lastContactedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 7 * 86400000)), // 7 days ago
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created recruiter: Mike Johnson (StartupXYZ)');

    // Recruiter 4: Emily Chen
    await demoUserRef.collection('recruiters').doc('recruiter_emily').set({
      email: 'emily.chen@bigtech.com',
      recruiterName: 'Emily Chen',
      companyName: 'BigTech Corp',
      jobRole: 'Backend Engineer',
      linkedinProfile: 'https://linkedin.com/in/emilychen',
      notes: 'Large company - competitive salary',
      status: 'SHORTLISTED',
      lastContactedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 10 * 86400000)), // 10 days ago
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created recruiter: Emily Chen (BigTech Corp)');

    // ========================================
    // 4. CREATE ASSIGNMENTS
    // ========================================
    console.log('\n🔗 Creating template-recruiter assignments...');

    await demoUserRef.collection('assignments').doc('assignment_1').set({
      recruiterId: 'recruiter_john',
      templateId: 'template_outreach',
      weekAssigned: 1,
      yearAssigned: 2025,
      assignmentStatus: 'ACTIVE',
      emailsSent: 0,
      lastEmailSentAt: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created assignment: John Doe + Default Outreach');

    await demoUserRef.collection('assignments').doc('assignment_2').set({
      recruiterId: 'recruiter_sarah',
      templateId: 'template_outreach',
      weekAssigned: 1,
      yearAssigned: 2025,
      assignmentStatus: 'COMPLETED',
      emailsSent: 1,
      lastEmailSentAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 3 * 86400000)),
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created assignment: Sarah Smith + Default Outreach (Completed)');

    await demoUserRef.collection('assignments').doc('assignment_3').set({
      recruiterId: 'recruiter_mike',
      templateId: 'template_followup',
      weekAssigned: 2,
      yearAssigned: 2025,
      assignmentStatus: 'ACTIVE',
      emailsSent: 1,
      lastEmailSentAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 7 * 86400000)),
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created assignment: Mike Johnson + Follow-up');

    // ========================================
    // 5. CREATE SCHEDULED EMAILS
    // ========================================
    console.log('\n📅 Creating scheduled emails...');

    await demoUserRef.collection('scheduled_emails').doc('scheduled_1').set({
      recipientEmail: 'john.doe@techcorp.com',
      subject: 'Exploring Opportunities at TechCorp',
      body: 'Dear John,\n\nI hope this email finds you well...',
      scheduleTime: admin.firestore.Timestamp.fromDate(new Date(Date.now() + 2 * 86400000)), // 2 days from now
      status: 'SCHEDULED',
      templateId: 'template_outreach',
      recruiterId: 'recruiter_john',
      isHtml: false,
      priority: 'NORMAL',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created scheduled email: To John Doe (2 days from now)');

    await demoUserRef.collection('scheduled_emails').doc('scheduled_2').set({
      recipientEmail: 'emily.chen@bigtech.com',
      subject: 'Following up on my application for Backend Engineer',
      body: 'Dear Emily,\n\nI wanted to follow up on my recent application...',
      scheduleTime: admin.firestore.Timestamp.fromDate(new Date(Date.now() + 5 * 86400000)), // 5 days from now
      status: 'SCHEDULED',
      templateId: 'template_followup',
      recruiterId: 'recruiter_emily',
      isHtml: false,
      priority: 'HIGH',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created scheduled email: To Emily Chen (5 days from now)');

    // ========================================
    // 6. CREATE EMAIL LOGS
    // ========================================
    console.log('\n📧 Creating email logs...');

    await demoUserRef.collection('email_logs').doc('log_1').set({
      recruiterContactId: 'recruiter_sarah',
      recipientEmail: 'sarah.smith@innovate.com',
      subject: 'Exploring Opportunities at Innovate Solutions',
      body: 'Dear Sarah,\n\nI hope this email finds you well...',
      status: 'SENT',
      messageId: 'msg_' + Date.now() + '_1',
      sentAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 3 * 86400000)),
      retryCount: 0,
      createdAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 3 * 86400000))
    });
    console.log('  ✅ Created email log: Sent to Sarah Smith');

    await demoUserRef.collection('email_logs').doc('log_2').set({
      recruiterContactId: 'recruiter_mike',
      recipientEmail: 'mike.johnson@startupxyz.com',
      subject: 'Following up on my application for Frontend Developer',
      body: 'Dear Mike,\n\nI wanted to follow up...',
      status: 'SENT',
      messageId: 'msg_' + Date.now() + '_2',
      sentAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 7 * 86400000)),
      retryCount: 0,
      createdAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 7 * 86400000))
    });
    console.log('  ✅ Created email log: Sent to Mike Johnson');

    await demoUserRef.collection('email_logs').doc('log_3').set({
      recruiterContactId: 'recruiter_emily',
      recipientEmail: 'emily.chen@bigtech.com',
      subject: 'Exploring Opportunities at BigTech Corp',
      body: 'Dear Emily,\n\nI am writing to express my interest...',
      status: 'DELIVERED',
      messageId: 'msg_' + Date.now() + '_3',
      sentAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 10 * 86400000)),
      deliveredAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 10 * 86400000 + 300000)),
      retryCount: 0,
      createdAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 10 * 86400000))
    });
    console.log('  ✅ Created email log: Delivered to Emily Chen');

    // ========================================
    // 7. CREATE INCOMING EMAILS
    // ========================================
    console.log('\n📨 Creating incoming emails...');

    await demoUserRef.collection('incoming_emails').doc('incoming_1').set({
      messageId: 'incoming_msg_001',
      senderEmail: 'hr@techcorp.com',
      senderName: 'TechCorp HR',
      subject: 'Interview Invitation - Senior Software Engineer',
      body: 'We would like to invite you for an interview for the Senior Software Engineer position. Are you available next week?',
      htmlBody: '<p>We would like to invite you for an interview for the Senior Software Engineer position. Are you available next week?</p>',
      category: 'SHORTLIST_INTERVIEW',
      priority: 'HIGH',
      isRead: false,
      isProcessed: false,
      receivedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 2 * 86400000)),
      createdAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 2 * 86400000)),
      threadId: 'thread_001',
      keywords: 'interview, invitation, senior software engineer',
      confidenceScore: 0.95
    });
    console.log('  ✅ Created incoming email: Interview invitation from TechCorp');

    await demoUserRef.collection('incoming_emails').doc('incoming_2').set({
      messageId: 'incoming_msg_002',
      senderEmail: 'recruiter@innovate.com',
      senderName: 'Innovate Recruiter',
      subject: 'Application Status Update',
      body: 'Thank you for your application. We are reviewing your resume and will get back to you within 5 business days.',
      htmlBody: '<p>Thank you for your application. We are reviewing your resume and will get back to you within 5 business days.</p>',
      category: 'APPLICATION_UPDATE',
      priority: 'NORMAL',
      isRead: true,
      isProcessed: true,
      receivedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 5 * 86400000)),
      createdAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 5 * 86400000)),
      threadId: 'thread_002',
      keywords: 'application, status, review',
      confidenceScore: 0.87
    });
    console.log('  ✅ Created incoming email: Application update from Innovate Solutions');

    await demoUserRef.collection('incoming_emails').doc('incoming_3').set({
      messageId: 'incoming_msg_003',
      senderEmail: 'noreply@startupxyz.com',
      senderName: 'StartupXYZ',
      subject: 'We received your application',
      body: 'Thank you for applying to StartupXYZ. We have received your application and will review it shortly.',
      htmlBody: '<p>Thank you for applying to StartupXYZ. We have received your application and will review it shortly.</p>',
      category: 'ACKNOWLEDGMENT',
      priority: 'LOW',
      isRead: true,
      isProcessed: true,
      receivedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 8 * 86400000)),
      createdAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 8 * 86400000)),
      threadId: 'thread_003',
      keywords: 'received, application, acknowledgment',
      confidenceScore: 0.92
    });
    console.log('  ✅ Created incoming email: Acknowledgment from StartupXYZ');

    await demoUserRef.collection('incoming_emails').doc('incoming_4').set({
      messageId: 'incoming_msg_004',
      senderEmail: 'emily.chen@bigtech.com',
      senderName: 'Emily Chen',
      subject: 'RE: Exploring Opportunities at BigTech Corp',
      body: 'Hi! Thanks for reaching out. Your profile looks interesting. Would you be available for a quick call next Tuesday?',
      htmlBody: '<p>Hi! Thanks for reaching out. Your profile looks interesting. Would you be available for a quick call next Tuesday?</p>',
      category: 'POSITIVE_RESPONSE',
      priority: 'HIGH',
      isRead: false,
      isProcessed: false,
      receivedAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 1 * 86400000)),
      createdAt: admin.firestore.Timestamp.fromDate(new Date(Date.now() - 1 * 86400000)),
      threadId: 'thread_004',
      keywords: 'call, interested, available',
      confidenceScore: 0.98
    });
    console.log('  ✅ Created incoming email: Positive response from Emily Chen');

    // ========================================
    // 8. CREATE RESUMES
    // ========================================
    console.log('\n📄 Creating resumes...');

    await demoUserRef.collection('resumes').doc('resume_default').set({
      name: 'Software Engineer Resume',
      fileName: 'resume_software_engineer.pdf',
      filePath: '/uploads/resumes/demo_user/resume_software_engineer.pdf',
      contentType: 'application/pdf',
      fileSize: 245760, // ~240KB
      isDefault: true,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created resume: Software Engineer Resume (default)');

    await demoUserRef.collection('resumes').doc('resume_fullstack').set({
      name: 'Full Stack Developer Resume',
      fileName: 'resume_fullstack.pdf',
      filePath: '/uploads/resumes/demo_user/resume_fullstack.pdf',
      contentType: 'application/pdf',
      fileSize: 198400, // ~194KB
      isDefault: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('  ✅ Created resume: Full Stack Developer Resume');

    // ========================================
    // SUMMARY
    // ========================================
    console.log('\n' + '='.repeat(60));
    console.log('🎉 Firestore schema initialized successfully!');
    console.log('='.repeat(60));
    console.log('\n📊 Created Collections and Documents:');
    console.log('  👤 Users: 1 user (demo@ecold.com)');
    console.log('  📝 Email Templates: 3 templates');
    console.log('  👥 Recruiters: 4 recruiters');
    console.log('  🔗 Assignments: 3 template-recruiter assignments');
    console.log('  📅 Scheduled Emails: 2 scheduled emails');
    console.log('  📧 Email Logs: 3 sent/delivered emails');
    console.log('  📨 Incoming Emails: 4 incoming emails');
    console.log('  📄 Resumes: 2 resume documents');
    console.log('\n📍 Demo User ID:', demoUserId);
    console.log('📧 Demo User Email: demo@ecold.com');
    console.log('\n🔗 View in Firebase Console:');
    console.log('   https://console.firebase.google.com/project/ecold-app-d3990/firestore');
    console.log('\n✅ All collections are now ready for the ECold application!');
    console.log('='.repeat(60));

  } catch (error) {
    console.error('\n❌ Error initializing schema:', error);
    throw error;
  } finally {
    process.exit();
  }
}

initializeSchema();
