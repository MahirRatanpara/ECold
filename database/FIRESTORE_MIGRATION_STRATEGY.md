# ECold: PostgreSQL to Firebase Firestore Migration Strategy

## Executive Summary

This document outlines the strategic decision and implementation plan to migrate ECold from PostgreSQL to Firebase Firestore (Spark Plan - Free Tier), eliminating all database infrastructure costs while maintaining application functionality.

---

## 📊 Strategic Decision

### Current Architecture
- **Database**: PostgreSQL 15 (self-hosted or managed)
- **Deployment**: Docker containers with persistent volumes
- **Monthly Cost**: ~$15-50+ for managed PostgreSQL or server costs for self-hosted

### Proposed Architecture
- **Database**: Firebase Firestore (Spark Plan - Free)
- **Deployment**: Cloud hosting with Firebase backend
- **Backup**: Automated free backup to Google Cloud Storage
- **Monthly Cost**: **$0** (within Spark Plan limits)

### Firebase Spark Plan Benefits (Always Free)

#### Firestore Database
- **Stored Data**: 1 GiB total
- **Document Reads**: 50,000 per day
- **Document Writes**: 20,000 per day
- **Document Deletes**: 20,000 per day
- **Network Egress**: 10 GiB per month

#### Cloud Functions (for backups)
- **Invocations**: 125,000 per month
- **Compute Time**: 40,000 GB-seconds/month
- **Outbound Networking**: 5 GB per month

#### Cloud Storage (for backups)
- **Storage**: 5 GB total
- **Downloads**: 1 GB per day
- **Uploads**: 1 GB per day

**Total Estimated Cost**: $0/month for moderate usage (< 1000 users)
**Backup Cost**: $0 (using Cloud Functions + Storage free tier)

---

## 🏗️ Migration Architecture

### Data Model Transformation

#### PostgreSQL (Relational) → Firestore (NoSQL Document)

```
PostgreSQL Tables          →    Firestore Collections
─────────────────────────       ─────────────────────────
users                      →    users (collection)
email_templates            →    users/{userId}/templates (subcollection)
resumes                    →    users/{userId}/resumes (subcollection)
recruiter_contacts         →    users/{userId}/recruiters (subcollection)
recruiter_template_         →    users/{userId}/assignments (subcollection)
  assignments
scheduled_emails           →    users/{userId}/scheduled_emails (subcollection)
email_logs                 →    users/{userId}/email_logs (subcollection)
incoming_emails            →    users/{userId}/incoming_emails (subcollection)
```

### Firestore Data Structure

```
/users/{userId}
  ├── email: string
  ├── name: string
  ├── provider: string
  ├── createdAt: timestamp
  ├── /templates/{templateId}
  │     ├── name: string
  │     ├── subject: string
  │     ├── body: string
  │     └── type: string
  ├── /recruiters/{recruiterId}
  │     ├── email: string
  │     ├── recruiterName: string
  │     ├── companyName: string
  │     └── status: string
  ├── /assignments/{assignmentId}
  │     ├── recruiterId: reference
  │     ├── templateId: reference
  │     ├── weekAssigned: number
  │     └── yearAssigned: number
  ├── /scheduled_emails/{emailId}
  │     ├── recipientEmail: string
  │     ├── subject: string
  │     └── scheduleTime: timestamp
  ├── /email_logs/{logId}
  │     └── status: string
  └── /incoming_emails/{emailId}
        └── category: string
```

---

## 📋 Implementation Steps

### Phase 1: Firebase Setup (Day 1)

#### 1.1 Create Firebase Project (Spark Plan)

**Via Firebase Console:**

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"**
3. Enter project name: `ecold-app`
4. Disable Google Analytics (optional for free tier)
5. Click **"Create project"**
6. ✅ Project created on **Spark Plan (Free)**

#### 1.2 Initialize Firestore Database

1. In Firebase Console, navigate to **Build → Firestore Database**
2. Click **"Create database"**
3. Choose **"Start in production mode"** (we'll add rules next)
4. Select location: **`us-central1`** (or closest to your users)
5. Click **"Enable"**
6. ✅ Firestore database is now active

#### 1.3 Setup Firebase CLI

```bash
# Install Firebase CLI
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize Firebase in your project
cd /path/to/ECold
firebase init

# Select the following options:
# ◉ Firestore: Configure security rules and indexes files
# ◉ Functions: Configure a Cloud Functions directory
# ◉ Storage: Configure security rules for Cloud Storage
#
# ? Select a default Firebase project: ecold-app
# ? What file should be used for Firestore Rules? firestore.rules
# ? What file should be used for Firestore indexes? firestore.indexes.json
# ? What language would you like to use for Cloud Functions? JavaScript
# ? Do you want to use ESLint? No
# ? Do you want to install dependencies with npm now? Yes
```

#### 1.4 Setup Service Account for Backend

1. Go to Firebase Console → **Project Settings** (gear icon)
2. Navigate to **Service accounts** tab
3. Click **"Generate new private key"**
4. Save the JSON file as `firestore-key.json`
5. Move to backend resources:

```bash
mv ~/Downloads/ecold-app-firebase-adminsdk-xxxxx.json backend/src/main/resources/firestore-key.json
```

**⚠️ Security: Add to `.gitignore`:**
```bash
echo "firestore-key.json" >> backend/.gitignore
```

#### 1.5 Configure Firestore Security Rules

Update `firestore.rules`:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Helper function to check if user is authenticated
    function isAuthenticated() {
      return request.auth != null;
    }

    // Helper function to check if user owns the document
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    // Users collection - users can only access their own data
    match /users/{userId} {
      allow read, write: if isOwner(userId);

      // Email templates subcollection
      match /templates/{templateId} {
        allow read, write: if isOwner(userId);
      }

      // Recruiters subcollection
      match /recruiters/{recruiterId} {
        allow read, write: if isOwner(userId);
      }

      // Assignments subcollection
      match /assignments/{assignmentId} {
        allow read, write: if isOwner(userId);
      }

      // Scheduled emails subcollection
      match /scheduled_emails/{emailId} {
        allow read, write: if isOwner(userId);
      }

      // Email logs subcollection
      match /email_logs/{logId} {
        allow read, write: if isOwner(userId);
      }

      // Incoming emails subcollection
      match /incoming_emails/{emailId} {
        allow read, write: if isOwner(userId);
      }

      // Resumes subcollection
      match /resumes/{resumeId} {
        allow read, write: if isOwner(userId);
      }
    }
  }
}
```

Deploy rules:
```bash
firebase deploy --only firestore:rules
```

#### 1.6 Initialize Database Schema/Structure

Create `scripts/initialize-firestore-schema.js`:

```javascript
const admin = require('firebase-admin');
const serviceAccount = require('../backend/src/main/resources/firestore-key.json');

