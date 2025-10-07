package com.ecold.repository.firestore;

import com.ecold.entity.User;
import com.ecold.util.EncryptionUtil;
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
 *
 * Note: This repository automatically encrypts access tokens and refresh tokens
 * before saving to Firestore and decrypts them after fetching.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserFirestoreRepository {

    private final Firestore firestore;
    private final EncryptionUtil encryptionUtil;
    private static final String COLLECTION_NAME = "users";

    /**
     * Save or update a user
     * Automatically encrypts access and refresh tokens before saving
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

        // Create a copy to avoid modifying the original user object
        User encryptedUser = createEncryptedCopy(user);

        ApiFuture<WriteResult> result = users.document(encryptedUser.getId()).set(encryptedUser);
        result.get();

        log.debug("User saved with encrypted tokens: {}", user.getId());
        return user;
    }

    /**
     * Find user by ID
     * Automatically decrypts access and refresh tokens after fetching
     */
    public Optional<User> findById(String id) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            User user = document.toObject(User.class);
            decryptUserTokens(user);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * Find user by email
     * Automatically decrypts access and refresh tokens after fetching
     */
    public Optional<User> findByEmail(String email) throws ExecutionException, InterruptedException {
        Query query = firestore.collection(COLLECTION_NAME)
                .whereEqualTo("email", email)
                .limit(1);

        ApiFuture<QuerySnapshot> querySnapshot = query.get();
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

        if (!documents.isEmpty()) {
            User user = documents.get(0).toObject(User.class);
            decryptUserTokens(user);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * Find user by provider and provider ID
     * Automatically decrypts access and refresh tokens after fetching
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
            User user = documents.get(0).toObject(User.class);
            decryptUserTokens(user);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * Find user by refresh token
     * WARNING: This method cannot use Firestore queries because tokens are encrypted with random IVs.
     * It must fetch all users and decrypt tokens to find a match. Use sparingly.
     * For better performance, consider using user ID lookups instead.
     */
    public Optional<User> findByRefreshToken(String refreshToken)
            throws ExecutionException, InterruptedException {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Optional.empty();
        }

        // Fetch all users and find matching refresh token by decryption
        // This is not performant but necessary due to encryption
        log.warn("findByRefreshToken: Fetching all users to find match. Consider redesigning to avoid this.");

        List<User> allUsers = findAll();

        return allUsers.stream()
                .filter(user -> refreshToken.equals(user.getRefreshToken()))
                .findFirst();
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
     * Automatically decrypts access and refresh tokens after fetching
     */
    public List<User> findAll() throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> query = firestore.collection(COLLECTION_NAME).get();
        List<QueryDocumentSnapshot> documents = query.get().getDocuments();

        return documents.stream()
                .map(doc -> {
                    User user = doc.toObject(User.class);
                    decryptUserTokens(user);
                    return user;
                })
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

    /**
     * Creates an encrypted copy of the user for saving to Firestore
     * Encrypts access and refresh tokens
     */
    private User createEncryptedCopy(User user) {
        User encryptedUser = new User();
        encryptedUser.setId(user.getId());
        encryptedUser.setEmail(user.getEmail());
        encryptedUser.setName(user.getName());
        encryptedUser.setPassword(user.getPassword());
        encryptedUser.setProfilePicture(user.getProfilePicture());
        encryptedUser.setProvider(user.getProvider());
        encryptedUser.setProviderId(user.getProviderId());
        encryptedUser.setTokenExpiresAt(user.getTokenExpiresAt());
        encryptedUser.setCreatedAt(user.getCreatedAt());
        encryptedUser.setUpdatedAt(user.getUpdatedAt());

        // Encrypt tokens if they exist
        try {
            if (user.getAccessToken() != null && !user.getAccessToken().isEmpty()) {
                encryptedUser.setAccessToken(encryptionUtil.encrypt(user.getAccessToken()));
            }
            if (user.getRefreshToken() != null && !user.getRefreshToken().isEmpty()) {
                encryptedUser.setRefreshToken(encryptionUtil.encrypt(user.getRefreshToken()));
            }
        } catch (Exception e) {
            log.error("Error encrypting user tokens", e);
            throw new RuntimeException("Failed to encrypt user tokens", e);
        }

        return encryptedUser;
    }

    /**
     * Decrypts access and refresh tokens in the user object
     * Modifies the user object in place
     */
    private void decryptUserTokens(User user) {
        if (user == null) {
            return;
        }

        try {
            if (user.getAccessToken() != null && !user.getAccessToken().isEmpty()) {
                user.setAccessToken(encryptionUtil.decrypt(user.getAccessToken()));
            }
            if (user.getRefreshToken() != null && !user.getRefreshToken().isEmpty()) {
                user.setRefreshToken(encryptionUtil.decrypt(user.getRefreshToken()));
            }
        } catch (Exception e) {
            log.error("Error decrypting user tokens for user: {}", user.getId(), e);
            // Don't throw exception - just log it and leave tokens encrypted
            // This prevents breaking the application if decryption fails
        }
    }
}
