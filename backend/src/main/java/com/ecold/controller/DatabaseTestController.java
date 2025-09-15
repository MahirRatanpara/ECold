package com.ecold.controller;

import com.ecold.entity.User;
import com.ecold.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/database-test")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class DatabaseTestController {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Test basic connection
            Connection connection = dataSource.getConnection();
            result.put("connectionStatus", "SUCCESS");
            result.put("databaseUrl", connection.getMetaData().getURL());
            result.put("databaseUser", connection.getMetaData().getUserName());
            connection.close();
            
            // Test user repository
            List<User> users = userRepository.findAll();
            result.put("userCount", users.size());
            result.put("users", users);
            
            return ResponseEntity.ok(result);
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
            testUser.setProvider(User.Provider.LOCAL);
            
            User saved = userRepository.save(testUser);
            result.put("status", "SUCCESS");
            result.put("createdUser", saved);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("error", e.getMessage());
            result.put("errorClass", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(result);
        }
    }
}