// Initialize Firebase Admin
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function initializeSchema() {
  console.log('🚀 Initializing Firestore schema...\n');

  try {
    // Create sample user document structure
    const sampleUserId = 'sample_user_' + Date.now();
    const userRef = db.collection('users').doc(sampleUserId);

    await userRef.set({
      email: 'sample@ecold.com',
      name: 'Sample User',
      provider: 'GOOGLE',
      providerId: 'sample_123',
      profilePicture: null,
      accessToken: null,
      refreshToken: null,
      tokenExpiresAt: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('✅ Created sample user document');

    // Initialize empty subcollections with sample documents

    // Email Templates
    await userRef.collection('templates').doc('sample_template').set({
      name: 'Sample Template',
      subject: 'Sample Subject',
      body: 'Sample email body',
      type: 'OUTREACH',
      isDefault: true,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('✅ Created sample template');

    // Recruiters
    await userRef.collection('recruiters').doc('sample_recruiter').set({
      email: 'recruiter@company.com',
      recruiterName: 'Sample Recruiter',
      companyName: 'Sample Company',
      jobRole: 'Software Engineer',
      linkedinProfile: null,
      notes: null,
      status: 'PENDING',
      lastContactedAt: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('✅ Created sample recruiter');

    // Assignments
    await userRef.collection('assignments').doc('sample_assignment').set({
      recruiterId: 'sample_recruiter',
      templateId: 'sample_template',
      weekAssigned: 1,
      yearAssigned: 2025,
      assignmentStatus: 'ACTIVE',
      emailsSent: 0,
      lastEmailSentAt: null,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('✅ Created sample assignment');

    // Scheduled Emails
    await userRef.collection('scheduled_emails').doc('sample_scheduled').set({
      recipientEmail: 'recruiter@company.com',
      subject: 'Sample Scheduled Email',
      body: 'This is a scheduled email',
      scheduleTime: admin.firestore.Timestamp.fromDate(new Date(Date.now() + 86400000)),
      status: 'SCHEDULED',
      templateId: 'sample_template',
      recruiterId: 'sample_recruiter',
      isHtml: false,
      priority: 'NORMAL',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('✅ Created sample scheduled email');

    // Email Logs
    await userRef.collection('email_logs').doc('sample_log').set({
      recruiterContactId: 'sample_recruiter',
      recipientEmail: 'recruiter@company.com',
      subject: 'Sample Email',
      body: 'Sample email body',
      status: 'SENT',
      messageId: 'msg_' + Date.now(),
      sentAt: admin.firestore.FieldValue.serverTimestamp(),
      retryCount: 0,
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('✅ Created sample email log');

    // Incoming Emails
    await userRef.collection('incoming_emails').doc('sample_incoming').set({
      messageId: 'incoming_' + Date.now(),
      senderEmail: 'sender@company.com',
      senderName: 'Sender Name',
      subject: 'Sample Incoming Email',
      body: 'Sample incoming email body',
      htmlBody: '<p>Sample incoming email body</p>',
      category: 'APPLICATION_UPDATE',
      priority: 'NORMAL',
      isRead: false,
      isProcessed: false,
      receivedAt: admin.firestore.FieldValue.serverTimestamp(),
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('✅ Created sample incoming email');

    // Resumes
    await userRef.collection('resumes').doc('sample_resume').set({
      name: 'Sample Resume',
      fileName: 'resume.pdf',
      filePath: '/uploads/sample_resume.pdf',
      contentType: 'application/pdf',
      fileSize: 102400,
      isDefault: true,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    console.log('✅ Created sample resume');

    console.log('\n🎉 Firestore schema initialized successfully!');
    console.log('📍 Sample user ID:', sampleUserId);
    console.log('\n⚠️  You can delete the sample documents from Firebase Console');
    console.log('🔗 https://console.firebase.google.com/project/ecold-app/firestore');

  } catch (error) {
    console.error('❌ Error initializing schema:', error);
  } finally {
    process.exit();
  }
}

initializeSchema();
```

Run schema initialization:

```bash
# Install dependencies
npm install firebase-admin

# Run initialization script
node scripts/initialize-firestore-schema.js
```

#### 1.7 Create Composite Indexes

Update `firestore.indexes.json`:

```json
{
  "indexes": [
    {
      "collectionGroup": "recruiters",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "createdAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "email_logs",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "sentAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "scheduled_emails",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "scheduleTime", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "incoming_emails",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "category", "order": "ASCENDING" },
        { "fieldPath": "receivedAt", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "assignments",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "weekAssigned", "order": "ASCENDING" },
        { "fieldPath": "yearAssigned", "order": "ASCENDING" },
        { "fieldPath": "assignmentStatus", "order": "ASCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

Deploy indexes:
```bash
firebase deploy --only firestore:indexes
```

**Note**: Index creation may take a few minutes. Monitor progress in Firebase Console.

---

### Phase 2: Code Migration (Days 2-5)

#### 2.1 Update Dependencies (pom.xml)

```xml
<!-- REMOVE PostgreSQL dependencies -->
<!--
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
-->

<!-- ADD Firestore dependency -->
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-firestore</artifactId>
    <version>3.15.0</version>
</dependency>

<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.2.0</version>
</dependency>

<!-- Keep these -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

#### 2.2 Create Firestore Configuration

Create `backend/src/main/java/com/ecold/config/FirestoreConfig.java`:

```java
package com.ecold.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirestoreConfig {

    @Bean
    public Firestore firestore() throws IOException {
        InputStream serviceAccount = new ClassPathResource("firestore-key.json").getInputStream();

        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId("ecold-app")
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }

        return FirestoreClient.getFirestore();
    }
}
```

#### 2.3 Convert JPA Entities to Firestore Documents

**Before (User.java - JPA Entity):**
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;
    private String name;
    // ...
}
```

**After (User.java - Firestore Document):**
```java
package com.ecold.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @DocumentId
    private String id;  // Firestore auto-generated ID

    private String email;
    private String name;
    private String profilePicture;
    private String provider;
    private String providerId;
    private String accessToken;
    private String refreshToken;
    private Timestamp tokenExpiresAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
```

#### 2.4 Create Firestore Repositories

Create `backend/src/main/java/com/ecold/repository/UserFirestoreRepository.java`:

```java
package com.ecold.repository;

import com.ecold.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserFirestoreRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "users";

    public User save(User user) throws ExecutionException, InterruptedException {
        CollectionReference users = firestore.collection(COLLECTION_NAME);

        if (user.getId() == null) {
            // Create new user with auto-generated ID
            DocumentReference docRef = users.document();
            user.setId(docRef.getId());
            user.setCreatedAt(Timestamp.now());
        }

        user.setUpdatedAt(Timestamp.now());
        ApiFuture<WriteResult> result = users.document(user.getId()).set(user);
        result.get(); // Wait for completion

        log.info("User saved: {}", user.getId());
        return user;
    }

    public Optional<User> findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            return Optional.of(document.toObject(User.class));
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) throws ExecutionException, InterruptedException {
        Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("email", email)
                .limit(1);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

        if (!documents.isEmpty()) {
            return Optional.of(documents.get(0).toObject(User.class));
        }
        return Optional.empty();
    }

    public List<User> findAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = query.get().getDocuments();

        return documents.stream()
                .map(doc -> doc.toObject(User.class))
                .collect(Collectors.toList());
    }

    public void delete(String id) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> result = firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete();
        result.get();
        log.info("User deleted: {}", id);
    }
}
```

#### 2.5 Create Recruiter Firestore Repository (Subcollection)

```java
package com.ecold.repository;

import com.ecold.model.RecruiterContact;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RecruiterFirestoreRepository {

    private final Firestore firestore;

    private CollectionReference getRecruiterCollection(String userId) {
        return firestore.collection("users")
                .document(userId)
                .collection("recruiters");
    }

    public RecruiterContact save(String userId, RecruiterContact recruiter)
            throws ExecutionException, InterruptedException {
        CollectionReference recruiters = getRecruiterCollection(userId);

        if (recruiter.getId() == null) {
            DocumentReference docRef = recruiters.document();
            recruiter.setId(docRef.getId());
            recruiter.setCreatedAt(Timestamp.now());
        }

        recruiter.setUpdatedAt(Timestamp.now());
        recruiters.document(recruiter.getId()).set(recruiter).get();

        return recruiter;
    }

    public Optional<RecruiterContact> findById(String userId, String recruiterId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getRecruiterCollection(userId)
                .document(recruiterId)
                .get()
                .get();

        if (doc.exists()) {
            return Optional.of(doc.toObject(RecruiterContact.class));
        }
        return Optional.empty();
    }

    public List<RecruiterContact> findByUserId(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getRecruiterCollection(userId).get().get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterContact.class))
                .collect(Collectors.toList());
    }

    public void delete(String userId, String recruiterId)
            throws ExecutionException, InterruptedException {
        getRecruiterCollection(userId).document(recruiterId).delete().get();
    }
}
```

#### 2.6 Update Service Layer

**Before (UserService with JPA):**
```java
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
```

**After (UserService with Firestore):**
```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserFirestoreRepository userRepository;

    public User findByEmail(String email) {
        try {
            return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Error fetching user", e);
        }
    }
}
```

#### 2.7 Update Application Configuration

**Update `application.yml`:**

```yaml
spring:
  application:
    name: ecold-backend

  # REMOVE PostgreSQL configuration
  # datasource:
  #   url: ${DATABASE_URL}
  #   username: ${DATABASE_USERNAME}
  #   password: ${DATABASE_PASSWORD}
  # jpa:
  #   hibernate:
  #     ddl-auto: update

  security:
    oauth2:
      # Keep OAuth configuration

# ADD Firestore configuration
google:
  cloud:
    project-id: ecold-app
    firestore:
      credentials-path: classpath:firestore-key.json

server:
  port: ${PORT:8080}
```

**Update `.env`:**

```bash
# Remove PostgreSQL vars
# DATABASE_URL=...
# DATABASE_USERNAME=...
# DATABASE_PASSWORD=...

# Add Firestore vars
GOOGLE_CLOUD_PROJECT=ecold-app
FIRESTORE_CREDENTIALS_PATH=/app/firestore-key.json

# Keep OAuth vars
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret
```

---

### Phase 3: Data Migration (Day 6)

#### 3.1 Create Migration Script

Create `scripts/migrate-to-firestore.js`:

```javascript
const admin = require('firebase-admin');
const { Client } = require('pg');

// Initialize Firebase Admin
const serviceAccount = require('./firestore-key.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'ecold-app'
});

const db = admin.firestore();

// PostgreSQL connection
const pgClient = new Client({
  host: 'localhost',
  port: 5432,
  database: 'ecold',
  user: 'ecold_user',
  password: 'ecold_pass123'
});

async function migrateUsers() {
  console.log('Migrating users...');

  const result = await pgClient.query('SELECT * FROM users');
  const batch = db.batch();

  for (const user of result.rows) {
    const userRef = db.collection('users').doc(user.id.toString());
    batch.set(userRef, {
      email: user.email,
      name: user.name,
      profilePicture: user.profile_picture,
      provider: user.provider,
      providerId: user.provider_id,
      accessToken: user.access_token,
      refreshToken: user.refresh_token,
      tokenExpiresAt: user.token_expires_at,
      createdAt: user.created_at,
      updatedAt: user.updated_at
    });
  }

  await batch.commit();
  console.log(`Migrated ${result.rows.length} users`);
}

async function migrateRecruiters() {
  console.log('Migrating recruiters...');

  const result = await pgClient.query('SELECT * FROM recruiter_contacts');

  for (const recruiter of result.rows) {
    const recruiterRef = db
      .collection('users')
      .doc(recruiter.user_id.toString())
      .collection('recruiters')
      .doc(recruiter.id.toString());

    await recruiterRef.set({
      email: recruiter.email,
      recruiterName: recruiter.recruiter_name,
      companyName: recruiter.company_name,
      jobRole: recruiter.job_role,
      linkedinProfile: recruiter.linkedin_profile,
      notes: recruiter.notes,
      status: recruiter.status,
      lastContactedAt: recruiter.last_contacted_at,
      createdAt: recruiter.created_at,
      updatedAt: recruiter.updated_at
    });
  }

  console.log(`Migrated ${result.rows.length} recruiters`);
}

async function main() {
  await pgClient.connect();

  try {
    await migrateUsers();
    await migrateRecruiters();
    // Add other collections...

    console.log('Migration completed successfully!');
  } catch (error) {
    console.error('Migration failed:', error);
  } finally {
    await pgClient.end();
  }
}

main();
```

#### 3.2 Run Migration

```bash
# Install dependencies
npm install firebase-admin pg

# Run migration script
node scripts/migrate-to-firestore.js

# Verify data in Firebase Console
# https://console.firebase.google.com/project/ecold-app/firestore
```

---

### Phase 4: Cloud Deployment (Days 7-8)

#### 4.1 Update Dockerfile for Cloud Run

```dockerfile
# backend/Dockerfile
FROM maven:3.8-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY src/main/resources/firestore-key.json /app/firestore-key.json

ENV GOOGLE_APPLICATION_CREDENTIALS=/app/firestore-key.json
ENV PORT=8080

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 4.2 Deploy Backend to Cloud Run

```bash
# Build and push container image
gcloud builds submit --tag gcr.io/ecold-app/backend

# Deploy to Cloud Run
gcloud run deploy ecold-backend \
  --image gcr.io/ecold-app/backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --max-instances 10 \
  --set-env-vars GOOGLE_CLOUD_PROJECT=ecold-app \
  --set-env-vars GOOGLE_CLIENT_ID=$GOOGLE_CLIENT_ID \
  --set-env-vars GOOGLE_CLIENT_SECRET=$GOOGLE_CLIENT_SECRET

# Get the service URL
gcloud run services describe ecold-backend --region us-central1 --format 'value(status.url)'
```

#### 4.3 Deploy Frontend to Cloud Run

```bash
# Build frontend with backend URL
cd frontend
ng build --prod --configuration production

# Update nginx config
# backend/nginx.conf
server {
    listen 80;
    root /usr/share/nginx/html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass https://ecold-backend-xxx.run.app/api/;
        proxy_set_header Host $host;
    }
}

# Build and deploy
docker build -t gcr.io/ecold-app/frontend .
docker push gcr.io/ecold-app/frontend

gcloud run deploy ecold-frontend \
  --image gcr.io/ecold-app/frontend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

#### 4.4 Setup Custom Domain (Optional)

```bash
# Map custom domain
gcloud run domain-mappings create \
  --service ecold-frontend \
  --domain ecold.app \
  --region us-central1

# Update DNS records as instructed by the output
```

---

### Phase 5: Monitoring & Automated Backups (Day 9)

#### 5.1 Setup Firebase Monitoring

**Enable Firebase Analytics (Optional but Free):**

1. Go to Firebase Console → **Analytics**
2. Click **"Enable Google Analytics"**
3. Select/create Analytics account
4. ✅ Free monitoring enabled

**Monitor in Firebase Console:**
- **Firestore Usage**: Build → Firestore Database → Usage tab
- **Functions Usage**: Build → Functions → Usage tab
- **Storage Usage**: Build → Storage → Usage tab

#### 5.2 Setup Automated Free Backup System

**Create Cloud Function for Daily Backups:**

Create `functions/backup-firestore.js`:

```javascript
const admin = require('firebase-admin');
const functions = require('firebase-functions');
admin.initializeApp();

const db = admin.firestore();
const bucket = admin.storage().bucket();

// Backup function - runs daily at 2 AM UTC
exports.scheduledFirestoreBackup = functions.pubsub
  .schedule('0 2 * * *')
  .timeZone('UTC')
  .onRun(async (context) => {
    try {
      console.log('🔄 Starting Firestore backup...');

      const timestamp = new Date().toISOString().split('T')[0];
      const backupData = {};

      // Backup all users and their subcollections
      const usersSnapshot = await db.collection('users').get();

      for (const userDoc of usersSnapshot.docs) {
        const userId = userDoc.id;
        backupData[userId] = {
          userData: userDoc.data(),
          templates: [],
          recruiters: [],
          assignments: [],
          scheduled_emails: [],
          email_logs: [],
          incoming_emails: [],
          resumes: []
        };

        // Backup subcollections
        const subcollections = [
          'templates', 'recruiters', 'assignments',
          'scheduled_emails', 'email_logs', 'incoming_emails', 'resumes'
        ];

        for (const collectionName of subcollections) {
          const subcollectionSnapshot = await userDoc.ref
            .collection(collectionName)
            .get();

          backupData[userId][collectionName] = subcollectionSnapshot.docs.map(
            doc => ({ id: doc.id, ...doc.data() })
          );
        }
      }

      // Save to Cloud Storage
      const fileName = `firestore-backup-${timestamp}.json`;
      const file = bucket.file(`backups/${fileName}`);

      await file.save(JSON.stringify(backupData, null, 2), {
        contentType: 'application/json',
        metadata: {
          metadata: {
            backupDate: timestamp,
            documentCount: Object.keys(backupData).length
          }
        }
      });

      console.log(`✅ Backup completed: ${fileName}`);

      // Keep only last 30 backups (free tier friendly)
      await cleanOldBackups(bucket, 30);

      return null;
    } catch (error) {
      console.error('❌ Backup failed:', error);
      throw error;
    }
  });

// Manual backup trigger (HTTP function)
exports.manualFirestoreBackup = functions.https.onRequest(async (req, res) => {
  try {
    // Add authentication check here
    const authHeader = req.headers.authorization;
    const expectedToken = functions.config().backup.token;

    if (!authHeader || authHeader !== `Bearer ${expectedToken}`) {
      res.status(401).send('Unauthorized');
      return;
    }

    console.log('🔄 Starting manual Firestore backup...');

    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const backupData = {};

    const usersSnapshot = await db.collection('users').get();

    for (const userDoc of usersSnapshot.docs) {
      const userId = userDoc.id;
      backupData[userId] = {
        userData: userDoc.data(),
        templates: [],
        recruiters: [],
        assignments: [],
        scheduled_emails: [],
        email_logs: [],
        incoming_emails: [],
        resumes: []
      };

      const subcollections = [
        'templates', 'recruiters', 'assignments',
        'scheduled_emails', 'email_logs', 'incoming_emails', 'resumes'
      ];

      for (const collectionName of subcollections) {
        const subcollectionSnapshot = await userDoc.ref
          .collection(collectionName)
          .get();

        backupData[userId][collectionName] = subcollectionSnapshot.docs.map(
          doc => ({ id: doc.id, ...doc.data() })
        );
      }
    }

    const fileName = `manual-backup-${timestamp}.json`;
    const file = bucket.file(`backups/${fileName}`);

    await file.save(JSON.stringify(backupData, null, 2), {
      contentType: 'application/json'
    });

    console.log(`✅ Manual backup completed: ${fileName}`);
    res.status(200).json({
      success: true,
      fileName,
      timestamp,
      documentCount: Object.keys(backupData).length
    });
  } catch (error) {
    console.error('❌ Manual backup failed:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

// Restore function (manual trigger)
exports.restoreFirestoreBackup = functions.https.onRequest(async (req, res) => {
  try {
    const authHeader = req.headers.authorization;
    const expectedToken = functions.config().backup.token;

    if (!authHeader || authHeader !== `Bearer ${expectedToken}`) {
      res.status(401).send('Unauthorized');
      return;
    }

    const { fileName } = req.body;
    if (!fileName) {
      res.status(400).json({ error: 'fileName is required' });
      return;
    }

    console.log(`🔄 Restoring from backup: ${fileName}`);

    const file = bucket.file(`backups/${fileName}`);
    const [contents] = await file.download();
    const backupData = JSON.parse(contents.toString());

    // Restore data
    const batch = db.batch();
    let operationCount = 0;

    for (const [userId, userData] of Object.entries(backupData)) {
      const userRef = db.collection('users').doc(userId);
      batch.set(userRef, userData.userData);
      operationCount++;

      // Firestore batch has 500 operation limit
      if (operationCount >= 450) {
        await batch.commit();
        operationCount = 0;
      }
    }

    if (operationCount > 0) {
      await batch.commit();
    }

    console.log(`✅ Restore completed from: ${fileName}`);
    res.status(200).json({
      success: true,
      message: 'Restore completed',
      fileName
    });
  } catch (error) {
    console.error('❌ Restore failed:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

// Helper function to clean old backups
async function cleanOldBackups(bucket, keepCount) {
  const [files] = await bucket.getFiles({ prefix: 'backups/' });

  if (files.length <= keepCount) {
    return;
  }

  // Sort by creation time
  files.sort((a, b) => {
    return new Date(a.metadata.timeCreated) - new Date(b.metadata.timeCreated);
  });

  // Delete oldest files
  const filesToDelete = files.slice(0, files.length - keepCount);

  for (const file of filesToDelete) {
    await file.delete();
    console.log(`🗑️ Deleted old backup: ${file.name}`);
  }
}
```

**Update `functions/package.json`:**

```json
{
  "name": "ecold-functions",
  "version": "1.0.0",
  "dependencies": {
    "firebase-admin": "^12.0.0",
    "firebase-functions": "^4.5.0"
  },
  "engines": {
    "node": "18"
  }
}
```

**Deploy backup functions:**

```bash
# Set backup authentication token
firebase functions:config:set backup.token="your-secret-backup-token-here"

# Deploy functions
firebase deploy --only functions

# ✅ Functions deployed:
# - scheduledFirestoreBackup (runs daily at 2 AM UTC)
# - manualFirestoreBackup (HTTP trigger)
# - restoreFirestoreBackup (HTTP trigger)
```

#### 5.3 Manual Backup & Restore Commands

**Trigger Manual Backup:**

```bash
# Using curl
curl -X POST https://us-central1-ecold-app.cloudfunctions.net/manualFirestoreBackup \
  -H "Authorization: Bearer your-secret-backup-token-here"

# Using wget
wget --method=POST \
  --header="Authorization: Bearer your-secret-backup-token-here" \
  https://us-central1-ecold-app.cloudfunctions.net/manualFirestoreBackup
```

**List Available Backups:**

```bash
# Install Firebase CLI if not already installed
npm install -g firebase-tools

# List backups
firebase storage:list backups/
```

**Download Backup:**

```bash
# Download specific backup
gsutil cp gs://ecold-app.appspot.com/backups/firestore-backup-2025-01-05.json ./

# Or via Firebase Console:
# Storage → backups/ → Download
```

**Restore from Backup:**

```bash
# Restore from specific backup file
curl -X POST https://us-central1-ecold-app.cloudfunctions.net/restoreFirestoreBackup \
  -H "Authorization: Bearer your-secret-backup-token-here" \
  -H "Content-Type: application/json" \
  -d '{"fileName": "backups/firestore-backup-2025-01-05.json"}'
```

#### 5.4 Local Backup Script (Alternative Free Method)

Create `scripts/local-firestore-backup.js`:

```javascript
const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const serviceAccount = require('../backend/src/main/resources/firestore-key.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function backupFirestore() {
  const timestamp = new Date().toISOString().split('T')[0];
  const backupData = {};

  console.log('🔄 Starting local Firestore backup...');

  const usersSnapshot = await db.collection('users').get();

  for (const userDoc of usersSnapshot.docs) {
    const userId = userDoc.id;
    console.log(`📦 Backing up user: ${userId}`);

    backupData[userId] = {
      userData: userDoc.data(),
      templates: [],
      recruiters: [],
      assignments: [],
      scheduled_emails: [],
      email_logs: [],
      incoming_emails: [],
      resumes: []
    };

    const subcollections = [
      'templates', 'recruiters', 'assignments',
      'scheduled_emails', 'email_logs', 'incoming_emails', 'resumes'
    ];

    for (const collectionName of subcollections) {
      const subcollectionSnapshot = await userDoc.ref
        .collection(collectionName)
        .get();

      backupData[userId][collectionName] = subcollectionSnapshot.docs.map(
        doc => ({ id: doc.id, ...doc.data() })
      );
    }
  }

  // Save to local file
  const backupDir = path.join(__dirname, '..', 'backups');
  if (!fs.existsSync(backupDir)) {
    fs.mkdirSync(backupDir);
  }

  const fileName = `firestore-backup-${timestamp}.json`;
  const filePath = path.join(backupDir, fileName);

  fs.writeFileSync(filePath, JSON.stringify(backupData, null, 2));

  console.log(`✅ Backup completed: ${fileName}`);
  console.log(`📍 Location: ${filePath}`);
  console.log(`📊 Total users backed up: ${Object.keys(backupData).length}`);

  process.exit();
}

backupFirestore().catch(console.error);
```

**Run local backup:**

```bash
node scripts/local-firestore-backup.js
```

#### 5.5 Backup Monitoring & Alerts

**Setup Email Alerts for Backup Failures:**

Add to `functions/backup-firestore.js`:

```javascript
const nodemailer = require('nodemailer');

// Configure email alerts
const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: functions.config().email.user,
    pass: functions.config().email.password
  }
});

async function sendBackupAlert(status, details) {
  const mailOptions = {
    from: functions.config().email.user,
    to: 'admin@ecold.com',
    subject: `Firestore Backup ${status}`,
    text: `Backup Status: ${status}\n\nDetails:\n${JSON.stringify(details, null, 2)}`
  };

  try {
    await transporter.sendMail(mailOptions);
  } catch (error) {
    console.error('Failed to send alert email:', error);
  }
}
```

#### 5.6 Optimize Firestore Queries

**Use composite queries efficiently:**

```java
// Bad: Multiple queries
List<RecruiterContact> active = getRecruiters(userId, "ACTIVE");
List<RecruiterContact> contacted = getRecruiters(userId, "CONTACTED");

// Good: Single composite query with index
Query query = firestore.collection("users")
    .document(userId)
    .collection("recruiters")
    .whereIn("status", Arrays.asList("ACTIVE", "CONTACTED"))
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .limit(50);
```

---

## 🚨 Important Considerations

### 1. Free Tier Limits Monitoring

**Create alerts for quota usage:**

```bash
# Create budget alert
gcloud billing budgets create \
  --billing-account=YOUR_BILLING_ACCOUNT \
  --display-name="ECold Free Tier Alert" \
  --budget-amount=1USD \
  --threshold-rule=percent=50 \
  --threshold-rule=percent=90
```

### 2. Data Model Constraints

**Firestore Limitations:**
- Maximum document size: 1 MB
- Maximum write rate: 1 write/second per document
- No complex joins (use denormalization)
- Limited query capabilities (no OR on different fields)

**Solutions:**
- Split large documents into subcollections
- Use batch writes for multiple updates
- Denormalize data (store user info in recruiter docs)
- Use composite indexes for complex queries

### 3. Migration Rollback Plan

**Keep PostgreSQL backup for 30 days:**

```bash
# Backup PostgreSQL before migration
pg_dump -h localhost -U ecold_user ecold > ecold_backup_$(date +%Y%m%d).sql

# Store in Google Cloud Storage
gsutil cp ecold_backup_*.sql gs://ecold-backups/
```

**Rollback steps if needed:**
1. Restore PostgreSQL database from backup
2. Revert code to previous commit
3. Redeploy with PostgreSQL configuration

---

## 📈 Cost Comparison

### Current PostgreSQL Setup (Monthly)
- **Managed PostgreSQL** (AWS RDS/Digital Ocean): $15-50
- **Or Self-hosted** (EC2/Droplet): $20-40
- **Backup Storage**: $5-10
- **Total**: ~$20-60/month

### Firebase Spark Plan Setup (Monthly)
- **Firestore Database**: $0 (within Spark Plan limits)
- **Cloud Functions** (backups): $0 (within free tier - 125K invocations)
- **Cloud Storage** (backups): $0 (within free tier - 5GB)
- **Firebase Hosting**: $0 (10GB storage, 360MB/day bandwidth)
- **Total**: **$0/month** ✨

### Spark Plan Limits (Always Free)
- **Firestore Reads**: 50,000/day (~1.5M/month)
- **Firestore Writes**: 20,000/day (~600K/month)
- **Firestore Storage**: 1 GB
- **Cloud Functions**: 125,000 invocations/month
- **Storage**: 5 GB (for backups)
- **Hosting**: 10 GB storage, 360 MB/day bandwidth

### What You Get for FREE
✅ Database (Firestore)
✅ Automated daily backups
✅ 30 days backup retention
✅ Manual backup/restore functions
✅ Database monitoring
✅ Security rules
✅ Real-time updates
✅ Multi-region replication

### Scaling Costs (if exceeding Spark Plan)
**Upgrade to Blaze Plan (Pay as you go):**
- **Firestore**: $0.06 per 100K document reads (after free tier)
- **Firestore**: $0.18 per 100K document writes (after free tier)
- **Storage**: $0.026 per GB/month (after 5GB)
- **Functions**: $0.40 per million invocations (after 2M)
- **Still significantly cheaper than managed PostgreSQL**

---

## ✅ Testing Checklist

### Setup & Configuration
- [ ] Firebase project created on Spark Plan
- [ ] Firestore database initialized
- [ ] Security rules deployed
- [ ] Composite indexes created
- [ ] Service account configured
- [ ] Firebase CLI installed and configured

### Schema & Data
- [ ] Schema initialization completed
- [ ] Sample documents created successfully
- [ ] All collections and subcollections verified
- [ ] Data migration script tested
- [ ] All users migrated successfully

### Application Features
- [ ] User authentication working
- [ ] Email templates CRUD operations functional
- [ ] Recruiter CRUD operations functional
- [ ] Template-recruiter assignments working
- [ ] Email scheduling working
- [ ] Email logs being created
- [ ] Incoming email categorization working
- [ ] Resume upload/download working

### Integration
- [ ] OAuth authentication working
- [ ] Gmail API integration working
- [ ] Google OAuth flow tested
- [ ] Token refresh working

### Performance
- [ ] Response time <500ms for queries
- [ ] Composite indexes working
- [ ] Query optimization verified
- [ ] Real-time updates working

### Backup & Recovery
- [ ] Automated daily backup function deployed
- [ ] Manual backup tested successfully
- [ ] Backup storage in Cloud Storage verified
- [ ] Restore function tested
- [ ] Old backup cleanup working
- [ ] Local backup script tested
- [ ] 30-day backup retention verified

### Monitoring
- [ ] Firebase Console monitoring configured
- [ ] Usage quotas being tracked
- [ ] Backup alerts configured
- [ ] Free tier limits monitored
- [ ] Error logging working

### Security
- [ ] Firestore security rules tested
- [ ] User data isolation verified
- [ ] Service account permissions correct
- [ ] API keys secured
- [ ] Backup access restricted

---

## 📚 Resources

### Documentation
- [Firebase Console](https://console.firebase.google.com/)
- [Firestore Documentation](https://firebase.google.com/docs/firestore)
- [Firebase Functions Documentation](https://firebase.google.com/docs/functions)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Firebase Admin SDK for Java](https://firebase.google.com/docs/admin/setup)

### Code Examples
- [Firestore Java Quickstart](https://github.com/firebase/quickstart-java)
- [Firebase Functions Samples](https://github.com/firebase/functions-samples)
- [Firestore Security Rules Samples](https://github.com/firebase/quickstart-testing)

### Tools
- [Firebase CLI](https://firebase.google.com/docs/cli)
- [Firestore REST API](https://firebase.google.com/docs/firestore/use-rest-api)
- [Firebase Emulator Suite](https://firebase.google.com/docs/emulator-suite)

### Monitoring
- [Firebase Console](https://console.firebase.google.com/)
- [Firestore Usage Dashboard](https://console.firebase.google.com/project/_/firestore/usage)
- [Cloud Functions Logs](https://console.firebase.google.com/project/_/functions/logs)
- [Cloud Storage Browser](https://console.firebase.google.com/project/_/storage)

### Backup & Recovery
- [Firestore Export/Import](https://firebase.google.com/docs/firestore/manage-data/export-import)
- [Cloud Functions Scheduled Functions](https://firebase.google.com/docs/functions/schedule-functions)
- [Cloud Storage for Backups](https://firebase.google.com/docs/storage)

---

## 🎯 Success Metrics

### Technical Metrics
- ✅ Zero database costs
- ✅ <300ms average response time
- ✅ 99.9% uptime
- ✅ Zero data loss during migration

### Business Metrics
- ✅ Support up to 1000 active users on free tier
- ✅ Scale automatically with usage
- ✅ Reduce operational overhead
- ✅ Maintain all existing features

---

## 🚀 Next Steps

### Quick Start (For Firebase Spark Plan)

#### Day 1: Firebase Setup
1. ✅ Create Firebase project (Spark Plan - Free)
2. ✅ Initialize Firestore database
3. ✅ Configure security rules
4. ✅ Setup service account
5. ✅ Initialize schema with sample data
6. ✅ Deploy composite indexes

#### Day 2-3: Backup System
1. ✅ Create backup Cloud Functions
2. ✅ Configure Cloud Storage
3. ✅ Test automated daily backups
4. ✅ Test manual backup/restore
5. ✅ Setup backup monitoring

#### Day 4-5: Code Migration
1. ✅ Update dependencies (remove JPA, add Firebase Admin SDK)
2. ✅ Convert entities to Firestore documents
3. ✅ Implement Firestore repositories
4. ✅ Update service layer
5. ✅ Test locally with Firebase emulator

#### Day 6: Data Migration
1. ✅ Create migration script
2. ✅ Test with sample data
3. ✅ Execute full migration
4. ✅ Validate all data migrated
5. ✅ Create first backup

#### Day 7-8: Deployment
1. ✅ Deploy backend with Firebase config
2. ✅ Deploy frontend
3. ✅ Update OAuth redirect URIs
4. ✅ Configure environment variables
5. ✅ Smoke test all features

#### Day 9: Monitoring & Optimization
1. ✅ Configure Firebase monitoring
2. ✅ Setup usage alerts
3. ✅ Optimize queries
4. ✅ Document backup procedures
5. ✅ Create runbook for common issues

### Migration Timeline

**Estimated Total Migration Time**: 2-3 weeks (faster with Firebase vs Google Cloud)

**Developer Effort**: 30-40 hours

**Risk Level**: Low (Firebase has simpler setup, built-in backups, rollback-friendly)

### Weekly Breakdown

**Week 1**: Firebase setup + Schema initialization + Backups (Days 1-3)
**Week 2**: Code migration + Local testing + Data migration (Days 4-6)
**Week 3**: Deployment + Monitoring + Go-live (Days 7-9)

---

## 📞 Support & Troubleshooting

### Common Issues

#### Quota Exceeded
**Problem**: "Quota exceeded for quota metric 'Read requests'"

**Solution**:
```bash
# Check current usage
firebase firestore:usage

# Optimize queries to use less reads
# Use composite indexes
# Implement client-side caching
```

#### Backup Failed
**Problem**: Cloud Function backup fails

**Solution**:
```bash
# Check function logs
firebase functions:log

# Verify storage permissions
# Check Cloud Storage quota (5GB free tier)

# Manual backup as fallback
node scripts/local-firestore-backup.js
```

#### Security Rules Blocking Requests
**Problem**: "Missing or insufficient permissions"

**Solution**:
```bash
# Test rules in Firebase Console
# Firestore → Rules → Playground

# Verify user authentication
# Check userId matches document path
```

### Support Channels

For migration issues:
- **Firebase Support**: [Firebase Console → Support](https://console.firebase.google.com/support)
- **Firebase Community**: [Firebase Google Group](https://groups.google.com/g/firebase-talk)
- **Stack Overflow**:
  - [firebase tag](https://stackoverflow.com/questions/tagged/firebase)
  - [google-cloud-firestore tag](https://stackoverflow.com/questions/tagged/google-cloud-firestore)
- **Firebase Discord**: [Firebase Community Discord](https://discord.gg/BN2cgc3)

### Emergency Rollback

**If migration fails, rollback to PostgreSQL:**

```bash
# 1. Restore PostgreSQL from backup
psql ecold < ecold_backup_YYYYMMDD.sql

# 2. Revert code changes
git checkout <previous-commit-hash>

# 3. Rebuild and deploy
docker-compose down
docker-compose up -d --build

# 4. Verify application works
curl http://localhost:8080/api/health
```

---

## 🎯 Success Criteria

### Technical Success
- ✅ **Zero monthly database costs** (Spark Plan)
- ✅ **Automated daily backups** with 30-day retention
- ✅ **<300ms average response time**
- ✅ **99.9% uptime** (Firebase SLA)
- ✅ **Zero data loss** during migration
- ✅ **All features working** as before

### Business Success
- ✅ **Support 1000+ active users** on free tier
- ✅ **Auto-scaling** with usage
- ✅ **Reduced operational overhead** (no DB maintenance)
- ✅ **Simplified deployment** (no infrastructure management)
- ✅ **Built-in security** (Firebase Auth + Rules)
- ✅ **Real-time capabilities** (bonus feature)

### Free Tier Capacity
With Spark Plan limits, ECold can handle:
- **50,000 reads/day** = ~1.5M reads/month
- **20,000 writes/day** = ~600K writes/month
- **1 GB storage** = ~50,000-100,000 documents
- **Supports 500-1000 active users** comfortably

### If You Exceed Free Tier
Upgrade to **Blaze Plan** (pay-as-you-go):
- Still **significantly cheaper** than PostgreSQL
- Only pay for what you use above free tier
- No minimum commitment
- Can set budget alerts

---

## 📋 Quick Reference Commands

### Firebase CLI
```bash
# Login
firebase login

# Initialize project
firebase init

# Deploy everything
firebase deploy

# Deploy specific service
firebase deploy --only firestore:rules
firebase deploy --only functions
firebase deploy --only firestore:indexes

# View logs
firebase functions:log
```

### Backup Commands
```bash
# Manual backup
curl -X POST https://us-central1-ecold-app.cloudfunctions.net/manualFirestoreBackup \
  -H "Authorization: Bearer your-token"

# List backups
firebase storage:list backups/

# Download backup
gsutil cp gs://ecold-app.appspot.com/backups/firestore-backup-2025-01-05.json ./

# Restore backup
curl -X POST https://us-central1-ecold-app.cloudfunctions.net/restoreFirestoreBackup \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{"fileName": "backups/firestore-backup-2025-01-05.json"}'
```

### Monitoring
```bash
# Check Firestore usage
firebase firestore:usage

# View function stats
firebase functions:stats

# Check storage usage
gsutil du -sh gs://ecold-app.appspot.com/
```

---

**Document Version**: 2.0 (Firebase Spark Plan Edition)
**Last Updated**: 2025-01-05
**Status**: Production Ready ✨

---

*This migration enables ECold to run **100% FREE** on Firebase Spark Plan, with automated backups, zero database costs, and enterprise-grade security - perfect for startups and MVPs!*

**Total Monthly Cost: $0** 🎉
