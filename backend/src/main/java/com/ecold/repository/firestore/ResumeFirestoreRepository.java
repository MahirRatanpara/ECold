package com.ecold.repository.firestore;

import com.ecold.entity.Resume;
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
 * Firestore Repository for Resume entity
 * Path: /users/{userId}/resumes/{resumeId}
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ResumeFirestoreRepository {

    private final Firestore firestore;
    private static final String USERS_COLLECTION = "users";
    private static final String RESUMES_COLLECTION = "resumes";

    /**
     * Get resumes collection reference for a user
     */
    private CollectionReference getResumesCollection(String userId) {
        return firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(RESUMES_COLLECTION);
    }

    /**
     * Save or update a resume
     */
    public Resume save(String userId, Resume resume)
            throws ExecutionException, InterruptedException {
        CollectionReference resumes = getResumesCollection(userId);

        if (resume.getId() == null || resume.getId().isEmpty()) {
            DocumentReference docRef = resumes.document();
            resume.setId(docRef.getId());
            resume.setCreatedAt(Timestamp.now());
        }

        resume.setUserId(userId);
        resume.setUpdatedAt(Timestamp.now());
        resumes.document(resume.getId()).set(resume).get();

        log.debug("Resume saved: userId={}, resumeId={}", userId, resume.getId());
        return resume;
    }

    /**
     * Find resume by ID
     */
    public Optional<Resume> findById(String userId, String resumeId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getResumesCollection(userId)
                .document(resumeId)
                .get()
                .get();

        if (doc.exists()) {
            return Optional.of(doc.toObject(Resume.class));
        }
        return Optional.empty();
    }

    /**
     * Find all resumes for a user
     */
    public List<Resume> findByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getResumesCollection(userId).get().get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Resume.class))
                .collect(Collectors.toList());
    }

    /**
     * Find default resume for a user
     */
    public Optional<Resume> findDefaultByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getResumesCollection(userId)
                .whereEqualTo("isDefault", true)
                .limit(1)
                .get()
                .get();

        if (!querySnapshot.getDocuments().isEmpty()) {
            return Optional.of(querySnapshot.getDocuments().get(0).toObject(Resume.class));
        }
        return Optional.empty();
    }

    /**
     * Find resume by name
     */
    public Optional<Resume> findByUserAndName(String userId, String name)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getResumesCollection(userId)
                .whereEqualTo("name", name)
                .limit(1)
                .get()
                .get();

        if (!querySnapshot.getDocuments().isEmpty()) {
            return Optional.of(querySnapshot.getDocuments().get(0).toObject(Resume.class));
        }
        return Optional.empty();
    }

    /**
     * Find resume by file name
     */
    public Optional<Resume> findByUserAndFileName(String userId, String fileName)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getResumesCollection(userId)
                .whereEqualTo("fileName", fileName)
                .limit(1)
                .get()
                .get();

        if (!querySnapshot.getDocuments().isEmpty()) {
            return Optional.of(querySnapshot.getDocuments().get(0).toObject(Resume.class));
        }
        return Optional.empty();
    }

    /**
     * Find resumes by content type
     */
    public List<Resume> findByUserAndContentType(String userId, String contentType)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getResumesCollection(userId)
                .whereEqualTo("contentType", contentType)
                .get()
                .get();

        return querySnapshot.getDocuments().stream()
                .map(doc -> doc.toObject(Resume.class))
                .collect(Collectors.toList());
    }

    /**
     * Set resume as default (and unset others)
     */
    public Resume setAsDefault(String userId, String resumeId)
            throws ExecutionException, InterruptedException {
        // First, unset all other default resumes
        List<Resume> currentDefaults = getResumesCollection(userId)
                .whereEqualTo("isDefault", true)
                .get()
                .get()
                .getDocuments()
                .stream()
                .map(doc -> doc.toObject(Resume.class))
                .collect(Collectors.toList());

        for (Resume defaultResume : currentDefaults) {
            if (!defaultResume.getId().equals(resumeId)) {
                defaultResume.setIsDefault(false);
                save(userId, defaultResume);
            }
        }

        // Set the new default
        Optional<Resume> resumeOpt = findById(userId, resumeId);
        if (resumeOpt.isPresent()) {
            Resume resume = resumeOpt.get();
            resume.setIsDefault(true);
            return save(userId, resume);
        }

        throw new IllegalArgumentException("Resume not found: " + resumeId);
    }

    /**
     * Count resumes for a user
     */
    public long countByUser(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot querySnapshot = getResumesCollection(userId).get().get();
        return querySnapshot.size();
    }

    /**
     * Delete resume
     */
    public void delete(String userId, String resumeId)
            throws ExecutionException, InterruptedException {
        getResumesCollection(userId).document(resumeId).delete().get();
        log.debug("Resume deleted: userId={}, resumeId={}", userId, resumeId);
    }

    /**
     * Delete resume entity
     */
    public void delete(String userId, Resume resume)
            throws ExecutionException, InterruptedException {
        if (resume.getId() != null) {
            delete(userId, resume.getId());
        }
    }

    /**
     * Check if resume exists
     */
    public boolean existsById(String userId, String resumeId)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = getResumesCollection(userId)
                .document(resumeId)
                .get()
                .get();
        return doc.exists();
    }

    /**
     * Check if resume exists by name
     */
    public boolean existsByUserAndName(String userId, String name)
            throws ExecutionException, InterruptedException {
        return findByUserAndName(userId, name).isPresent();
    }

    /**
     * Check if resume exists by file name
     */
    public boolean existsByUserAndFileName(String userId, String fileName)
            throws ExecutionException, InterruptedException {
        return findByUserAndFileName(userId, fileName).isPresent();
    }
}
