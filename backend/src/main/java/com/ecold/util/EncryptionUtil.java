package com.ecold.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Encryption utility for securing sensitive data like access tokens and refresh tokens
 * Uses AES-256-GCM encryption for strong security
 */
@Slf4j
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 16;
    private static final int AUTH_TAG_LENGTH = 128; // 128 bits
    private static final int SALT_LENGTH = 64;
    private static final int KEY_LENGTH = 256; // 256 bits
    private static final int ITERATION_COUNT = 100000;
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA512";

    private final String encryptionKey;
    private final SecureRandom secureRandom;

    public EncryptionUtil(@Value("${encryption.key:}") String encryptionKey) {
        this.encryptionKey = encryptionKey;
        this.secureRandom = new SecureRandom();

        if (encryptionKey == null || encryptionKey.isEmpty()) {
            log.warn("⚠️  ENCRYPTION_KEY not configured. Token encryption will not work!");
        }
    }

    /**
     * Encrypts a plaintext string
     * @param plaintext The text to encrypt (e.g., access token)
     * @return Encrypted string in format: salt:iv:encryptedData (all base64 encoded)
     * @throws RuntimeException If encryption fails or encryption key is not configured
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }

        if (encryptionKey == null || encryptionKey.isEmpty()) {
            throw new RuntimeException("ENCRYPTION_KEY environment variable is not set. Please configure it in your application.properties or environment.");
        }

        try {
            // Generate random salt and IV
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);

            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Derive key from encryption secret
            SecretKey key = deriveKey(encryptionKey, salt);

            // Create cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AUTH_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);

            // Encrypt the data
            byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine salt, iv, and encrypted data
            // Format: salt:iv:encryptedData (base64 encoded)
            return Base64.getEncoder().encodeToString(salt) + ":" +
                   Base64.getEncoder().encodeToString(iv) + ":" +
                   Base64.getEncoder().encodeToString(encryptedData);

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts an encrypted string
     * @param encryptedData The encrypted string in format: salt:iv:encryptedData
     * @return Decrypted plaintext
     * @throws RuntimeException If decryption fails or encryption key is not configured
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return null;
        }

        if (encryptionKey == null || encryptionKey.isEmpty()) {
            throw new RuntimeException("ENCRYPTION_KEY environment variable is not set. Please configure it in your application.properties or environment.");
        }

        try {
            // Split the encrypted data
            String[] parts = encryptedData.split(":");

            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid encrypted data format. Expected 3 parts, got " + parts.length);
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);

            // Derive the same key
            SecretKey key = deriveKey(encryptionKey, salt);

            // Create decipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AUTH_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);

            // Decrypt the data
            byte[] decryptedData = cipher.doFinal(encrypted);

            return new String(decryptedData, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Derives a key from the encryption secret using PBKDF2
     * @param secret The encryption secret
     * @param salt Salt for key derivation
     * @return Derived key
     */
    private SecretKey deriveKey(String secret, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(
            secret.toCharArray(),
            salt,
            ITERATION_COUNT,
            KEY_LENGTH
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Generates a secure random encryption key
     * Use this to generate a new ENCRYPTION_KEY for your environment
     * @return A random 64-character hex string
     */
    public static String generateEncryptionKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[32];
        random.nextBytes(keyBytes);

        StringBuilder hex = new StringBuilder();
        for (byte b : keyBytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * Check if encryption is properly configured
     * @return true if encryption key is set, false otherwise
     */
    public boolean isConfigured() {
        return encryptionKey != null && !encryptionKey.isEmpty();
    }
}
