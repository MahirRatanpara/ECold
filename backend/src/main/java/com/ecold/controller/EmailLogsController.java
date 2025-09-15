package com.ecold.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/email-logs")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class EmailLogsController {
    
    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentEmails() {
        // Return mock data for now since EmailService might not be fully implemented
        List<Map<String, Object>> recentEmails = new ArrayList<>();
        
        // Mock recent emails data
        Map<String, Object> email1 = new HashMap<>();
        email1.put("id", 1L);
        email1.put("recipientEmail", "recruiter1@company.com");
        email1.put("subject", "Software Engineer Application - John Doe");
        email1.put("status", "DELIVERED");
        email1.put("sentAt", LocalDateTime.now().minusHours(2));
        recentEmails.add(email1);
        
        Map<String, Object> email2 = new HashMap<>();
        email2.put("id", 2L);
        email2.put("recipientEmail", "hr@techcorp.com");
        email2.put("subject", "Following up on my application");
        email2.put("status", "OPENED");
        email2.put("sentAt", LocalDateTime.now().minusHours(5));
        recentEmails.add(email2);
        
        Map<String, Object> email3 = new HashMap<>();
        email3.put("id", 3L);
        email3.put("recipientEmail", "recruiter@startup.io");
        email3.put("subject", "Full Stack Developer Position");
        email3.put("status", "SENT");
        email3.put("sentAt", LocalDateTime.now().minusHours(8));
        recentEmails.add(email3);
        
        return ResponseEntity.ok(recentEmails);
    }
}