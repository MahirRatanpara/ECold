package com.ecold.repository.firestore;

import com.ecold.entity.RecruiterTemplateAssignment;
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
 * Firestore Repository for RecruiterTemplateAssignment entity
 * Path: /users/{userId}/assignments/{assignmentId}
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RecruiterTemplateAssignmentFirestoreRepository {

    private final Firestore firestore;
    private static final String USERS_COLLECTION = "users";
    private static final String ASSIGNMENTS_COLLECTION = "assignments";

    /**
     * Get assignments collection reference for a user
     */
    private CollectionReference getAssignmentsCollection(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(ASSIGNMENTS_COLLECTION);
    }

    /**
     * Save or update an assignment
     */
    public RecruiterTemplateAssignment save(String userId, RecruiterTemplateAssignment assignment)
            throws ExecutionException, InterruptedException {
        CollectionReference assignments = getAssignmentsCollection(userId);

        if (assignment.getId() == null || assignment.getId().isEmpty()) {
            DocumentReference docRef = assignments.document();
            assignment.setId(docRef.getId());
            assignment.setCreatedAt(Timestamp.now());
        }

        assignment.setUserId(userId);
        assignment.setUpdatedAt(Timestamp.now());
        assignments.document(assignment.getId()).set(assignment).get();

        log.debug("RecruiterTemplateAssignment saved: userId={}, assignmentId={}", userId, assignment.getId());
        return assignment;
    }

    /**
     * Find assignment by ID
     */
    public Optional<RecruiterTemplateAssignment> findById(String userId, String assignmentId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getAssignmentsCollection(userId)
                .document(assignmentId)
                .get()
                .get();

        if (doc.exists()) {
            return Optional.of(doc.toObject(RecruiterTemplateAssignment.class));
        }
        return Optional.empty();
    }

    /**
     * Find all assignments for a user
     */
    public List<RecruiterTemplateAssignment> findByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId).get().get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterTemplateAssignment.class))
                .collect(Collectors.toList());
    }

    /**
     * Find assignments by user with pagination
     */
    public Page<RecruiterTemplateAssignment> findByUser(String userId, Pageable pageable)
            throws ExecutionException, InterruptedException {
        Query query = getAssignmentsCollection(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        QuerySnapshot querySnapshot = query.get().get();
        List<RecruiterTemplateAssignment> assignments = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterTemplateAssignment.class))
                .collect(Collectors.toList());

        long total = getAssignmentsCollection(userId).get().get().size();
        return new PageImpl<>(assignments, pageable, total);
    }

    /**
     * Find assignments by user and recruiter
     */
    public List<RecruiterTemplateAssignment> findByUserAndRecruiter(String userId, String recruiterId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId)
                .whereEqualTo("recruiterId", recruiterId)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterTemplateAssignment.class))
                .collect(Collectors.toList());
    }

    /**
     * Find assignments by user and template
     */
    public List<RecruiterTemplateAssignment> findByUserAndTemplate(String userId, String templateId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId)
                .whereEqualTo("templateId", templateId)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterTemplateAssignment.class))
                .collect(Collectors.toList());
    }

    /**
     * Find assignments by user and status
     */
    public List<RecruiterTemplateAssignment> findByUserAndStatus(String userId, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId)
                .whereEqualTo("assignmentStatus", status)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterTemplateAssignment.class))
                .collect(Collectors.toList());
    }

    /**
     * Find assignments by user and week/year
     */
    public List<RecruiterTemplateAssignment> findByUserAndWeekAndYear(String userId, Integer week, Integer year)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId)
                .whereEqualTo("weekAssigned", week)
                .whereEqualTo("yearAssigned", year)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterTemplateAssignment.class))
                .collect(Collectors.toList());
    }

    /**
     * Find assignment by user, recruiter, week, and year
     */
    public Optional<RecruiterTemplateAssignment> findByUserAndRecruiterAndWeekAndYear(
            String userId, String recruiterId, Integer week, Integer year)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId)
                .whereEqualTo("recruiterId", recruiterId)
                .whereEqualTo("weekAssigned", week)
                .whereEqualTo("yearAssigned", year)
                .limit(1)
                .get()
                .get();

        if (!querySnapshot.getDocuments().isEmpty()) {
            return Optional.of(querySnapshot.getDocuments().get(0).toObject(RecruiterTemplateAssignment.class));
        }
        return Optional.empty();
    }

    /**
     * Find assignments by user, status, and week/year
     */
    public List<RecruiterTemplateAssignment> findByUserAndStatusAndWeekAndYear(
            String userId, String status, Integer week, Integer year)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId)
                .whereEqualTo("assignmentStatus", status)
                .whereEqualTo("weekAssigned", week)
                .whereEqualTo("yearAssigned", year)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterTemplateAssignment.class))
                .collect(Collectors.toList());
    }

    /**
     * Find assignments by user, recruiter, and status
     */
    public List<RecruiterTemplateAssignment> findByUserAndRecruiterAndStatus(
            String userId, String recruiterId, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId)
                .whereEqualTo("recruiterId", recruiterId)
                .whereEqualTo("assignmentStatus", status)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterTemplateAssignment.class))
                .collect(Collectors.toList());
    }

    /**
     * Find assignments by user, template, and status
     */
    public List<RecruiterTemplateAssignment> findByUserAndTemplateAndStatus(
            String userId, String templateId, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId)
                .whereEqualTo("templateId", templateId)
                .whereEqualTo("assignmentStatus", status)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterTemplateAssignment.class))
                .collect(Collectors.toList());
    }

    /**
     * Find active assignments (ACTIVE status) for a user
     */
    public List<RecruiterTemplateAssignment> findActiveByUser(String userId)
            throws ExecutionException, InterruptedException {
        return findByUserAndStatus(userId, RecruiterTemplateAssignment.AssignmentStatus.ACTIVE.name());
    }

    /**
     * Find assignments sent before a certain date
     */
    public List<RecruiterTemplateAssignment> findByUserAndLastEmailSentBefore(String userId, Timestamp date)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId)
                .whereLessThan("lastEmailSentAt", date)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterTemplateAssignment.class))
                .collect(Collectors.toList());
    }

    /**
     * Find assignments where emails haven't been sent
     */
    public List<RecruiterTemplateAssignment> findByUserAndEmailsNotSent(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId)
                .whereEqualTo("emailsSent", 0)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterTemplateAssignment.class))
                .collect(Collectors.toList());
    }

    /**
     * Count assignments by user and status
     */
    public long countByUserAndStatus(String userId, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId)
                .whereEqualTo("assignmentStatus", status)
                .get()
                .get();
        return querySnapshot.size();
    }

    /**
     * Count assignments by user and week/year
     */
    public long countByUserAndWeekAndYear(String userId, Integer week, Integer year)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getAssignmentsCollection(userId)
                .whereEqualTo("weekAssigned", week)
                .whereEqualTo("yearAssigned", year)
                .get()
                .get();
        return querySnapshot.size();
    }

    /**
     * Delete assignment
     */
    public void delete(String userId, String assignmentId)
            throws ExecutionException, InterruptedException {
        getAssignmentsCollection(userId).document(assignmentId).delete().get();
        log.debug("RecruiterTemplateAssignment deleted: userId={}, assignmentId={}", userId, assignmentId);
    }

    /**
     * Delete assignment entity
     */
    public void delete(String userId, RecruiterTemplateAssignment assignment)
            throws ExecutionException, InterruptedException {
        if (assignment.getId() != null) {
            delete(userId, assignment.getId());
        }
    }

    /**
     * Delete all assignments for a recruiter
     */
    public void deleteByUserAndRecruiter(String userId, String recruiterId)
            throws ExecutionException, InterruptedException {
        List<RecruiterTemplateAssignment> assignments = findByUserAndRecruiter(userId, recruiterId);
        for (RecruiterTemplateAssignment assignment : assignments) {
            delete(userId, assignment.getId());
        }
        log.debug("All assignments deleted for: userId={}, recruiterId={}", userId, recruiterId);
    }

    /**
     * Delete all assignments for a template
     */
    public void deleteByUserAndTemplate(String userId, String templateId)
            throws ExecutionException, InterruptedException {
        List<RecruiterTemplateAssignment> assignments = findByUserAndTemplate(userId, templateId);
        for (RecruiterTemplateAssignment assignment : assignments) {
            delete(userId, assignment.getId());
        }
        log.debug("All assignments deleted for: userId={}, templateId={}", userId, templateId);
    }

    /**
     * Check if assignment exists
     */
    public boolean existsById(String userId, String assignmentId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getAssignmentsCollection(userId)
                .document(assignmentId)
                .get()
                .get();
        return doc.exists();
    }

    /**
     * Check if assignment exists for recruiter and week/year
     */
    public boolean existsByUserAndRecruiterAndWeekAndYear(
            String userId, String recruiterId, Integer week, Integer year)
            throws ExecutionException, InterruptedException {
        return findByUserAndRecruiterAndWeekAndYear(userId, recruiterId, week, year).isPresent();
    }
}
