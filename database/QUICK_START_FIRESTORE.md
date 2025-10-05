# Quick Start Guide: Firebase Firestore for ECold

## 🚀 Getting Started

### 1. **Prerequisites**

- Firebase CLI installed: `npm install -g firebase-tools`
- Node.js installed (for initialization scripts)
- Firestore credentials file (`firestore-key.json`) in `backend/src/main/resources/`

---

### 2. **Initialize Firestore Data**

```bash
cd database

# Install dependencies
npm install firebase-admin

# Run initialization script
node initialize-firestore-schema.js
```

**Output:**
```
🚀 Initializing Firestore schema with comprehensive sample data...

📦 Creating demo user...
✅ Created demo user: demo_user_1759649339687

📝 Creating email templates...
  ✅ Created template: Default Outreach
  ✅ Created template: Follow-up
  ✅ Created template: Thank You

👥 Creating recruiter contacts...
  ✅ Created recruiter: John Doe (TechCorp)
  ✅ Created recruiter: Sarah Smith (Innovate Solutions)
  ✅ Created recruiter: Mike Johnson (StartupXYZ)
  ✅ Created recruiter: Emily Chen (BigTech Corp)

... [additional output]

🎉 Firestore schema initialized successfully!
```

---

### 3. **Deploy Security Rules**

```bash
# Deploy Firestore security rules
firebase deploy --only firestore:rules
```

**What it does:**
- Deploys user-based access control
- Users can only access their own data
- All subcollections inherit user-level permissions

---

### 4. **Deploy Composite Indexes**

```bash
# Deploy Firestore composite indexes
firebase deploy --only firestore:indexes
```

**Creates 5 indexes for:**
1. Recruiters by status and created date
2. Email logs by status and sent date
3. Scheduled emails by status and schedule time
4. Incoming emails by category and received date
5. Assignments by week, year, and status

---

### 5. **Verify Data**

```bash
# Run verification script
node verify-firestore-data.js
```

**Output:**
```
🔍 Verifying Firestore data...

✅ USERS COLLECTION: Found 1 user(s)

👤 USER: demo_user_1759649339687
   📝 TEMPLATES: 3 documents
   👥 RECRUITERS: 4 documents
   🔗 ASSIGNMENTS: 3 documents
   📅 SCHEDULED EMAILS: 2 documents
   📧 EMAIL LOGS: 3 documents
   📨 INCOMING EMAILS: 4 documents
   📄 RESUMES: 2 documents

🎉 All data verified successfully!
```

---

## 🔥 **Access Firestore Console**

1. **Open Firebase Console:**
   https://console.firebase.google.com/project/ecold-app-d3990/firestore

2. **Navigate to Data:**
   - Click on **"users"** collection
   - Click on a user document (e.g., `demo_user_1759649339687`)
   - See all subcollections: templates, recruiters, assignments, etc.

3. **View Subcollections:**
   - Subcollections are NOT visible at root level
   - You must click into a user document to see them
   - This is normal Firestore behavior!

---

## 📊 **Firestore Data Structure**

```
Firestore Database
│
└── users/ (collection)
    │
    ├── demo_user_1759649339687/ (document)
    │   ├── email: "demo@ecold.com"
    │   ├── name: "Demo User"
    │   ├── provider: "GOOGLE"
    │   │
    │   ├── templates/ (subcollection)
    │   │   ├── template_outreach
    │   │   ├── template_followup
    │   │   └── template_thankyou
    │   │
    │   ├── recruiters/ (subcollection)
    │   │   ├── recruiter_john
    │   │   ├── recruiter_sarah
    │   │   ├── recruiter_mike
    │   │   └── recruiter_emily
    │   │
    │   ├── assignments/ (subcollection)
    │   ├── scheduled_emails/ (subcollection)
    │   ├── email_logs/ (subcollection)
    │   ├── incoming_emails/ (subcollection)
    │   └── resumes/ (subcollection)
    │
    └── [other users...]
```

---

## 🔧 **Common Operations**

### **Query Data**

```javascript
// Get all recruiters for a user
const recruitersSnapshot = await db
  .collection('users')
  .doc(userId)
  .collection('recruiters')
  .get();

// Get recruiters by status
const activeRecruiters = await db
  .collection('users')
  .doc(userId)
  .collection('recruiters')
  .where('status', '==', 'ACTIVE')
  .get();
```

### **Create Document**

```javascript
// Add new template
await db
  .collection('users')
  .doc(userId)
  .collection('templates')
  .add({
    name: 'New Template',
    subject: 'Subject',
    body: 'Body',
    type: 'OUTREACH',
    isDefault: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });
```

### **Update Document**

