package com.ecold.controller;

import com.ecold.dto.RecruiterContactDto;
import com.ecold.dto.RecruiterImportRequest;
import com.ecold.service.RecruiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/recruiters")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class RecruiterController {
    
    private final RecruiterService recruiterService;
    
    @GetMapping
    public ResponseEntity<Page<RecruiterContactDto>> getRecruiters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String company) {
        try {
            Page<RecruiterContactDto> recruiters = recruiterService.getRecruiters(page, size, status, search, company);
            return ResponseEntity.ok(recruiters);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    @PostMapping
    public ResponseEntity<RecruiterContactDto> createRecruiter(
            @Valid @RequestBody RecruiterContactDto recruiterDto,
            @RequestParam(required = false) Long templateId) {
        RecruiterContactDto created = recruiterService.createRecruiter(recruiterDto, templateId);
        return ResponseEntity.ok(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<RecruiterContactDto> updateRecruiter(
            @PathVariable Long id, 
            @Valid @RequestBody RecruiterContactDto recruiterDto) {
        RecruiterContactDto updated = recruiterService.updateRecruiter(id, recruiterDto);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecruiter(@PathVariable Long id) {
        recruiterService.deleteRecruiter(id);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{id}/mark-contacted")
    public ResponseEntity<RecruiterContactDto> markAsContacted(@PathVariable Long id) {
        RecruiterContactDto updated = recruiterService.markAsContacted(id);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/import/csv")
    public ResponseEntity<List<RecruiterContactDto>> importFromCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long templateId) {
        List<RecruiterContactDto> imported = recruiterService.importFromCsv(file, templateId);
        return ResponseEntity.ok(imported);
    }
    
    @PostMapping("/import/excel")
    public ResponseEntity<List<RecruiterContactDto>> importFromExcel(@RequestParam("file") MultipartFile file) {
        List<RecruiterContactDto> imported = recruiterService.importFromExcel(file);
        return ResponseEntity.ok(imported);
    }
    
    @PostMapping("/import/manual")
    public ResponseEntity<List<RecruiterContactDto>> importManual(@Valid @RequestBody RecruiterImportRequest request) {
        List<RecruiterContactDto> imported = recruiterService.importManualFromCsv(request.getCsvData());
        return ResponseEntity.ok(imported);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Object> getRecruiterStats() {
        Object stats = recruiterService.getRecruiterStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/uncontacted")
    public ResponseEntity<List<RecruiterContactDto>> getUncontactedRecruiters() {
        List<RecruiterContactDto> uncontacted = recruiterService.getUncontactedRecruiters();
        return ResponseEntity.ok(uncontacted);
    }
    
    @DeleteMapping("/bulk")
    public ResponseEntity<Void> bulkDeleteRecruiters(@RequestBody List<Long> ids) {
        recruiterService.bulkDeleteRecruiters(ids);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/bulk/status")
    public ResponseEntity<List<RecruiterContactDto>> bulkUpdateStatus(
            @RequestBody BulkStatusUpdateRequest request) {
        List<RecruiterContactDto> updated = recruiterService.bulkUpdateStatus(request.getIds(), request.getStatus());
        return ResponseEntity.ok(updated);
    }
    
    public static class BulkStatusUpdateRequest {
        private List<Long> ids;
        private String status;
        
        public List<Long> getIds() { return ids; }
        public void setIds(List<Long> ids) { this.ids = ids; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}