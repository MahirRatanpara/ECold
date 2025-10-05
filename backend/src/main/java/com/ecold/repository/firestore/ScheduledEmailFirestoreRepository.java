package com.ecold.repository.firestore;

import com.ecold.entity.ScheduledEmail;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore Repository for ScheduledEmail entity
 * Path: /users/{userId}/scheduled_emails/{emailId}
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ScheduledEmailFirestoreRepository {

    private final Firestore firestore;
    private static final String USERS_COLLECTION = "users";
    private static final String SCHEDULED_EMAILS_COLLECTION = "scheduled_emails";

    /**
     * Get scheduled emails collection reference for a user
     */
    private CollectionReference getScheduledEmailsCollection(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(SCHEDULED_EMAILS_COLLECTION);
    }

    /**
     * Save or update a scheduled email
     */
    public ScheduledEmail save(String userId, ScheduledEmail email)
            throws ExecutionException, InterruptedException {
        CollectionReference emails = getScheduledEmailsCollection(userId);

        if (email.getId() == null || email.getId().isEmpty()) {
            DocumentReference docRef = emails.document();
            email.setId(docRef.getId());
            email.setCreatedAt(Timestamp.now());
        }

        email.setUserId(userId);
        emails.document(email.getId()).set(email).get();

        log.debug("ScheduledEmail saved: userId={}, emailId={}", userId, email.getId());
        return email;
    }

    /**
     * Find scheduled email by ID
     */
    public Optional<ScheduledEmail> findById(String userId, String emailId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getScheduledEmailsCollection(userId)
                .document(emailId)
                .get()
                .get();

        if (doc.exists()) {
            return Optional.of(doc.toObject(ScheduledEmail.class));
        }
        return Optional.empty();
    }

    /**
     * Find all scheduled emails for a user
     */
    public List<ScheduledEmail> findByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getScheduledEmailsCollection(userId).get().get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(ScheduledEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find scheduled emails by user with pagination
     */
    public Page<ScheduledEmail> findByUser(String userId, Pageable pageable)
            throws ExecutionException, InterruptedException {
        Query query = getScheduledEmailsCollection(userId)
                .orderBy("scheduleTime", Query.Direction.ASCENDING)
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        QuerySnapshot querySnapshot = query.get().get();
        List<ScheduledEmail> emails = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(ScheduledEmail.class))
                .collect(Collectors.toList());

        long total = getScheduledEmailsCollection(userId).get().get().size();
        return new PageImpl<>(emails, pageable, total);
    }

    /**
     * Find scheduled emails by user and status
     */
    public List<ScheduledEmail> findByUserAndStatus(String userId, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getScheduledEmailsCollection(userId)
                .whereEqualTo("status", status)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(ScheduledEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find scheduled emails by user and status with pagination
     */
    public Page<ScheduledEmail> findByUserAndStatus(String userId, String status, Pageable pageable)
            throws ExecutionException, InterruptedException {
        Query query = getScheduledEmailsCollection(userId)
                .whereEqualTo("status", status)
                .orderBy("scheduleTime", Query.Direction.ASCENDING)
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        QuerySnapshot querySnapshot = query.get().get();
        List<ScheduledEmail> emails = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(ScheduledEmail.class))
                .collect(Collectors.toList());

        long total = countByUserAndStatus(userId, status);
        return new PageImpl<>(emails, pageable, total);
    }

    /**
     * Find scheduled emails that are due (schedule time before now and status is SCHEDULED)
     */
    public List<ScheduledEmail> findDueEmails(String userId, Timestamp currentTime)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getScheduledEmailsCollection(userId)
                .whereEqualTo("status", ScheduledEmail.Status.SCHEDULED.name())
                .whereLessThanOrEqualTo("scheduleTime", currentTime)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(ScheduledEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find scheduled emails by recipient email
     */
    public List<ScheduledEmail> findByUserAndRecipientEmail(String userId, String recipientEmail)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getScheduledEmailsCollection(userId)
                .whereEqualTo("recipientEmail", recipientEmail)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(ScheduledEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find scheduled emails by template ID
     */
    public List<ScheduledEmail> findByUserAndTemplate(String userId, String templateId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getScheduledEmailsCollection(userId)
                .whereEqualTo("templateId", templateId)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(ScheduledEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find scheduled emails by recruiter ID
     */
    public List<ScheduledEmail> findByUserAndRecruiter(String userId, String recruiterId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getScheduledEmailsCollection(userId)
                .whereEqualTo("recruiterId", recruiterId)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(ScheduledEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find scheduled emails by schedule time range
     */
    public List<ScheduledEmail> findByUserAndScheduleTimeBetween(
            String userId, Timestamp startTime, Timestamp endTime)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getScheduledEmailsCollection(userId)
                .whereGreaterThanOrEqualTo("scheduleTime", startTime)
                .whereLessThanOrEqualTo("scheduleTime", endTime)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(ScheduledEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find pending scheduled emails (SCHEDULED status) for a user
     */
    public List<ScheduledEmail> findPendingByUser(String userId)
            throws ExecutionException, InterruptedException {
        return findByUserAndStatus(userId, ScheduledEmail.Status.SCHEDULED.name());
    }

    /**
     * Find sent emails for a user
     */
    public List<ScheduledEmail> findSentByUser(String userId)
            throws ExecutionException, InterruptedException {
        return findByUserAndStatus(userId, ScheduledEmail.Status.SENT.name());
    }

    /**
     * Find failed emails for a user
     */
    public List<ScheduledEmail> findFailedByUser(String userId)
            throws ExecutionException, InterruptedException {
        return findByUserAndStatus(userId, ScheduledEmail.Status.FAILED.name());
    }

    /**
     * Count scheduled emails by user and status
     */
    public long countByUserAndStatus(String userId, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getScheduledEmailsCollection(userId)
                .whereEqualTo("status", status)
                .get()
                .get();
        return querySnapshot.size();
    }

    /**
     * Count all scheduled emails for a user
     */
    public long countByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getScheduledEmailsCollection(userId).get().get();
        return querySnapshot.size();
    }

    /**
     * Delete scheduled email
     */
    public void delete(String userId, String emailId)
            throws ExecutionException, InterruptedException {
        getScheduledEmailsCollection(userId).document(emailId).delete().get();
        log.debug("ScheduledEmail deleted: userId={}, emailId={}", userId, emailId);
    }

    /**
     * Delete scheduled email entity
     */
    public void delete(String userId, ScheduledEmail email)
            throws ExecutionException, InterruptedException {
        if (email.getId() != null) {
            delete(userId, email.getId());
        }
    }

    /**
     * Delete all scheduled emails for a template
     */
    public void deleteByUserAndTemplate(String userId, String templateId)
            throws ExecutionException, InterruptedException {
        List<ScheduledEmail> emails = findByUserAndTemplate(userId, templateId);
        for (ScheduledEmail email : emails) {
            delete(userId, email.getId());
        }
        log.debug("All scheduled emails deleted for: userId={}, templateId={}", userId, templateId);
    }

    /**
     * Delete all scheduled emails for a recruiter
     */
    public void deleteByUserAndRecruiter(String userId, String recruiterId)
            throws ExecutionException, InterruptedException {
        List<ScheduledEmail> emails = findByUserAndRecruiter(userId, recruiterId);
        for (ScheduledEmail email : emails) {
            delete(userId, email.getId());
        }
        log.debug("All scheduled emails deleted for: userId={}, recruiterId={}", userId, recruiterId);
    }

    /**
     * Check if scheduled email exists
     */
    public boolean existsById(String userId, String emailId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getScheduledEmailsCollection(userId)
                .document(emailId)
                .get()
                .get();
        return doc.exists();
    }
}
