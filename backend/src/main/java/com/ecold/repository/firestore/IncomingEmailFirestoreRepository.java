package com.ecold.repository.firestore;

import com.ecold.entity.IncomingEmail;
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
 * Firestore Repository for IncomingEmail entity
 * Path: /users/{userId}/incoming_emails/{emailId}
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class IncomingEmailFirestoreRepository {

    private final Firestore firestore;
    private static final String USERS_COLLECTION = "users";
    private static final String INCOMING_EMAILS_COLLECTION = "incoming_emails";

    /**
     * Get incoming emails collection reference for a user
     */
    private CollectionReference getIncomingEmailsCollection(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(INCOMING_EMAILS_COLLECTION);
    }

    /**
     * Save or update an incoming email
     */
    public IncomingEmail save(String userId, IncomingEmail email)
            throws ExecutionException, InterruptedException {
        CollectionReference emails = getIncomingEmailsCollection(userId);

        if (email.getId() == null || email.getId().isEmpty()) {
            DocumentReference docRef = emails.document();
            email.setId(docRef.getId());
            email.setCreatedAt(Timestamp.now());
        }

        email.setUserId(userId);
        emails.document(email.getId()).set(email).get();

        log.debug("IncomingEmail saved: userId={}, emailId={}", userId, email.getId());
        return email;
    }

    /**
     * Find incoming email by ID
     */
    public Optional<IncomingEmail> findById(String userId, String emailId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getIncomingEmailsCollection(userId)
                .document(emailId)
                .get()
                .get();

        if (doc.exists()) {
            return Optional.of(doc.toObject(IncomingEmail.class));
        }
        return Optional.empty();
    }

    /**
     * Find all incoming emails for a user
     */
    public List<IncomingEmail> findByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId).get().get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find incoming emails by user with pagination
     */
    public Page<IncomingEmail> findByUser(String userId, Pageable pageable)
            throws ExecutionException, InterruptedException {
        Query query = getIncomingEmailsCollection(userId)
                .orderBy("receivedAt", Query.Direction.DESCENDING)
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        QuerySnapshot querySnapshot = query.get().get();
        List<IncomingEmail> emails = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());

        long total = getIncomingEmailsCollection(userId).get().get().size();
        return new PageImpl<>(emails, pageable, total);
    }

    /**
     * Find incoming emails by category
     */
    public List<IncomingEmail> findByUserAndCategory(String userId, String category)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("category", category)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find incoming emails by category with pagination
     */
    public Page<IncomingEmail> findByUserAndCategory(String userId, String category, Pageable pageable)
            throws ExecutionException, InterruptedException {
        Query query = getIncomingEmailsCollection(userId)
                .whereEqualTo("category", category)
                .orderBy("receivedAt", Query.Direction.DESCENDING)
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        QuerySnapshot querySnapshot = query.get().get();
        List<IncomingEmail> emails = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());

        long total = countByUserAndCategory(userId, category);
        return new PageImpl<>(emails, pageable, total);
    }

    /**
     * Find incoming emails by priority
     */
    public List<IncomingEmail> findByUserAndPriority(String userId, String priority)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("priority", priority)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find incoming emails by priority with pagination
     */
    public Page<IncomingEmail> findByUserAndPriority(String userId, String priority, Pageable pageable)
            throws ExecutionException, InterruptedException {
        Query query = getIncomingEmailsCollection(userId)
                .whereEqualTo("priority", priority)
                .orderBy("receivedAt", Query.Direction.DESCENDING)
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        QuerySnapshot querySnapshot = query.get().get();
        List<IncomingEmail> emails = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());

        long total = countByUserAndPriority(userId, priority);
        return new PageImpl<>(emails, pageable, total);
    }

    /**
     * Find unread incoming emails
     */
    public List<IncomingEmail> findUnreadByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("isRead", false)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find unread incoming emails with pagination
     */
    public Page<IncomingEmail> findUnreadByUser(String userId, Pageable pageable)
            throws ExecutionException, InterruptedException {
        Query query = getIncomingEmailsCollection(userId)
                .whereEqualTo("isRead", false)
                .orderBy("receivedAt", Query.Direction.DESCENDING)
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        QuerySnapshot querySnapshot = query.get().get();
        List<IncomingEmail> emails = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());

        long total = countUnreadByUser(userId);
        return new PageImpl<>(emails, pageable, total);
    }

    /**
     * Find unprocessed incoming emails
     */
    public List<IncomingEmail> findUnprocessedByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("isProcessed", false)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find incoming emails by sender email
     */
    public List<IncomingEmail> findByUserAndSenderEmail(String userId, String senderEmail)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("senderEmail", senderEmail)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find incoming email by message ID
     */
    public Optional<IncomingEmail> findByUserAndMessageId(String userId, String messageId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("messageId", messageId)
                .limit(1)
                .get()
                .get();

        if (!querySnapshot.getDocuments().isEmpty()) {
            return Optional.of(querySnapshot.getDocuments().get(0).toObject(IncomingEmail.class));
        }
        return Optional.empty();
    }

    /**
     * Find incoming emails by thread ID
     */
    public List<IncomingEmail> findByUserAndThreadId(String userId, String threadId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("threadId", threadId)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find incoming emails received after a certain date
     */
    public List<IncomingEmail> findByUserAndReceivedAtAfter(String userId, Timestamp date)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereGreaterThan("receivedAt", date)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find incoming emails received between two dates
     */
    public List<IncomingEmail> findByUserAndReceivedAtBetween(String userId, Timestamp startDate, Timestamp endDate)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereGreaterThanOrEqualTo("receivedAt", startDate)
                .whereLessThanOrEqualTo("receivedAt", endDate)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Find incoming emails by category and priority
     */
    public List<IncomingEmail> findByUserAndCategoryAndPriority(String userId, String category, String priority)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("category", category)
                .whereEqualTo("priority", priority)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Search incoming emails by keywords (in-memory filtering)
     */
    public List<IncomingEmail> searchByUserAndKeywords(String userId, String searchTerm)
            throws ExecutionException, InterruptedException {
        List<IncomingEmail> allEmails = findByUser(userId);
        String searchLower = searchTerm.toLowerCase();

        return allEmails.stream()
                .filter(email -> (email.getSubject() != null && email.getSubject().toLowerCase().contains(searchLower))
                        || (email.getBody() != null && email.getBody().toLowerCase().contains(searchLower))
                        || (email.getSenderEmail() != null && email.getSenderEmail().toLowerCase().contains(searchLower))
                        || (email.getSenderName() != null && email.getSenderName().toLowerCase().contains(searchLower))
                        || (email.getKeywords() != null && email.getKeywords().toLowerCase().contains(searchLower)))
                .collect(Collectors.toList());
    }

    /**
     * Find high priority unread emails
     */
    public List<IncomingEmail> findHighPriorityUnreadByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("priority", IncomingEmail.EmailPriority.HIGH.name())
                .whereEqualTo("isRead", false)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(IncomingEmail.class))
                .collect(Collectors.toList());
    }

    /**
     * Count incoming emails by category
     */
    public long countByUserAndCategory(String userId, String category)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("category", category)
                .get()
                .get();
        return querySnapshot.size();
    }

    /**
     * Count incoming emails by priority
     */
    public long countByUserAndPriority(String userId, String priority)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("priority", priority)
                .get()
                .get();
        return querySnapshot.size();
    }

    /**
     * Count unread emails
     */
    public long countUnreadByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("isRead", false)
                .get()
                .get();
        return querySnapshot.size();
    }

    /**
     * Count unprocessed emails
     */
    public long countUnprocessedByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId)
                .whereEqualTo("isProcessed", false)
                .get()
                .get();
        return querySnapshot.size();
    }

    /**
     * Count all incoming emails for a user
     */
    public long countByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getIncomingEmailsCollection(userId).get().get();
        return querySnapshot.size();
    }

    /**
     * Delete incoming email
     */
    public void delete(String userId, String emailId)
            throws ExecutionException, InterruptedException {
        getIncomingEmailsCollection(userId).document(emailId).delete().get();
        log.debug("IncomingEmail deleted: userId={}, emailId={}", userId, emailId);
    }

    /**
     * Delete incoming email entity
     */
    public void delete(String userId, IncomingEmail email)
            throws ExecutionException, InterruptedException {
        if (email.getId() != null) {
            delete(userId, email.getId());
        }
    }

    /**
     * Check if incoming email exists
     */
    public boolean existsById(String userId, String emailId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getIncomingEmailsCollection(userId)
                .document(emailId)
                .get()
                .get();
        return doc.exists();
    }

    /**
     * Check if email exists by message ID
     */
    public boolean existsByUserAndMessageId(String userId, String messageId)
            throws ExecutionException, InterruptedException {
        return findByUserAndMessageId(userId, messageId).isPresent();
    }
}