```javascript
// Update recruiter status
await db
  .collection('users')
  .doc(userId)
  .collection('recruiters')
  .doc(recruiterId)
  .update({
    status: 'CONTACTED',
    lastContactedAt: admin.firestore.FieldValue.serverTimestamp()
  });
```

### **Delete Document**

```javascript
// Delete template
await db
  .collection('users')
  .doc(userId)
  .collection('templates')
  .doc(templateId)
  .delete();
```

---

## 🛠️ **Troubleshooting**

### **Issue: Can't see subcollections in Firebase Console**

**Solution:** Subcollections don't appear at root level. Click into a user document first.

1. Click `users` collection
2. Click a user document (e.g., `demo_user_1759649339687`)
3. Scroll down to see subcollections

---

### **Issue: Firestore credentials not found**

**Error:** `Firestore credentials file not found`

**Solution:**
```bash
# Ensure firestore-key.json is in the right location
cp database/firestore-key.json backend/src/main/resources/
```

---

### **Issue: Permission denied**

**Error:** `Missing or insufficient permissions`

**Solution:**
1. Deploy security rules: `firebase deploy --only firestore:rules`
2. Ensure user is authenticated
3. Check that userId matches the document path

---

### **Issue: Index not found**

**Error:** `The query requires an index`

**Solution:**
```bash
# Deploy composite indexes
firebase deploy --only firestore:indexes

# Wait 2-5 minutes for indexes to build
```

---

## 📈 **Monitoring Usage**

### **Check Firestore Usage**

1. **Firebase Console:**
   - Go to: https://console.firebase.google.com/project/ecold-app-d3990/usage
   - View: Reads, Writes, Deletes, Storage

2. **CLI:**
```bash
firebase firestore:usage
```

### **Free Tier Limits**

- **Reads:** 50,000/day
- **Writes:** 20,000/day
- **Deletes:** 20,000/day
- **Storage:** 1 GiB

---

## 🔐 **Security Rules Reference**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Helper: Check if user is authenticated
    function isAuthenticated() {
      return request.auth != null;
    }

    // Helper: Check if user owns the document
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    // Users can only access their own data
    match /users/{userId} {
      allow read, write: if isOwner(userId);

      // All subcollections inherit this rule
      match /{subcollection}/{document=**} {
        allow read, write: if isOwner(userId);
      }
    }
  }
}
```

---

## 📚 **Useful Commands**

```bash
# Firebase CLI
firebase login                          # Login to Firebase
firebase projects:list                  # List all projects
firebase use ecold-app-d3990           # Set active project

# Firestore
firebase deploy --only firestore:rules  # Deploy security rules
firebase deploy --only firestore:indexes # Deploy indexes
firebase firestore:delete /path         # Delete data (careful!)

# Database
cd database
node initialize-firestore-schema.js     # Initialize schema
node verify-firestore-data.js          # Verify data

# Backup (manual)
node scripts/local-firestore-backup.js  # Create local backup
```

---

## 🎯 **Best Practices**

### **1. Always Use Subcollections**
✅ **Good:** `/users/{userId}/templates/{templateId}`
❌ **Bad:** `/templates/{templateId}` (with userId field)

**Why:** Better security, data isolation, and automatic cleanup

---

### **2. Use Server Timestamps**
```javascript
// ✅ Good
createdAt: admin.firestore.FieldValue.serverTimestamp()

// ❌ Bad
createdAt: new Date()  // Client time can be wrong
```

---

### **3. Batch Write Operations**
```javascript
// ✅ Good - Batch writes
const batch = db.batch();
batch.set(ref1, data1);
batch.set(ref2, data2);
await batch.commit();

// ❌ Bad - Individual writes
await ref1.set(data1);
await ref2.set(data2);
```

---

### **4. Use Composite Indexes**
Always create composite indexes for queries with multiple filters:
```javascript
// This query needs a composite index
.where('status', '==', 'ACTIVE')
.orderBy('createdAt', 'desc')
```

---

## 🔗 **Useful Links**

- **Firebase Console:** https://console.firebase.google.com/project/ecold-app-d3990
- **Firestore Docs:** https://firebase.google.com/docs/firestore
- **Security Rules:** https://firebase.google.com/docs/firestore/security/get-started
- **Indexes:** https://firebase.google.com/docs/firestore/query-data/indexing

---

## ✅ **Quick Checklist**

- [ ] Firebase credentials in `backend/src/main/resources/firestore-key.json`
- [ ] Run `node initialize-firestore-schema.js`
- [ ] Deploy security rules: `firebase deploy --only firestore:rules`
- [ ] Deploy indexes: `firebase deploy --only firestore:indexes`
- [ ] Verify data: `node verify-firestore-data.js`
- [ ] Check Firebase Console for data visibility
- [ ] Start application and test endpoints

---

**You're all set! 🎉**

ECold is now running on Firebase Firestore with **zero monthly database costs**!
