package com.ecold.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Firestore Configuration
 *
 * Initializes Firebase Admin SDK and provides Firestore instance as a Spring Bean.
 * Supports both classpath and file system resource loading for credentials.
 *
 * @author ECold Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class FirestoreConfig {

    @Value("${google.cloud.project-id}")
    private String projectId;

    @Value("${google.cloud.firestore.credentials-path}")
    private String credentialsPath;

    /**
     * Creates and configures Firestore instance as a Spring Bean.
     *
     * @return Firestore instance configured with Firebase Admin SDK
     * @throws IOException if credentials file cannot be read
     */
    @Bean
    public Firestore firestore() throws IOException {
        log.info("🔥 Initializing Firebase Firestore...");
        log.info("📍 Project ID: {}", projectId);
        log.info("🔑 Credentials path: {}", credentialsPath);

        try {
            InputStream serviceAccount = getCredentialsInputStream();

            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();

            // Initialize Firebase App if not already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase App initialized successfully");
            } else {
                log.info("ℹ️ Firebase App already initialized");
            }

            Firestore firestore = FirestoreClient.getFirestore();
            log.info("✅ Firestore instance created successfully");
            log.info("🚀 ECold is now using Firebase Firestore (FREE tier)");

            return firestore;
        } catch (IOException e) {
            log.error("❌ Failed to initialize Firestore", e);
            log.error("💡 Ensure firestore-key.json is in src/main/resources/");
            log.error("💡 Or set FIRESTORE_CREDENTIALS_PATH environment variable");
            throw e;
        }
    }

    /**
     * Gets InputStream for credentials from either classpath or file system.
     *
     * @return InputStream for credentials file
     * @throws IOException if file cannot be found or read
     */
    private InputStream getCredentialsInputStream() throws IOException {
        Resource resource;

        // Check if it's a classpath resource (e.g., "classpath:firestore-key.json")
        if (credentialsPath.startsWith("classpath:")) {
            String path = credentialsPath.substring("classpath:".length());
            resource = new ClassPathResource(path);
            log.debug("📂 Loading credentials from classpath: {}", path);
        }
        // Otherwise treat as file system path
        else {
            resource = new FileSystemResource(credentialsPath);
            log.debug("📂 Loading credentials from file system: {}", credentialsPath);
        }

        if (!resource.exists()) {
            throw new IOException("Firestore credentials file not found: " + credentialsPath);
        }

        return resource.getInputStream();
    }
}
