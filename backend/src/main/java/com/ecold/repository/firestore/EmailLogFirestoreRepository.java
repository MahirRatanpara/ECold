package com.ecold.repository.firestore;

import com.ecold.entity.EmailLog;
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
 * Firestore Repository for EmailLog entity
 * Path: /users/{userId}/email_logs/{logId}
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class EmailLogFirestoreRepository {

    private final Firestore firestore;
    private static final String USERS_COLLECTION = "users";
    private static final String EMAIL_LOGS_COLLECTION = "email_logs";

    /**
     * Get email logs collection reference for a user
     */
    private CollectionReference getEmailLogsCollection(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EMAIL_LOGS_COLLECTION);
    }

    /**
     * Save or update an email log
     */
    public EmailLog save(String userId, EmailLog emailLog)
            throws ExecutionException, InterruptedException {
        CollectionReference logs = getEmailLogsCollection(userId);

        if (emailLog.getId() == null || emailLog.getId().isEmpty()) {
            DocumentReference docRef = logs.document();
            emailLog.setId(docRef.getId());
            emailLog.setCreatedAt(Timestamp.now());
        }

        emailLog.setUserId(userId);
        logs.document(emailLog.getId()).set(emailLog).get();

        log.debug("EmailLog saved: userId={}, logId={}", userId, emailLog.getId());
        return emailLog;
    }

    /**
     * Find email log by ID
     */
    public Optional<EmailLog> findById(String userId, String logId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getEmailLogsCollection(userId)
                .document(logId)
                .get()
                .get();

        if (doc.exists()) {
            return Optional.of(doc.toObject(EmailLog.class));
        }
        return Optional.empty();
    }

    /**
     * Find all email logs for a user
     */
    public List<EmailLog> findByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getEmailLogsCollection(userId).get().get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailLog.class))
                .collect(Collectors.toList());
    }

    /**
     * Find email logs by user with pagination
     */
    public Page<EmailLog> findByUser(String userId, Pageable pageable)
            throws ExecutionException, InterruptedException {
        Query query = getEmailLogsCollection(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        QuerySnapshot querySnapshot = query.get().get();
        List<EmailLog> logs = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailLog.class))
                .collect(Collectors.toList());

        long total = getEmailLogsCollection(userId).get().get().size();
        return new PageImpl<>(logs, pageable, total);
    }

    /**
     * Find email logs by user and status
     */
    public List<EmailLog> findByUserAndStatus(String userId, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getEmailLogsCollection(userId)
                .whereEqualTo("status", status)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailLog.class))
                .collect(Collectors.toList());
    }

    /**
     * Find email logs by user and status with pagination
     */
    public Page<EmailLog> findByUserAndStatus(String userId, String status, Pageable pageable)
            throws ExecutionException, InterruptedException {
        Query query = getEmailLogsCollection(userId)
                .whereEqualTo("status", status)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        QuerySnapshot querySnapshot = query.get().get();
        List<EmailLog> logs = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailLog.class))
                .collect(Collectors.toList());

        long total = countByUserAndStatus(userId, status);
        return new PageImpl<>(logs, pageable, total);
    }

    /**
     * Find email logs by recruiter contact
     */
    public List<EmailLog> findByUserAndRecruiter(String userId, String recruiterContactId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getEmailLogsCollection(userId)
                .whereEqualTo("recruiterContactId", recruiterContactId)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailLog.class))
                .collect(Collectors.toList());
    }

    /**
     * Find email logs by recipient email
     */
    public List<EmailLog> findByUserAndRecipientEmail(String userId, String recipientEmail)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getEmailLogsCollection(userId)
                .whereEqualTo("recipientEmail", recipientEmail)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailLog.class))
                .collect(Collectors.toList());
    }

    /**
     * Find email log by message ID
     */
    public Optional<EmailLog> findByUserAndMessageId(String userId, String messageId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getEmailLogsCollection(userId)
                .whereEqualTo("messageId", messageId)
                .limit(1)
                .get()
                .get();

        if (!querySnapshot.getDocuments().isEmpty()) {
            return Optional.of(querySnapshot.getDocuments().get(0).toObject(EmailLog.class));
        }
        return Optional.empty();
    }

    /**
     * Find email logs sent after a certain date
     */
    public List<EmailLog> findByUserAndSentAtAfter(String userId, Timestamp date)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getEmailLogsCollection(userId)
                .whereGreaterThan("sentAt", date)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailLog.class))
                .collect(Collectors.toList());
    }

    /**
     * Find email logs sent before a certain date
     */
    public List<EmailLog> findByUserAndSentAtBefore(String userId, Timestamp date)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getEmailLogsCollection(userId)
                .whereLessThan("sentAt", date)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailLog.class))
                .collect(Collectors.toList());
    }

    /**
     * Find email logs sent between two dates
     */
    public List<EmailLog> findByUserAndSentAtBetween(String userId, Timestamp startDate, Timestamp endDate)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getEmailLogsCollection(userId)
                .whereGreaterThanOrEqualTo("sentAt", startDate)
                .whereLessThanOrEqualTo("sentAt", endDate)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailLog.class))
                .collect(Collectors.toList());
    }

    /**
     * Find opened emails (emails with openedAt timestamp)
     */
    public List<EmailLog> findOpenedByUser(String userId)
            throws ExecutionException, InterruptedException {
        // Get all logs and filter for openedAt not null (Firestore limitation)
        List<EmailLog> allLogs = findByUser(userId);
        return allLogs.stream()
                .filter(log -> log.getOpenedAt() != null)
                .collect(Collectors.toList());
    }

    /**
     * Find clicked emails (emails with clickedAt timestamp)
     */
    public List<EmailLog> findClickedByUser(String userId)
            throws ExecutionException, InterruptedException {
        // Get all logs and filter for clickedAt not null (Firestore limitation)
        List<EmailLog> allLogs = findByUser(userId);
        return allLogs.stream()
                .filter(log -> log.getClickedAt() != null)
                .collect(Collectors.toList());
    }

    /**
     * Find bounced emails
     */
    public List<EmailLog> findBouncedByUser(String userId)
            throws ExecutionException, InterruptedException {
        return findByUserAndStatus(userId, EmailLog.EmailStatus.BOUNCED.name());
    }

    /**
     * Find failed emails
     */
    public List<EmailLog> findFailedByUser(String userId)
            throws ExecutionException, InterruptedException {
        return findByUserAndStatus(userId, EmailLog.EmailStatus.FAILED.name());
    }

    /**
     * Count email logs by user and status
     */
    public long countByUserAndStatus(String userId, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getEmailLogsCollection(userId)
                .whereEqualTo("status", status)
                .get()
                .get();
        return querySnapshot.size();
    }

    /**
     * Count all email logs for a user
     */
    public long countByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getEmailLogsCollection(userId).get().get();
        return querySnapshot.size();
    }

    /**
     * Count opened emails
     */
    public long countOpenedByUser(String userId)
            throws ExecutionException, InterruptedException {
        return findOpenedByUser(userId).size();
    }

    /**
     * Count clicked emails
     */
    public long countClickedByUser(String userId)
            throws ExecutionException, InterruptedException {
        return findClickedByUser(userId).size();
    }

    /**
     * Delete email log
     */
    public void delete(String userId, String logId)
            throws ExecutionException, InterruptedException {
        getEmailLogsCollection(userId).document(logId).delete().get();
        log.debug("EmailLog deleted: userId={}, logId={}", userId, logId);
    }

    /**
     * Delete email log entity
     */
    public void delete(String userId, EmailLog emailLog)
            throws ExecutionException, InterruptedException {
        if (emailLog.getId() != null) {
            delete(userId, emailLog.getId());
        }
    }

    /**
     * Delete all email logs for a recruiter
     */
    public void deleteByUserAndRecruiter(String userId, String recruiterContactId)
            throws ExecutionException, InterruptedException {
        List<EmailLog> logs = findByUserAndRecruiter(userId, recruiterContactId);
        for (EmailLog log : logs) {
            delete(userId, log.getId());
        }
        log.debug("All email logs deleted for: userId={}, recruiterContactId={}", userId, recruiterContactId);
    }

    /**
     * Check if email log exists
     */
    public boolean existsById(String userId, String logId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getEmailLogsCollection(userId)
                .document(logId)
                .get()
                .get();
        return doc.exists();
    }
}
