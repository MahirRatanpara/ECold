package com.ecold.service.impl;

import com.ecold.dto.RecruiterContactDto;
import com.ecold.entity.RecruiterContact;
import com.ecold.entity.User;
import com.ecold.repository.RecruiterContactRepository;
import com.ecold.repository.UserRepository;
import com.ecold.service.RecruiterService;
import com.ecold.service.RecruiterTemplateAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruiterServiceImpl implements RecruiterService {

    private final RecruiterContactRepository recruiterRepository;
    private final UserRepository userRepository;
    private final RecruiterTemplateAssignmentService assignmentService;

    @Override
    public Page<RecruiterContactDto> getRecruiters(int page, int size, String status, String search, String company) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RecruiterContact> recruiters;
        User currentUser = getCurrentUser();
        
        // First, try to find and assign orphaned recruiters to current user
        this.assignOrphanedRecruitersToCurrentUser(currentUser);
        
        // Handle different combinations of filters
        boolean hasStatus = status != null && !status.isEmpty();
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasCompany = company != null && !company.trim().isEmpty();
        
        try {
            if (hasStatus && hasSearch && hasCompany) {
                // All three filters
                RecruiterContact.ContactStatus statusEnum = RecruiterContact.ContactStatus.valueOf(status.toUpperCase());
                recruiters = recruiterRepository.findByUserAndStatusAndCompanyAndSearchTerm(currentUser, statusEnum, company.trim(), search.trim(), pageable);
            } else if (hasStatus && hasCompany) {
                // Status + Company
                RecruiterContact.ContactStatus statusEnum = RecruiterContact.ContactStatus.valueOf(status.toUpperCase());
                recruiters = recruiterRepository.findByUserAndStatusAndCompany(currentUser, statusEnum, company.trim(), pageable);
            } else if (hasStatus && hasSearch) {
                // Status + Search
                RecruiterContact.ContactStatus statusEnum = RecruiterContact.ContactStatus.valueOf(status.toUpperCase());
                recruiters = recruiterRepository.findByUserAndStatusAndSearchTerm(currentUser, statusEnum, search.trim(), pageable);
            } else if (hasCompany && hasSearch) {
                // Company + Search
                recruiters = recruiterRepository.findByUserAndCompanyAndSearchTerm(currentUser, company.trim(), search.trim(), pageable);
            } else if (hasStatus) {
                // Only status filter
                RecruiterContact.ContactStatus statusEnum = RecruiterContact.ContactStatus.valueOf(status.toUpperCase());
                recruiters = recruiterRepository.findByUserAndStatus(currentUser, statusEnum, pageable);
            } else if (hasSearch) {
                // Only search filter
                recruiters = recruiterRepository.findByUserAndSearchTerm(currentUser, search.trim(), pageable);
            } else if (hasCompany) {
                // Only company filter
                recruiters = recruiterRepository.findByUserAndCompanyName(currentUser, company.trim(), pageable);
            } else {
                // No filters - show only user-specific recruiters
                recruiters = recruiterRepository.findByUser(currentUser, pageable);
            }
        } catch (IllegalArgumentException e) {
            // Invalid status, fallback based on other filters
            if (hasSearch && hasCompany) {
                recruiters = recruiterRepository.findByUserAndCompanyAndSearchTerm(currentUser, company.trim(), search.trim(), pageable);
            } else if (hasSearch) {
                recruiters = recruiterRepository.findByUserAndSearchTerm(currentUser, search.trim(), pageable);
            } else if (hasCompany) {
                recruiters = recruiterRepository.findByUserAndCompanyName(currentUser, company.trim(), pageable);
            } else {
                recruiters = recruiterRepository.findByUser(currentUser, pageable);
            }
        }
        
        List<RecruiterContactDto> dtos = recruiters.getContent().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        
        // Debug: System.out.println("Returning " + dtos.size() + " recruiters as DTOs. Total elements: " + recruiters.getTotalElements());
            
        return new PageImpl<>(dtos, pageable, recruiters.getTotalElements());
    }

    @Override
    public RecruiterContactDto createRecruiter(RecruiterContactDto recruiterDto) {
        return createRecruiter(recruiterDto, null);
    }

    @Override
    public RecruiterContactDto createRecruiter(RecruiterContactDto recruiterDto, Long templateId) {
        RecruiterContact recruiter = convertToEntity(recruiterDto);
        User currentUser = getCurrentUser();
        recruiter.setUser(currentUser);
        recruiter.setCreatedAt(LocalDateTime.now());
        recruiter.setUpdatedAt(LocalDateTime.now());

        RecruiterContact saved = recruiterRepository.save(recruiter);

        // If templateId is provided, create template assignment
        if (templateId != null) {
            try {
                assignmentService.assignRecruiterToTemplate(saved.getId(), templateId);
                System.out.println("Successfully assigned recruiter " + saved.getId() + " to template " + templateId);
            } catch (Exception e) {
                System.err.println("Failed to assign recruiter " + saved.getId() + " to template " + templateId + ": " + e.getMessage());
                // Don't fail the recruiter creation if assignment fails
            }
        }

        return convertToDto(saved);
    }

    @Override
    public RecruiterContactDto getRecruiterById(Long id) {
        RecruiterContact recruiter = recruiterRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recruiter not found with id: " + id));
        return convertToDto(recruiter);
    }

    @Override
    public RecruiterContactDto updateRecruiter(Long id, RecruiterContactDto recruiterDto) {
        RecruiterContact existing = recruiterRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recruiter not found with id: " + id));
            
        // Check if status is changing from PENDING to CONTACTED
        boolean isStatusChangingToContacted = existing.getStatus() == RecruiterContact.ContactStatus.PENDING 
            && recruiterDto.getStatus() == RecruiterContact.ContactStatus.CONTACTED;
            
        existing.setEmail(recruiterDto.getEmail());
        existing.setRecruiterName(recruiterDto.getRecruiterName());
        existing.setCompanyName(recruiterDto.getCompanyName());
        existing.setJobRole(recruiterDto.getJobRole());
        existing.setLinkedinProfile(recruiterDto.getLinkedinProfile());
        existing.setNotes(recruiterDto.getNotes());
        existing.setStatus(recruiterDto.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        
        // Update lastContactedAt when status changes to CONTACTED
        if (isStatusChangingToContacted) {
            existing.setLastContactedAt(LocalDateTime.now());
        }
        
        RecruiterContact updated = recruiterRepository.save(existing);
        return convertToDto(updated);
    }

    @Override
    public void deleteRecruiter(Long id) {
        if (!recruiterRepository.existsById(id)) {
            throw new RuntimeException("Recruiter not found with id: " + id);
        }
        recruiterRepository.deleteById(id);
    }

    @Override
    public RecruiterContactDto markAsContacted(Long id) {
        RecruiterContact existing = recruiterRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Recruiter not found with id: " + id));
            
        existing.setStatus(RecruiterContact.ContactStatus.CONTACTED);
        existing.setLastContactedAt(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        
        RecruiterContact updated = recruiterRepository.save(existing);
        return convertToDto(updated);
    }

    @Override
    public List<RecruiterContactDto> importFromCsv(MultipartFile file) {
        // Validate file
        validateCsvFile(file);
        
        List<RecruiterContactDto> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new RuntimeException("CSV file is empty or has no headers");
            }
            
            String[] headers = parseCSVLine(headerLine);
            Map<String, Integer> headerMap = new HashMap<>();
            
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i].trim().toLowerCase(), i);
            }
            
            // Validate required headers
            validateRequiredHeaders(headerMap);
            
            String line;
            int lineNumber = 2;
            int successCount = 0;
            int skipCount = 0;
            int errorCount = 0;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // Skip empty lines
                }
                
                try {
                    String[] values = parseCSVLine(line);
                    
                    if (values.length < 1) {
                        errors.add("Line " + lineNumber + ": Empty or invalid row");
                        errorCount++;
                        lineNumber++;
                        continue;
                    }
                    
                    // Validate email field
                    String email = getValue(values, headerMap, "email");
                    if (email == null || email.trim().isEmpty()) {
                        errors.add("Line " + lineNumber + ": Email is required");
                        errorCount++;
                        lineNumber++;
                        continue;
                    }
                    
                    // Basic email validation
                    if (!isValidEmail(email)) {
                        errors.add("Line " + lineNumber + ": Invalid email format '" + email + "'");
                        errorCount++;
                        lineNumber++;
                        continue;
                    }
                    
                    RecruiterContactDto dto = new RecruiterContactDto();
                    dto.setEmail(email);
                    dto.setRecruiterName(getValue(values, headerMap, "recruitername"));
                    dto.setCompanyName(getValue(values, headerMap, "company"));
                    dto.setJobRole(getValue(values, headerMap, "role"));
                    dto.setLinkedinProfile(getValue(values, headerMap, "linkedin"));
                    dto.setStatus(RecruiterContact.ContactStatus.PENDING);
                    
                    // Validate required fields
                    if (dto.getCompanyName() == null || dto.getCompanyName().trim().isEmpty()) {
                        errors.add("Line " + lineNumber + ": Company name is required");
                        errorCount++;
                        lineNumber++;
                        continue;
                    }
                    
                    if (dto.getJobRole() == null || dto.getJobRole().trim().isEmpty()) {
                        errors.add("Line " + lineNumber + ": Job role is required");
                        errorCount++;
                        lineNumber++;
                        continue;
                    }
                    
                    // Check if recruiter already exists for current user
                    User currentUser = getCurrentUser();
                    if (recruiterRepository.findByUserAndEmail(currentUser, dto.getEmail()).isPresent()) {
                        skipCount++;
                        lineNumber++;
                        continue;
                    }
                    
                    RecruiterContact entity = convertToEntity(dto);
                    entity.setUser(currentUser);
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    
                    RecruiterContact saved = recruiterRepository.save(entity);
                    imported.add(convertToDto(saved));
                    successCount++;
                    
                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": Error processing row - " + e.getMessage());
                    errorCount++;
                }
                lineNumber++;
            }
            
            // Report results
            if (errorCount > 0) {
                String errorSummary = String.format("Import completed with errors. Success: %d, Skipped: %d, Errors: %d. First few errors: %s", 
                    successCount, skipCount, errorCount, 
                    errors.stream().limit(3).collect(Collectors.joining("; ")));
                throw new RuntimeException(errorSummary);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error processing CSV file: " + e.getMessage(), e);
        }
        
        return imported;
    }

    @Override
    public List<RecruiterContactDto> importFromCsv(MultipartFile file, Long templateId) {
        // Import recruiters normally first
        List<RecruiterContactDto> imported = importFromCsv(file);

        // If templateId is provided, assign all imported recruiters to the template
        if (templateId != null && !imported.isEmpty()) {
            try {
                List<Long> recruiterIds = imported.stream()
                    .map(RecruiterContactDto::getId)
                    .collect(Collectors.toList());
                assignmentService.bulkAssignRecruitersToTemplate(recruiterIds, templateId);
                System.out.println("Successfully assigned " + imported.size() + " imported recruiters to template " + templateId);
            } catch (Exception e) {
                System.err.println("Failed to assign imported recruiters to template " + templateId + ": " + e.getMessage());
                // Don't fail the import if assignment fails
            }
        }

        return imported;
    }

    @Override
    public List<RecruiterContactDto> importFromExcel(MultipartFile file) {
        // TODO: Implement Excel import
        throw new RuntimeException("Excel import not yet implemented");
    }

    @Override
    public List<RecruiterContactDto> importManual(List<RecruiterContactDto> recruiters) {
        List<RecruiterContactDto> imported = new ArrayList<>();
        User currentUser = getCurrentUser();
        
        for (RecruiterContactDto dto : recruiters) {
            if (recruiterRepository.findByUserAndEmail(currentUser, dto.getEmail()).isEmpty()) {
                RecruiterContact entity = convertToEntity(dto);
                entity.setUser(currentUser);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());
                
                RecruiterContact saved = recruiterRepository.save(entity);
                imported.add(convertToDto(saved));
            }
        }
        
        return imported;
    }

    @Override
    public List<RecruiterContactDto> importManualFromCsv(String csvData) {
        // Simple implementation for testing - just return empty list for now
        return new ArrayList<>();
    }

    @Override
    public Object getRecruiterStats() {
        User currentUser = getCurrentUser();
        Map<String, Object> stats = new HashMap<>();
        long totalCount = recruiterRepository.findByUser(currentUser, PageRequest.of(0, 1)).getTotalElements();
        stats.put("total", totalCount);
        stats.put("pending", recruiterRepository.countByUserAndStatus(currentUser, RecruiterContact.ContactStatus.PENDING));
        stats.put("contacted", recruiterRepository.countByUserAndStatus(currentUser, RecruiterContact.ContactStatus.CONTACTED));
        stats.put("responded", recruiterRepository.countByUserAndStatus(currentUser, RecruiterContact.ContactStatus.RESPONDED));
        return stats;
    }

    @Override
    public List<RecruiterContactDto> getUncontactedRecruiters() {
        User currentUser = getCurrentUser();
        List<RecruiterContact> uncontacted = recruiterRepository.findByUserAndStatus(currentUser, RecruiterContact.ContactStatus.PENDING);
        return uncontacted.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @Override
    public void bulkDeleteRecruiters(List<Long> ids) {
        User currentUser = getCurrentUser();
        // Ensure all recruiters belong to current user before deleting
        List<RecruiterContact> recruitersToDelete = recruiterRepository.findAllById(ids);
        List<RecruiterContact> userRecruiters = recruitersToDelete.stream()
            .filter(recruiter -> recruiter.getUser().getId().equals(currentUser.getId()))
            .collect(Collectors.toList());
        
        if (userRecruiters.size() != ids.size()) {
            throw new RuntimeException("Some recruiters do not belong to the current user");
        }
        
        recruiterRepository.deleteAllById(ids);
    }

    @Override
    public List<RecruiterContactDto> bulkUpdateStatus(List<Long> ids, String statusStr) {
        User currentUser = getCurrentUser();
        RecruiterContact.ContactStatus status;
        
        try {
            status = RecruiterContact.ContactStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + statusStr);
        }
        
        // Ensure all recruiters belong to current user before updating
        List<RecruiterContact> recruitersToUpdate = recruiterRepository.findAllById(ids);
        List<RecruiterContact> userRecruiters = recruitersToUpdate.stream()
            .filter(recruiter -> recruiter.getUser().getId().equals(currentUser.getId()))
            .collect(Collectors.toList());
        
        if (userRecruiters.size() != ids.size()) {
            throw new RuntimeException("Some recruiters do not belong to the current user");
        }
        
        // Update status and lastContactedAt for all recruiters
        LocalDateTime now = LocalDateTime.now();
        List<RecruiterContact> updatedRecruiters = userRecruiters.stream()
            .map(recruiter -> {
                recruiter.setStatus(status);
                recruiter.setUpdatedAt(now);
                // Update lastContactedAt when status changes to CONTACTED
                if (status == RecruiterContact.ContactStatus.CONTACTED) {
                    recruiter.setLastContactedAt(now);
                }
                return recruiterRepository.save(recruiter);
            })
            .collect(Collectors.toList());
        
        return updatedRecruiters.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            String email = authentication.getName(); // Assuming email is used as username
            return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + email));
        }
        
        // Fallback for testing - find first user or create default
        return userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User defaultUser = new User();
            defaultUser.setEmail("default@ecold.com");
            defaultUser.setName("Default User");
            defaultUser.setPassword("defaultpassword");
            defaultUser.setProvider(User.Provider.LOCAL);
            return userRepository.save(defaultUser);
        });
    }

    private void assignOrphanedRecruitersToCurrentUser(User currentUser) {
        try {
            // Find recruiters without a user assigned (orphaned data from before user-isolation)
            List<RecruiterContact> orphanedRecruiters = recruiterRepository.findByUserIsNull();
            if (!orphanedRecruiters.isEmpty()) {
                System.out.println("Found " + orphanedRecruiters.size() + " orphaned recruiters, assigning to user: " + currentUser.getEmail());
                for (RecruiterContact recruiter : orphanedRecruiters) {
                    recruiter.setUser(currentUser);
                }
                recruiterRepository.saveAll(orphanedRecruiters);
                System.out.println("Successfully assigned orphaned recruiters to current user");
            }
        } catch (Exception e) {
            System.err.println("Error assigning orphaned recruiters: " + e.getMessage());
            // Don't throw exception, just log it and continue
        }
    }

    private String getValue(String[] values, Map<String, Integer> headerMap, String key) {
        Integer index = headerMap.get(key);
        if (index != null && index < values.length) {
            return values[index].trim().replaceAll("^\"|\"$", ""); // Remove quotes
        }
        return null;
    }

    private RecruiterContactDto convertToDto(RecruiterContact entity) {
        RecruiterContactDto dto = new RecruiterContactDto();
        dto.setId(entity.getId());
        dto.setEmail(entity.getEmail());
        dto.setRecruiterName(entity.getRecruiterName());
        dto.setCompanyName(entity.getCompanyName());
        dto.setJobRole(entity.getJobRole());
        dto.setLinkedinProfile(entity.getLinkedinProfile());
        dto.setNotes(entity.getNotes());
        dto.setStatus(entity.getStatus());
        dto.setLastContactedAt(entity.getLastContactedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private RecruiterContact convertToEntity(RecruiterContactDto dto) {
        RecruiterContact entity = new RecruiterContact();
        entity.setEmail(dto.getEmail());
        entity.setRecruiterName(dto.getRecruiterName());
        entity.setCompanyName(dto.getCompanyName());
        entity.setJobRole(dto.getJobRole());
        entity.setLinkedinProfile(dto.getLinkedinProfile());
        entity.setNotes(dto.getNotes());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : RecruiterContact.ContactStatus.PENDING);
        entity.setLastContactedAt(dto.getLastContactedAt());
        return entity;
    }
    
    // CSV validation helper methods
    private void validateCsvFile(MultipartFile file) {
        if (file == null) {
            throw new RuntimeException("No file uploaded");
        }
        
        if (file.isEmpty()) {
            throw new RuntimeException("Uploaded file is empty");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new RuntimeException("Invalid file format. Please upload a CSV file (.csv extension required)");
        }
        
        // Check file size (limit to 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("File too large. Maximum size allowed is 10MB");
        }
    }
    
    private void validateRequiredHeaders(Map<String, Integer> headerMap) {
        List<String> missingHeaders = new ArrayList<>();
        
        if (!headerMap.containsKey("email")) {
            missingHeaders.add("email");
        }
        if (!headerMap.containsKey("company")) {
            missingHeaders.add("company");
        }
        if (!headerMap.containsKey("role")) {
            missingHeaders.add("role");
        }
        
        if (!missingHeaders.isEmpty()) {
            throw new RuntimeException("Missing required CSV columns: " + String.join(", ", missingHeaders) + 
                ". Required columns are: email, company, role. Optional: recruiterName, linkedin");
        }
    }
    
    private String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString());
        
        return values.toArray(new String[0]);
    }
    
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }
}