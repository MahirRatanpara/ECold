package com.ecold.repository.firestore;

import com.ecold.entity.RecruiterContact;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore Repository for RecruiterContact entity
 * Path: /users/{userId}/recruiters/{recruiterId}
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RecruiterContactFirestoreRepository {

    private final Firestore firestore;
    private static final String USERS_COLLECTION = "users";
    private static final String RECRUITERS_COLLECTION = "recruiters";

    /**
     * Get recruiters collection reference for a user
     */
    private CollectionReference getRecruitersCollection(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(RECRUITERS_COLLECTION);
    }

    /**
     * Save or update a recruiter contact
     */
    public RecruiterContact save(String userId, RecruiterContact recruiter)
            throws ExecutionException, InterruptedException {
        CollectionReference recruiters = getRecruitersCollection(userId);

        if (recruiter.getId() == null || recruiter.getId().isEmpty()) {
            DocumentReference docRef = recruiters.document();
            recruiter.setId(docRef.getId());
            recruiter.setCreatedAt(Timestamp.now());
        }

        recruiter.setUserId(userId);
        recruiter.setUpdatedAt(Timestamp.now());
        recruiters.document(recruiter.getId()).set(recruiter).get();

        log.debug("RecruiterContact saved: userId={}, recruiterId={}", userId, recruiter.getId());
        return recruiter;
    }

    /**
     * Find recruiter by ID
     */
    public Optional<RecruiterContact> findById(String userId, String recruiterId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getRecruitersCollection(userId)
                .document(recruiterId)
                .get()
                .get();

        if (doc.exists()) {
            return Optional.of(doc.toObject(RecruiterContact.class));
        }
        return Optional.empty();
    }

    /**
     * Find all recruiters for a user with pagination
     */
    public Page<RecruiterContact> findByUser(String userId, Pageable pageable)
            throws ExecutionException, InterruptedException {
        Query query = getRecruitersCollection(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        QuerySnapshot querySnapshot = query.get().get();
        List<RecruiterContact> recruiters = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterContact.class))
                .collect(Collectors.toList());

        long total = getRecruitersCollection(userId).get().get().size();
        return new PageImpl<>(recruiters, pageable, total);
    }

    /**
     * Find recruiters by user and status (List)
     */
    public List<RecruiterContact> findByUserAndStatus(String userId, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getRecruitersCollection(userId)
                .whereEqualTo("status", status)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterContact.class))
                .collect(Collectors.toList());
    }

    /**
     * Find recruiters by user and status with pagination
     */
    public Page<RecruiterContact> findByUserAndStatus(String userId, String status, Pageable pageable)
            throws ExecutionException, InterruptedException {
        Query query = getRecruitersCollection(userId)
                .whereEqualTo("status", status)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        QuerySnapshot querySnapshot = query.get().get();
        List<RecruiterContact> recruiters = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterContact.class))
                .collect(Collectors.toList());

        long total = countByUserAndStatus(userId, status);
        return new PageImpl<>(recruiters, pageable, total);
    }

    /**
     * Find recruiter by user and email
     */
    public Optional<RecruiterContact> findByUserAndEmail(String userId, String email)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getRecruitersCollection(userId)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .get();

        if (!querySnapshot.getDocuments().isEmpty()) {
            return Optional.of(querySnapshot.getDocuments().get(0).toObject(RecruiterContact.class));
        }
        return Optional.empty();
    }

    /**
     * Check if recruiter exists by user and email
     */
    public boolean existsByUserAndEmail(String userId, String email)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getRecruitersCollection(userId)
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .get();

        return !querySnapshot.getDocuments().isEmpty();
    }

    /**
     * Find uncontacted recruiters by user
     */
    public List<RecruiterContact> findUncontactedByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getRecruitersCollection(userId)
                .whereEqualTo("lastContactedAt", null)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterContact.class))
                .collect(Collectors.toList());
    }

    /**
     * Find recruiters contacted before a certain date
     */
    public List<RecruiterContact> findContactedBeforeDate(String userId, Timestamp date)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getRecruitersCollection(userId)
                .whereLessThan("lastContactedAt", date)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterContact.class))
                .collect(Collectors.toList());
    }

    /**
     * Search recruiters by user and search term with pagination
     */
    public Page<RecruiterContact> findByUserAndSearchTerm(String userId, String search, Pageable pageable)
            throws ExecutionException, InterruptedException {
        // Firestore doesn't support LIKE queries, so we need to fetch all and filter in-memory
        // For production, consider using Algolia or Elasticsearch for better search
        List<RecruiterContact> allRecruiters = getRecruitersCollection(userId).get().get()
                .getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterContact.class))
                .collect(Collectors.toList());

        String searchLower = search.toLowerCase();
        List<RecruiterContact> filtered = allRecruiters.stream()
                .filter(r -> (r.getRecruiterName() != null && r.getRecruiterName().toLowerCase().contains(searchLower))
                        || (r.getEmail() != null && r.getEmail().toLowerCase().contains(searchLower))
                        || (r.getCompanyName() != null && r.getCompanyName().toLowerCase().contains(searchLower))
                        || (r.getJobRole() != null && r.getJobRole().toLowerCase().contains(searchLower)))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<RecruiterContact> page = filtered.subList(start, end);

        return new PageImpl<>(page, pageable, filtered.size());
    }

    /**
     * Search recruiters by user, status, and search term with pagination
     */
    public Page<RecruiterContact> findByUserAndStatusAndSearchTerm(String userId, String status,
                                                                     String search, Pageable pageable)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getRecruitersCollection(userId)
                .whereEqualTo("status", status)
                .get()
                .get();

        String searchLower = search.toLowerCase();
        List<RecruiterContact> filtered = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterContact.class))
                .filter(r -> (r.getRecruiterName() != null && r.getRecruiterName().toLowerCase().contains(searchLower))
                        || (r.getEmail() != null && r.getEmail().toLowerCase().contains(searchLower))
                        || (r.getCompanyName() != null && r.getCompanyName().toLowerCase().contains(searchLower))
                        || (r.getJobRole() != null && r.getJobRole().toLowerCase().contains(searchLower)))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<RecruiterContact> page = filtered.subList(start, end);

        return new PageImpl<>(page, pageable, filtered.size());
    }

    /**
     * Find recruiters by user and company name with pagination
     */
    public Page<RecruiterContact> findByUserAndCompanyName(String userId, String companyName, Pageable pageable)
            throws ExecutionException, InterruptedException {
        Query query = getRecruitersCollection(userId)
                .whereEqualTo("companyName", companyName)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        QuerySnapshot querySnapshot = query.get().get();
        List<RecruiterContact> recruiters = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterContact.class))
                .collect(Collectors.toList());

        // Count total for this company
        long total = getRecruitersCollection(userId)
                .whereEqualTo("companyName", companyName)
                .get()
                .get()
                .size();

        return new PageImpl<>(recruiters, pageable, total);
    }

    /**
     * Find recruiters by user, company, and search term with pagination
     */
    public Page<RecruiterContact> findByUserAndCompanyAndSearchTerm(String userId, String company,
                                                                      String search, Pageable pageable)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getRecruitersCollection(userId)
                .whereEqualTo("companyName", company)
                .get()
                .get();

        String searchLower = search.toLowerCase();
        List<RecruiterContact> filtered = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterContact.class))
                .filter(r -> (r.getRecruiterName() != null && r.getRecruiterName().toLowerCase().contains(searchLower))
                        || (r.getEmail() != null && r.getEmail().toLowerCase().contains(searchLower))
                        || (r.getCompanyName() != null && r.getCompanyName().toLowerCase().contains(searchLower))
                        || (r.getJobRole() != null && r.getJobRole().toLowerCase().contains(searchLower)))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<RecruiterContact> page = filtered.subList(start, end);

        return new PageImpl<>(page, pageable, filtered.size());
    }

    /**
     * Find recruiters by user, status, and company with pagination
     */
    public Page<RecruiterContact> findByUserAndStatusAndCompany(String userId, String status,
                                                                  String company, Pageable pageable)
            throws ExecutionException, InterruptedException {
        // Firestore requires composite index for multiple where clauses
        // For now, fetch and filter in-memory
        QuerySnapshot querySnapshot = getRecruitersCollection(userId)
                .whereEqualTo("status", status)
                .whereEqualTo("companyName", company)
                .get()
                .get();

        List<RecruiterContact> recruiters = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterContact.class))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), recruiters.size());
        List<RecruiterContact> page = recruiters.subList(start, end);

        return new PageImpl<>(page, pageable, recruiters.size());
    }

    /**
     * Find recruiters by user, status, company, and search term with pagination
     */
    public Page<RecruiterContact> findByUserAndStatusAndCompanyAndSearchTerm(String userId, String status,
                                                                               String company, String search,
                                                                               Pageable pageable)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getRecruitersCollection(userId)
                .whereEqualTo("status", status)
                .whereEqualTo("companyName", company)
                .get()
                .get();

        String searchLower = search.toLowerCase();
        List<RecruiterContact> filtered = querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(RecruiterContact.class))
                .filter(r -> (r.getRecruiterName() != null && r.getRecruiterName().toLowerCase().contains(searchLower))
                        || (r.getEmail() != null && r.getEmail().toLowerCase().contains(searchLower))
                        || (r.getCompanyName() != null && r.getCompanyName().toLowerCase().contains(searchLower))
                        || (r.getJobRole() != null && r.getJobRole().toLowerCase().contains(searchLower)))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<RecruiterContact> page = filtered.subList(start, end);

        return new PageImpl<>(page, pageable, filtered.size());
    }

    /**
     * Count recruiters by user and status
     */
    public Long countByUserAndStatus(String userId, String status)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getRecruitersCollection(userId)
                .whereEqualTo("status", status)
                .get()
                .get();
        return (long) querySnapshot.size();
    }

    /**
     * Delete recruiter
     */
    public void delete(String userId, String recruiterId)
            throws ExecutionException, InterruptedException {
        getRecruitersCollection(userId).document(recruiterId).delete().get();
        log.debug("RecruiterContact deleted: userId={}, recruiterId={}", userId, recruiterId);
    }

    /**
     * Delete recruiter entity
     */
    public void delete(String userId, RecruiterContact recruiter)
            throws ExecutionException, InterruptedException {
        if (recruiter.getId() != null) {
            delete(userId, recruiter.getId());
        }
    }

    /**
     * Check if recruiter exists
     */
    public boolean existsById(String userId, String recruiterId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getRecruitersCollection(userId)
                .document(recruiterId)
                .get()
                .get();
        return doc.exists();
    }
}
