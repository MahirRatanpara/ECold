package com.ecold.controller;

import com.ecold.dto.EmailTemplateDto;
import com.ecold.entity.EmailTemplate;
import com.ecold.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/email-templates")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:3000"})
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    @GetMapping
    public ResponseEntity<List<EmailTemplateDto>> getAllTemplates() {
        List<EmailTemplateDto> templates = emailTemplateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/search")
    public ResponseEntity<List<EmailTemplateDto>> searchTemplates(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        
        EmailTemplate.Category categoryEnum = null;
        EmailTemplate.Status statusEnum = null;
        
        if (category != null && !category.isEmpty()) {
            try {
                categoryEnum = EmailTemplate.Category.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = EmailTemplate.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        List<EmailTemplateDto> templates = emailTemplateService.searchTemplates(query, categoryEnum, statusEnum);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<EmailTemplateDto>> getTemplatesByCategory(@PathVariable String category) {
        try {
            EmailTemplate.Category categoryEnum = EmailTemplate.Category.valueOf(category.toUpperCase());
            List<EmailTemplateDto> templates = emailTemplateService.getTemplatesByCategory(categoryEnum);
            return ResponseEntity.ok(templates);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EmailTemplateDto>> getTemplatesByStatus(@PathVariable String status) {
        try {
            EmailTemplate.Status statusEnum = EmailTemplate.Status.valueOf(status.toUpperCase());
            List<EmailTemplateDto> templates = emailTemplateService.getTemplatesByStatus(statusEnum);
            return ResponseEntity.ok(templates);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplateDto> getTemplateById(@PathVariable Long id) {
        EmailTemplateDto template = emailTemplateService.getTemplateById(id);
        return ResponseEntity.ok(template);
    }

    @PostMapping
    public ResponseEntity<EmailTemplateDto> createTemplate(@Valid @RequestBody EmailTemplateDto templateDto) {
        EmailTemplateDto created = emailTemplateService.createTemplate(templateDto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplateDto> updateTemplate(
            @PathVariable Long id, 
            @Valid @RequestBody EmailTemplateDto templateDto) {
        EmailTemplateDto updated = emailTemplateService.updateTemplate(id, templateDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        emailTemplateService.deleteTemplate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<EmailTemplateDto> duplicateTemplate(@PathVariable Long id) {
        EmailTemplateDto duplicated = emailTemplateService.duplicateTemplate(id);
        return ResponseEntity.ok(duplicated);
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<Void> archiveTemplate(@PathVariable Long id) {
        emailTemplateService.archiveTemplate(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Void> activateTemplate(@PathVariable Long id) {
        emailTemplateService.activateTemplate(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/use")
    public ResponseEntity<Void> useTemplate(@PathVariable Long id) {
        emailTemplateService.incrementUsage(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTemplateStats() {
        Map<String, Object> stats = emailTemplateService.getTemplateStats();
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<Void> clearAllTemplates() {
        emailTemplateService.clearAllTemplates();
        return ResponseEntity.ok().build();
    }

}