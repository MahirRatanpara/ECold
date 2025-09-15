package com.ecold.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class DashboardController {
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        // For now, return mock data since the EmailService might not be fully implemented
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecruiters", 25);
        stats.put("emailsSent", 150);
        stats.put("responseRate", 18.5);
        stats.put("activeCampaigns", 3);
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/email-counts")
    public ResponseEntity<Map<String, Object>> getEmailCounts() {
        Map<String, Object> counts = new HashMap<>();
        counts.put("sent", 150);
        counts.put("delivered", 142);
        counts.put("opened", 28);
        counts.put("failed", 8);
        
        return ResponseEntity.ok(counts);
    }
}