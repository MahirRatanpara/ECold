const admin = require('firebase-admin');
const serviceAccount = require('./firestore-key.json');

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function verifyFirestoreData() {
  console.log('🔍 Verifying Firestore data...\n');
  console.log('='.repeat(70));

  try {
    // Get all users
    const usersSnapshot = await db.collection('users').get();
    console.log(`\n✅ USERS COLLECTION: Found ${usersSnapshot.size} user(s)\n`);

    if (usersSnapshot.empty) {
      console.log('❌ No users found!');
      process.exit(1);
    }

    // Iterate through each user and check subcollections
    for (const userDoc of usersSnapshot.docs) {
      const userId = userDoc.id;
      const userData = userDoc.data();

      console.log('👤 USER:', userId);
      console.log('   Email:', userData.email);
      console.log('   Name:', userData.name);
      console.log('   Provider:', userData.provider);
      console.log('-'.repeat(70));

      // Check templates
      const templatesSnapshot = await db.collection('users').doc(userId).collection('templates').get();
      console.log(`   📝 TEMPLATES: ${templatesSnapshot.size} documents`);
      templatesSnapshot.forEach(doc => {
        const data = doc.data();
        console.log(`      - ${doc.id}: "${data.name}" (${data.type})`);
      });

      // Check recruiters
      const recruitersSnapshot = await db.collection('users').doc(userId).collection('recruiters').get();
      console.log(`\n   👥 RECRUITERS: ${recruitersSnapshot.size} documents`);
      recruitersSnapshot.forEach(doc => {
        const data = doc.data();
        console.log(`      - ${doc.id}: ${data.recruiterName} at ${data.companyName} (${data.status})`);
      });

      // Check assignments
      const assignmentsSnapshot = await db.collection('users').doc(userId).collection('assignments').get();
      console.log(`\n   🔗 ASSIGNMENTS: ${assignmentsSnapshot.size} documents`);
      assignmentsSnapshot.forEach(doc => {
        const data = doc.data();
        console.log(`      - ${doc.id}: Week ${data.weekAssigned}/${data.yearAssigned} (${data.assignmentStatus})`);
      });

      // Check scheduled emails
      const scheduledSnapshot = await db.collection('users').doc(userId).collection('scheduled_emails').get();
      console.log(`\n   📅 SCHEDULED EMAILS: ${scheduledSnapshot.size} documents`);
      scheduledSnapshot.forEach(doc => {
        const data = doc.data();
        console.log(`      - ${doc.id}: To ${data.recipientEmail} (${data.status})`);
      });

      // Check email logs
      const logsSnapshot = await db.collection('users').doc(userId).collection('email_logs').get();
      console.log(`\n   📧 EMAIL LOGS: ${logsSnapshot.size} documents`);
      logsSnapshot.forEach(doc => {
        const data = doc.data();
        console.log(`      - ${doc.id}: To ${data.recipientEmail} (${data.status})`);
      });

      // Check incoming emails
      const incomingSnapshot = await db.collection('users').doc(userId).collection('incoming_emails').get();
      console.log(`\n   📨 INCOMING EMAILS: ${incomingSnapshot.size} documents`);
      incomingSnapshot.forEach(doc => {
        const data = doc.data();
        console.log(`      - ${doc.id}: From ${data.senderEmail} - "${data.subject}" (${data.category})`);
      });

      // Check resumes
      const resumesSnapshot = await db.collection('users').doc(userId).collection('resumes').get();
      console.log(`\n   📄 RESUMES: ${resumesSnapshot.size} documents`);
      resumesSnapshot.forEach(doc => {
        const data = doc.data();
        console.log(`      - ${doc.id}: "${data.name}" (${data.isDefault ? 'DEFAULT' : 'Not default'})`);
      });

      console.log('\n' + '='.repeat(70));
    }

    // Summary
    console.log('\n📊 VERIFICATION SUMMARY:');
    console.log('='.repeat(70));

    let totalTemplates = 0;
    let totalRecruiters = 0;
    let totalAssignments = 0;
    let totalScheduled = 0;
    let totalLogs = 0;
    let totalIncoming = 0;
    let totalResumes = 0;

    for (const userDoc of usersSnapshot.docs) {
      const userId = userDoc.id;
      totalTemplates += (await db.collection('users').doc(userId).collection('templates').get()).size;
      totalRecruiters += (await db.collection('users').doc(userId).collection('recruiters').get()).size;
      totalAssignments += (await db.collection('users').doc(userId).collection('assignments').get()).size;
      totalScheduled += (await db.collection('users').doc(userId).collection('scheduled_emails').get()).size;
      totalLogs += (await db.collection('users').doc(userId).collection('email_logs').get()).size;
      totalIncoming += (await db.collection('users').doc(userId).collection('incoming_emails').get()).size;
      totalResumes += (await db.collection('users').doc(userId).collection('resumes').get()).size;
    }

    console.log(`✅ Users:            ${usersSnapshot.size}`);
    console.log(`✅ Templates:        ${totalTemplates}`);
    console.log(`✅ Recruiters:       ${totalRecruiters}`);
    console.log(`✅ Assignments:      ${totalAssignments}`);
    console.log(`✅ Scheduled Emails: ${totalScheduled}`);
    console.log(`✅ Email Logs:       ${totalLogs}`);
    console.log(`✅ Incoming Emails:  ${totalIncoming}`);
    console.log(`✅ Resumes:          ${totalResumes}`);
    console.log('='.repeat(70));

    console.log('\n🎉 All data verified successfully!');
    console.log('\n📌 HOW TO VIEW SUBCOLLECTIONS IN FIREBASE CONSOLE:');
    console.log('   1. Go to: https://console.firebase.google.com/project/ecold-app-d3990/firestore');
    console.log('   2. Click on the "users" collection');
    console.log('   3. Click on a user document (e.g., ' + usersSnapshot.docs[0].id + ')');
    console.log('   4. You will see all subcollections listed inside the user document');
    console.log('   5. Click on any subcollection (templates, recruiters, etc.) to view its documents\n');

  } catch (error) {
    console.error('❌ Error verifying data:', error);
    throw error;
  } finally {
    process.exit();
  }
}

verifyFirestoreData();
