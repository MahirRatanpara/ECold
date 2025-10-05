package com.ecold.repository.firestore;

import com.ecold.entity.EmailTemplate;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore Repository for EmailTemplate entity
 * Path: /users/{userId}/templates/{templateId}
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class EmailTemplateFirestoreRepository {

    private final Firestore firestore;
    private static final String USERS_COLLECTION = "users";
    private static final String TEMPLATES_COLLECTION = "templates";

    /**
     * Get templates collection reference for a user
     */
    private CollectionReference getTemplatesCollection(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(TEMPLATES_COLLECTION);
    }

    /**
     * Save or update an email template
     */
    public EmailTemplate save(String userId, EmailTemplate template)
            throws ExecutionException, InterruptedException {
        CollectionReference templates = getTemplatesCollection(userId);

        if (template.getId() == null || template.getId().isEmpty()) {
            DocumentReference docRef = templates.document();
            template.setId(docRef.getId());
            template.setCreatedAt(Timestamp.now());
        }

        template.setUserId(userId);
        template.setUpdatedAt(Timestamp.now());
        templates.document(template.getId()).set(template).get();

        log.debug("EmailTemplate saved: userId={}, templateId={}", userId, template.getId());
        return template;
    }

    /**
     * Find template by ID
     */
    public Optional<EmailTemplate> findById(String userId, String templateId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getTemplatesCollection(userId)
                .document(templateId)
                .get()
                .get();

        if (doc.exists()) {
            return Optional.of(doc.toObject(EmailTemplate.class));
        }
        return Optional.empty();
    }

    /**
     * Find all templates for a user
     */
    public List<EmailTemplate> findByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getTemplatesCollection(userId).get().get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailTemplate.class))
                .collect(Collectors.toList());
    }

    /**
     * Find templates by user and category
     */
    public List<EmailTemplate> findByUserAndCategory(String userId, String category)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getTemplatesCollection(userId)
                .whereEqualTo("category", category)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailTemplate.class))
                .collect(Collectors.toList());
    }

    /**
     * Find templates by user and status
     */
    public List<EmailTemplate> findByUserAndStatus(String userId, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getTemplatesCollection(userId)
                .whereEqualTo("status", status)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailTemplate.class))
                .collect(Collectors.toList());
    }

    /**
     * Find templates by user, category and status
     */
    public List<EmailTemplate> findByUserAndCategoryAndStatus(String userId, String category, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getTemplatesCollection(userId)
                .whereEqualTo("category", category)
                .whereEqualTo("status", status)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(EmailTemplate.class))
                .collect(Collectors.toList());
    }

    /**
     * Find template by user and name
     */
    public Optional<EmailTemplate> findByUserAndName(String userId, String name)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getTemplatesCollection(userId)
                .whereEqualTo("name", name)
                .limit(1)
                .get()
                .get();

        if (!querySnapshot.getDocuments().isEmpty()) {
            return Optional.of(querySnapshot.getDocuments().get(0).toObject(EmailTemplate.class));
        }
        return Optional.empty();
    }

    /**
     * Count templates for a user
     */
    public long countByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getTemplatesCollection(userId).get().get();
        return querySnapshot.getDocuments().size();
    }

    /**
     * Count templates by user and status
     */
    public long countByUserAndStatus(String userId, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getTemplatesCollection(userId)
                .whereEqualTo("status", status)
                .get()
                .get();
        return querySnapshot.getDocuments().size();
    }

    /**
     * Delete template
     */
    public void delete(String userId, String templateId)
            throws ExecutionException, InterruptedException {
        getTemplatesCollection(userId).document(templateId).delete().get();
        log.debug("EmailTemplate deleted: userId={}, templateId={}", userId, templateId);
    }

    /**
     * Delete template entity
     */
    public void delete(String userId, EmailTemplate template)
            throws ExecutionException, InterruptedException {
        if (template.getId() != null) {
            delete(userId, template.getId());
        }
    }

    /**
     * Check if template exists
     */
    public boolean existsById(String userId, String templateId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getTemplatesCollection(userId)
                .document(templateId)
                .get()
                .get();
        return doc.exists();
    }
}
