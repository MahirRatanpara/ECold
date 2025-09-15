package com.ecold.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/incoming-emails")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class IncomingEmailsController {
    
    @GetMapping("/highlights")
    public ResponseEntity<List<Map<String, Object>>> getInboxHighlights() {
        // Return mock data for now
        List<Map<String, Object>> highlights = new ArrayList<>();
        
        // Mock highlight emails data
        Map<String, Object> email1 = new HashMap<>();
        email1.put("id", 1L);
        email1.put("senderEmail", "recruiter@techcompany.com");
        email1.put("subject", "Interview Invitation - Software Engineer Position");
        email1.put("category", "SHORTLIST_INTERVIEW");
        email1.put("receivedAt", LocalDateTime.now().minusHours(3));
        highlights.add(email1);
        
        Map<String, Object> email2 = new HashMap<>();
        email2.put("id", 2L);
        email2.put("senderEmail", "hr@startup.io");
        email2.put("subject", "Thank you for your application");
        email2.put("category", "APPLICATION_UPDATE");
        email2.put("receivedAt", LocalDateTime.now().minusHours(6));
        highlights.add(email2);
        
        Map<String, Object> email3 = new HashMap<>();
        email3.put("id", 3L);
        email3.put("senderEmail", "talent@bigcorp.com");
        email3.put("subject", "Exciting opportunity for Full Stack Developer");
        email3.put("category", "RECRUITER_OUTREACH");
        email3.put("receivedAt", LocalDateTime.now().minusHours(12));
        highlights.add(email3);
        
        return ResponseEntity.ok(highlights);
    }
    
    @GetMapping("/unread-counts")
    public ResponseEntity<Map<String, Long>> getUnreadCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("APPLICATION_UPDATE", 3L);
        counts.put("SHORTLIST_INTERVIEW", 1L);
        counts.put("REJECTION_CLOSED", 2L);
        counts.put("RECRUITER_OUTREACH", 5L);
        counts.put("GENERAL_INQUIRY", 1L);
        counts.put("TOTAL", 12L);
        
        return ResponseEntity.ok(counts);
    }
    
    @PostMapping("/scan")
    public ResponseEntity<Void> scanIncomingEmails() {
        // Mock implementation - just return success
        return ResponseEntity.ok().build();
    }
}