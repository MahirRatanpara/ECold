package com.ecold.repository.firestore;

import com.ecold.entity.User;
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
 * Firestore Repository for User entity
 * Replaces JPA UserRepository with Firestore implementation
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserFirestoreRepository {

    private final Firestore firestore;
    private static final String COLLECTION_NAME = "users";

    /**
     * Save or update a user
     */
    public User save(User user) throws ExecutionException, InterruptedException {
        CollectionReference users = firestore.collection(COLLECTION_NAME);

        if (user.getId() == null || user.getId().isEmpty()) {
            // Create new user with auto-generated ID
            DocumentReference docRef = users.document();
            user.setId(docRef.getId());
            user.setCreatedAt(Timestamp.now());
        }

        user.setUpdatedAt(Timestamp.now());
        ApiFuture<WriteResult> result = users.document(user.getId()).set(user);
        result.get(); // Wait for completion

        log.debug("User saved: {}", user.getId());
        return user;
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            return Optional.of(document.toObject(User.class));
        }
        return Optional.empty();
    }

    /**
     * Find user by email
     */
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

    /**
     * Find user by provider and provider ID
     */
    public Optional<User> findByProviderAndProviderId(String provider, String providerId)
            throws ExecutionException, InterruptedException {
        Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("provider", provider)
                .whereEqualTo("providerId", providerId)
                .limit(1);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

        if (!documents.isEmpty()) {
            return Optional.of(documents.get(0).toObject(User.class));
        }
        return Optional.empty();
    }

    /**
     * Find user by refresh token
     */
    public Optional<User> findByRefreshToken(String refreshToken)
            throws ExecutionException, InterruptedException {
        Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("refreshToken", refreshToken)
                .limit(1);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

        if (!documents.isEmpty()) {
            return Optional.of(documents.get(0).toObject(User.class));
        }
        return Optional.empty();
    }

    /**
     * Check if user exists by email
     */
    public boolean existsByEmail(String email) throws ExecutionException, InterruptedException {
        Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("email", email)
                .limit(1);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        return !querySnapshot.get().getDocuments().isEmpty();
    }

    /**
     * Find all users
     */
    public List<User> findAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = query.get().getDocuments();

        return documents.stream()
                .map(doc -> doc.toObject(User.class))
                .collect(Collectors.toList());
    }

    /**
     * Delete user by ID
     */
    public void delete(String id) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> result = firestore.collection(COLLECTION_NAME)
                .document(id)
                .delete();
        result.get();
        log.debug("User deleted: {}", id);
    }

    /**
     * Delete user entity
     */
    public void delete(User user) throws ExecutionException, InterruptedException {
        if (user.getId() != null) {
            delete(user.getId());
        }
    }

    /**
     * Count all users
     */
    public long count() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME).get();
        return query.get().getDocuments().size();
    }

    /**
     * Check if user exists by ID
     */
    public boolean existsById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        return future.get().exists();
    }
}
