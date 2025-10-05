package com.ecold.service.impl;

import com.ecold.dto.RecruiterContactDto;
import com.ecold.entity.RecruiterContact;
import com.ecold.entity.User;
import com.ecold.repository.firestore.RecruiterContactFirestoreRepository;
import com.ecold.repository.firestore.UserFirestoreRepository;
import com.ecold.service.RecruiterService;
import com.ecold.service.RecruiterTemplateAssignmentService;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruiterServiceImpl implements RecruiterService {

    private final RecruiterContactFirestoreRepository recruiterFirestoreRepository;
    private final UserFirestoreRepository userFirestoreRepository;
    private final RecruiterTemplateAssignmentService assignmentService;

    @Override
    public Page<RecruiterContactDto> getRecruiters(int page, int size, String status, String search, String company) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<RecruiterContact> recruiters;
            User currentUser = getCurrentUser();

            // Handle different combinations of filters
            boolean hasStatus = status != null && !status.isEmpty();
            boolean hasSearch = search != null && !search.trim().isEmpty();
            boolean hasCompany = company != null && !company.trim().isEmpty();

            if (hasStatus && hasSearch && hasCompany) {
                // All three filters
                recruiters = recruiterFirestoreRepository.findByUserAndStatusAndCompanyAndSearchTerm(
                    currentUser.getId(), status.toUpperCase(), company.trim(), search.trim(), pageable);
            } else if (hasStatus && hasCompany) {
                // Status + Company
                recruiters = recruiterFirestoreRepository.findByUserAndStatusAndCompany(
                    currentUser.getId(), status.toUpperCase(), company.trim(), pageable);
            } else if (hasStatus && hasSearch) {
                // Status + Search
                recruiters = recruiterFirestoreRepository.findByUserAndStatusAndSearchTerm(
                    currentUser.getId(), status.toUpperCase(), search.trim(), pageable);
            } else if (hasCompany && hasSearch) {
                // Company + Search
                recruiters = recruiterFirestoreRepository.findByUserAndCompanyAndSearchTerm(
                    currentUser.getId(), company.trim(), search.trim(), pageable);
            } else if (hasStatus) {
                // Only status filter
                recruiters = recruiterFirestoreRepository.findByUserAndStatus(
                    currentUser.getId(), status.toUpperCase(), pageable);
            } else if (hasSearch) {
                // Only search filter
                recruiters = recruiterFirestoreRepository.findByUserAndSearchTerm(
                    currentUser.getId(), search.trim(), pageable);
            } else if (hasCompany) {
                // Only company filter
                recruiters = recruiterFirestoreRepository.findByUserAndCompanyName(
                    currentUser.getId(), company.trim(), pageable);
            } else {
                // No filters - show only user-specific recruiters
                recruiters = recruiterFirestoreRepository.findByUser(currentUser.getId(), pageable);
            }

            List<RecruiterContactDto> dtos = recruiters.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

            return new PageImpl<>(dtos, pageable, recruiters.getTotalElements());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching recruiters", e);
        }
    }

    @Override
    public RecruiterContactDto createRecruiter(RecruiterContactDto recruiterDto) {
        return createRecruiter(recruiterDto, null);
    }

    @Override
    public RecruiterContactDto createRecruiter(RecruiterContactDto recruiterDto, String templateId) {
        try {
            RecruiterContact recruiter = convertToEntity(recruiterDto);
            User currentUser = getCurrentUser();
            recruiter.setUserId(currentUser.getId());
            recruiter.setCreatedAt(Timestamp.now());
            recruiter.setUpdatedAt(Timestamp.now());

            RecruiterContact saved = recruiterFirestoreRepository.save(currentUser.getId(), recruiter);

            // If templateId is provided, create template assignment
            if (templateId != null) {
                try {
                    assignmentService.assignRecruiterToTemplate(saved.getId(), templateId);
                    log.debug("Successfully assigned recruiter {} to template {}", saved.getId(), templateId);
                } catch (Exception e) {
                    log.error("Failed to assign recruiter {} to template {}: {}", saved.getId(), templateId, e.getMessage());
                    // Don't fail the recruiter creation if assignment fails
                }
            }

            return convertToDto(saved);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error creating recruiter", e);
        }
    }

    @Override
    public RecruiterContactDto getRecruiterById(String id) {
        try {
            User currentUser = getCurrentUser();
            RecruiterContact recruiter = recruiterFirestoreRepository.findById(currentUser.getId(), id)
                .orElseThrow(() -> new RuntimeException("Recruiter not found with id: " + id));
            return convertToDto(recruiter);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching recruiter by id", e);
        }
    }

    @Override
    public RecruiterContactDto updateRecruiter(String id, RecruiterContactDto recruiterDto) {
        try {
            User currentUser = getCurrentUser();
            RecruiterContact existing = recruiterFirestoreRepository.findById(currentUser.getId(), id)
                .orElseThrow(() -> new RuntimeException("Recruiter not found with id: " + id));

            // Check if status is changing from PENDING to CONTACTED
            boolean isStatusChangingToContacted = existing.getStatusEnum() == RecruiterContact.ContactStatus.PENDING
                && recruiterDto.getStatus() == RecruiterContact.ContactStatus.CONTACTED;

            existing.setEmail(recruiterDto.getEmail());
            existing.setRecruiterName(recruiterDto.getRecruiterName());
            existing.setCompanyName(recruiterDto.getCompanyName());
            existing.setJobRole(recruiterDto.getJobRole());
            existing.setLinkedinProfile(recruiterDto.getLinkedinProfile());
            existing.setNotes(recruiterDto.getNotes());
            existing.setStatusEnum(recruiterDto.getStatus());
            existing.setUpdatedAt(Timestamp.now());

            // Update lastContactedAt when status changes to CONTACTED
            if (isStatusChangingToContacted) {
                existing.setLastContactedAt(Timestamp.now());
            }

            RecruiterContact updated = recruiterFirestoreRepository.save(currentUser.getId(), existing);
            return convertToDto(updated);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error updating recruiter", e);
        }
    }

    @Override
    public void deleteRecruiter(String id) {
        try {
            User currentUser = getCurrentUser();
            if (!recruiterFirestoreRepository.existsById(currentUser.getId(), id)) {
                throw new RuntimeException("Recruiter not found with id: " + id);
            }
            recruiterFirestoreRepository.delete(currentUser.getId(), id);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error deleting recruiter", e);
        }
    }

    @Override
    public RecruiterContactDto markAsContacted(String id) {
        try {
            User currentUser = getCurrentUser();
            RecruiterContact existing = recruiterFirestoreRepository.findById(currentUser.getId(), id)
                .orElseThrow(() -> new RuntimeException("Recruiter not found with id: " + id));

            existing.setStatusEnum(RecruiterContact.ContactStatus.CONTACTED);
            existing.setLastContactedAt(Timestamp.now());
            existing.setUpdatedAt(Timestamp.now());

            RecruiterContact updated = recruiterFirestoreRepository.save(currentUser.getId(), existing);
            return convertToDto(updated);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error marking recruiter as contacted", e);
        }
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
                    if (recruiterFirestoreRepository.findByUserAndEmail(currentUser.getId(), dto.getEmail()).isPresent()) {
                        skipCount++;
                        lineNumber++;
                        continue;
                    }

                    RecruiterContact entity = convertToEntity(dto);
                    entity.setUserId(currentUser.getId());
                    entity.setCreatedAt(Timestamp.now());
                    entity.setUpdatedAt(Timestamp.now());

                    RecruiterContact saved = recruiterFirestoreRepository.save(currentUser.getId(), entity);
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
    public List<RecruiterContactDto> importFromCsv(MultipartFile file, String templateId) {
        // Import recruiters normally first
        List<RecruiterContactDto> imported = importFromCsv(file);

        // If templateId is provided, assign all imported recruiters to the template
        if (templateId != null && !imported.isEmpty()) {
            try {
                List<String> recruiterIds = imported.stream()
                    .map(RecruiterContactDto::getId)
                    .collect(Collectors.toList());
                assignmentService.bulkAssignRecruitersToTemplate(recruiterIds, templateId);
                log.debug("Successfully assigned {} imported recruiters to template {}", imported.size(), templateId);
            } catch (Exception e) {
                log.error("Failed to assign imported recruiters to template {}: {}", templateId, e.getMessage());
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
        try {
            List<RecruiterContactDto> imported = new ArrayList<>();
            User currentUser = getCurrentUser();

            for (RecruiterContactDto dto : recruiters) {
                if (recruiterFirestoreRepository.findByUserAndEmail(currentUser.getId(), dto.getEmail()).isEmpty()) {
                    RecruiterContact entity = convertToEntity(dto);
                    entity.setUserId(currentUser.getId());
                    entity.setCreatedAt(Timestamp.now());
                    entity.setUpdatedAt(Timestamp.now());

                    RecruiterContact saved = recruiterFirestoreRepository.save(currentUser.getId(), entity);
                    imported.add(convertToDto(saved));
                }
            }

            return imported;
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error importing manual recruiters", e);
        }
    }

    @Override
    public List<RecruiterContactDto> importManualFromCsv(String csvData) {
        // Simple implementation for testing - just return empty list for now
        return new ArrayList<>();
    }

    @Override
    public Object getRecruiterStats() {
        try {
            User currentUser = getCurrentUser();
            Map<String, Object> stats = new HashMap<>();
            long totalCount = recruiterFirestoreRepository.findByUser(currentUser.getId(), PageRequest.of(0, 1)).getTotalElements();
            stats.put("total", totalCount);
            stats.put("pending", recruiterFirestoreRepository.countByUserAndStatus(currentUser.getId(), RecruiterContact.ContactStatus.PENDING.name()));
            stats.put("contacted", recruiterFirestoreRepository.countByUserAndStatus(currentUser.getId(), RecruiterContact.ContactStatus.CONTACTED.name()));
            stats.put("responded", recruiterFirestoreRepository.countByUserAndStatus(currentUser.getId(), RecruiterContact.ContactStatus.RESPONDED.name()));
            return stats;
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching recruiter stats", e);
        }
    }

    @Override
    public List<RecruiterContactDto> getUncontactedRecruiters() {
        try {
            User currentUser = getCurrentUser();
            List<RecruiterContact> uncontacted = recruiterFirestoreRepository.findByUserAndStatus(
                currentUser.getId(), RecruiterContact.ContactStatus.PENDING.name());
            return uncontacted.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching uncontacted recruiters", e);
        }
    }

    @Override
    public void bulkDeleteRecruiters(List<String> ids) {
        try {
            User currentUser = getCurrentUser();
            // Delete all provided IDs for current user
            for (String id : ids) {
                if (recruiterFirestoreRepository.existsById(currentUser.getId(), id)) {
                    recruiterFirestoreRepository.delete(currentUser.getId(), id);
                }
            }
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error bulk deleting recruiters", e);
        }
    }

    @Override
    public List<RecruiterContactDto> bulkUpdateStatus(List<String> ids, String statusStr) {
        try {
            User currentUser = getCurrentUser();
            RecruiterContact.ContactStatus status;

            try {
                status = RecruiterContact.ContactStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status: " + statusStr);
            }

            List<RecruiterContact> updatedRecruiters = new ArrayList<>();
            Timestamp now = Timestamp.now();

            for (String id : ids) {
                RecruiterContact recruiter = recruiterFirestoreRepository.findById(currentUser.getId(), id)
                    .orElse(null);

                if (recruiter != null) {
                    recruiter.setStatusEnum(status);
                    recruiter.setUpdatedAt(now);
                    // Update lastContactedAt when status changes to CONTACTED
                    if (status == RecruiterContact.ContactStatus.CONTACTED) {
                        recruiter.setLastContactedAt(now);
                    }
                    RecruiterContact updated = recruiterFirestoreRepository.save(currentUser.getId(), recruiter);
                    updatedRecruiters.add(updated);
                }
            }

            return updatedRecruiters.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error bulk updating recruiter status", e);
        }
    }

    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
                String email = authentication.getName();
                return userFirestoreRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Current user not found: " + email));
            }

            // Fallback for testing - always return the first user consistently
            java.util.Optional<User> existingDefault = userFirestoreRepository.findByEmail("default@ecold.com");
            if (existingDefault.isPresent()) {
                return existingDefault.get();
            }

            // If no default user, get the first user by ID to ensure consistency
            return userFirestoreRepository.findAll().stream()
                .min((u1, u2) -> u1.getId().compareTo(u2.getId()))
                .orElseGet(() -> {
                    try {
                        // Create new default user if no users exist
                        User defaultUser = new User();
                        defaultUser.setEmail("default@ecold.com");
                        defaultUser.setName("Default User");
                        defaultUser.setPassword("defaultpassword");
                        defaultUser.setProviderEnum(User.Provider.LOCAL);
                        return userFirestoreRepository.save(defaultUser);
                    } catch (ExecutionException | InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Error creating default user", e);
                    }
                });
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching current user", e);
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
        dto.setStatus(entity.getStatusEnum());
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
        entity.setStatusEnum(dto.getStatus() != null ? dto.getStatus() : RecruiterContact.ContactStatus.PENDING);
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
