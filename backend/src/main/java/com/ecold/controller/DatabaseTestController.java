package com.ecold.controller;

import com.ecold.entity.User;
import com.ecold.repository.firestore.UserFirestoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/database-test")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class DatabaseTestController {

    @Autowired
    private UserFirestoreRepository userFirestoreRepository;

    @GetMapping("/connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Test Firestore connection by fetching all users
            List<User> users = userFirestoreRepository.findAll();
            result.put("connectionStatus", "SUCCESS");
            result.put("database", "Firestore");
            result.put("userCount", users.size());
            result.put("users", users);

            return ResponseEntity.ok(result);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            result.put("connectionStatus", "FAILED");
            result.put("error", e.getMessage());
            result.put("errorClass", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(result);
        } catch (Exception e) {
            result.put("connectionStatus", "FAILED");
            result.put("error", e.getMessage());
            result.put("errorClass", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/create-user")
    public ResponseEntity<Map<String, Object>> createTestUser() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Create a test user
            User testUser = new User();
            testUser.setEmail("test@ecold.com");
            testUser.setName("Test User");
            testUser.setPassword("testpassword");
            testUser.setProviderEnum(User.Provider.LOCAL);

            User saved = userFirestoreRepository.save(testUser);
            result.put("status", "SUCCESS");
            result.put("createdUser", saved);

            return ResponseEntity.ok(result);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            result.put("errorClass", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(result);
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            result.put("errorClass", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(result);
        }
    }
}
