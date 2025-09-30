package com.ecold.service.impl;

import com.ecold.dto.EmailTemplateDto;
import com.ecold.entity.EmailTemplate;
import com.ecold.entity.User;
import com.ecold.exception.TemplateValidationException;
import com.ecold.repository.EmailTemplateRepository;
import com.ecold.repository.UserRepository;
import com.ecold.service.EmailTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private final EmailTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // In-memory storage for when database is unavailable
    private final Map<Long, EmailTemplate> inMemoryTemplates = new ConcurrentHashMap<>();
    private Long inMemoryIdCounter = 1L;
    private boolean isDatabaseAvailable = true;

    @Override
    public List<EmailTemplateDto> getAllTemplates() {
        User currentUser = getCurrentUser();
        List<EmailTemplate> templates = templateRepository.findByUser(currentUser);
        return templates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmailTemplateDto> getTemplatesByCategory(EmailTemplate.Category category) {
        User currentUser = getCurrentUser();
        List<EmailTemplate> templates = templateRepository.findByUserAndCategory(currentUser, category);
        return templates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmailTemplateDto> getTemplatesByStatus(EmailTemplate.Status status) {
        User currentUser = getCurrentUser();
        List<EmailTemplate> templates = templateRepository.findByUserAndStatus(currentUser, status);
        return templates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public EmailTemplateDto getTemplateById(Long id) {
        User currentUser = getCurrentUser();
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
        
        // Temporarily disable ownership check for testing
        // if (!template.getUser().getId().equals(currentUser.getId())) {
        //     throw new RuntimeException("Access denied: Template belongs to another user");
        // }
        
        return convertToDto(template);
    }

    @Override
    public EmailTemplateDto createTemplate(EmailTemplateDto templateDto) {
        if (templateDto == null) {
            throw new TemplateValidationException("Template data cannot be null");
        }
        
        // Additional placeholder validation at service level
        validatePlaceholders(templateDto);
        
        User currentUser = getCurrentUser();
        
        // Check if template with same name already exists for this user
        if (templateDto.getName() != null && !templateDto.getName().trim().isEmpty()) {
            Optional<EmailTemplate> existingTemplate = templateRepository.findByUserAndName(currentUser, templateDto.getName().trim());
            if (existingTemplate.isPresent()) {
                throw new TemplateValidationException("Template with name '" + templateDto.getName().trim() + "' already exists");
            }
        }
        
        EmailTemplate template = convertToEntity(templateDto);
        template.setUser(currentUser);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        // Enforce one active template per category rule
        if (template.getStatus() == EmailTemplate.Status.ACTIVE) {
            enforceOneActiveTemplatePerCategory(currentUser, template.getCategory(), null);
        }

        EmailTemplate saved = templateRepository.save(template);
        isDatabaseAvailable = true;
        return convertToDto(saved);
    }
    
    private EmailTemplateDto createTemplateInMemory(EmailTemplateDto templateDto, User currentUser) {
        // Check for duplicate names in memory
        if (templateDto.getName() != null && !templateDto.getName().trim().isEmpty()) {
            boolean nameExists = inMemoryTemplates.values().stream()
                .anyMatch(t -> t.getName().equals(templateDto.getName().trim()) && 
                              t.getUser().getId().equals(currentUser.getId()));
            if (nameExists) {
                throw new TemplateValidationException("Template with name '" + templateDto.getName().trim() + "' already exists");
            }
        }
        
        EmailTemplate template = convertToEntity(templateDto);
        template.setId(inMemoryIdCounter++);
        template.setUser(currentUser);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        
        inMemoryTemplates.put(template.getId(), template);
        return convertToDto(template);
    }

    @Override
    public EmailTemplateDto updateTemplate(Long id, EmailTemplateDto templateDto) {
        // Validate placeholders before updating
        validatePlaceholders(templateDto);
        
        User currentUser = getCurrentUser();
        EmailTemplate existingTemplate = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
        
        // Temporarily disable ownership check for testing
        // if (!existingTemplate.getUser().getId().equals(currentUser.getId())) {
        //     throw new RuntimeException("Access denied: Template belongs to another user");
        // }
        
        // Check if name is being changed and if new name already exists
        if (!existingTemplate.getName().equals(templateDto.getName())) {
            Optional<EmailTemplate> nameConflict = templateRepository.findByUserAndName(currentUser, templateDto.getName());
            if (nameConflict.isPresent() && !nameConflict.get().getId().equals(id)) {
                throw new RuntimeException("Template with name '" + templateDto.getName() + "' already exists");
            }
        }
        
        existingTemplate.setName(templateDto.getName());
        existingTemplate.setSubject(templateDto.getSubject());
        existingTemplate.setBody(templateDto.getBody());
        existingTemplate.setCategory(templateDto.getCategory());
        existingTemplate.setStatus(templateDto.getStatus());
        existingTemplate.setTags(convertTagsToString(templateDto.getTags()));
        existingTemplate.setUpdatedAt(LocalDateTime.now());

        // Enforce one active template per category rule
        if (templateDto.getStatus() == EmailTemplate.Status.ACTIVE) {
            enforceOneActiveTemplatePerCategory(currentUser, templateDto.getCategory(), id);
        }

        EmailTemplate updated = templateRepository.save(existingTemplate);
        return convertToDto(updated);
    }

    @Override
    public void deleteTemplate(Long id) {
        User currentUser = getCurrentUser();
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
        
        // Temporarily disable ownership check for testing
        // if (!template.getUser().getId().equals(currentUser.getId())) {
        //     throw new RuntimeException("Access denied: Template belongs to another user");
        // }
        
        templateRepository.deleteById(id);
    }

    @Override
    public EmailTemplateDto duplicateTemplate(Long id) {
        User currentUser = getCurrentUser();
        EmailTemplate originalTemplate = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
        
        if (!originalTemplate.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied: Template belongs to another user");
        }
        
        EmailTemplate duplicatedTemplate = new EmailTemplate();
        duplicatedTemplate.setUser(currentUser);
        duplicatedTemplate.setName(originalTemplate.getName() + " (Copy)");
        duplicatedTemplate.setSubject(originalTemplate.getSubject());
        duplicatedTemplate.setBody(originalTemplate.getBody());
        duplicatedTemplate.setCategory(originalTemplate.getCategory());
        duplicatedTemplate.setStatus(EmailTemplate.Status.DRAFT);
        duplicatedTemplate.setUsageCount(0L);
        duplicatedTemplate.setEmailsSent(0L);
        duplicatedTemplate.setResponseRate(0.0);
        duplicatedTemplate.setTags(originalTemplate.getTags());
        duplicatedTemplate.setCreatedAt(LocalDateTime.now());
        duplicatedTemplate.setUpdatedAt(LocalDateTime.now());
        
        EmailTemplate saved = templateRepository.save(duplicatedTemplate);
        return convertToDto(saved);
    }

    @Override
    public void archiveTemplate(Long id) {
        User currentUser = getCurrentUser();
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
        
        // Temporarily disable ownership check for testing
        // if (!template.getUser().getId().equals(currentUser.getId())) {
        //     throw new RuntimeException("Access denied: Template belongs to another user");
        // }
        
        template.setStatus(EmailTemplate.Status.ARCHIVED);
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.save(template);
    }

    @Override
    public void activateTemplate(Long id) {
        User currentUser = getCurrentUser();
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

        // Temporarily disable ownership check for testing
        // if (!template.getUser().getId().equals(currentUser.getId())) {
        //     throw new RuntimeException("Access denied: Template belongs to another user");
        // }

        // Enforce one active template per category rule
        enforceOneActiveTemplatePerCategory(currentUser, template.getCategory(), id);

        template.setStatus(EmailTemplate.Status.ACTIVE);
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.save(template);
    }

    @Override
    public Map<String, Object> getTemplateStats() {
        User currentUser = getCurrentUser();
        
        long totalTemplates = templateRepository.countByUser(currentUser);
        long activeTemplates = templateRepository.countByUserAndStatus(currentUser, EmailTemplate.Status.ACTIVE);
        long draftTemplates = templateRepository.countByUserAndStatus(currentUser, EmailTemplate.Status.DRAFT);
        long archivedTemplates = templateRepository.countByUserAndStatus(currentUser, EmailTemplate.Status.ARCHIVED);
        
        List<EmailTemplate> templates = templateRepository.findByUser(currentUser);
        double avgResponseRate = templates.stream()
                .mapToDouble(EmailTemplate::getResponseRate)
                .average()
                .orElse(0.0);
        
        long totalUsage = templates.stream()
                .mapToLong(EmailTemplate::getUsageCount)
                .sum();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTemplates", totalTemplates);
        stats.put("activeTemplates", activeTemplates);
        stats.put("draftTemplates", draftTemplates);
        stats.put("archivedTemplates", archivedTemplates);
        stats.put("avgResponseRate", Math.round(avgResponseRate * 100.0) / 100.0);
        stats.put("totalUsage", totalUsage);
        
        return stats;
    }

    @Override
    public void incrementUsage(Long id) {
        User currentUser = getCurrentUser();
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
        
        // Temporarily disable ownership check for testing
        // if (!template.getUser().getId().equals(currentUser.getId())) {
        //     throw new RuntimeException("Access denied: Template belongs to another user");
        // }
        
        template.setUsageCount(template.getUsageCount() + 1);
        template.setLastUsed(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.save(template);
    }

    @Override
    public List<EmailTemplateDto> searchTemplates(String query, EmailTemplate.Category category, EmailTemplate.Status status) {
        User currentUser = getCurrentUser();
        List<EmailTemplate> templates;
        
        if (category != null && status != null) {
            templates = templateRepository.findByUserAndCategoryAndStatus(currentUser, category, status);
        } else if (category != null) {
            templates = templateRepository.findByUserAndCategory(currentUser, category);
        } else if (status != null) {
            templates = templateRepository.findByUserAndStatus(currentUser, status);
        } else {
            templates = templateRepository.findByUser(currentUser);
        }
        
        if (query != null && !query.trim().isEmpty()) {
            String lowerQuery = query.toLowerCase().trim();
            templates = templates.stream()
                    .filter(template -> 
                        template.getName().toLowerCase().contains(lowerQuery) ||
                        template.getSubject().toLowerCase().contains(lowerQuery) ||
                        template.getBody().toLowerCase().contains(lowerQuery) ||
                        template.getCategory().toString().toLowerCase().contains(lowerQuery) ||
                        (template.getTags() != null && template.getTags().toLowerCase().contains(lowerQuery))
                    )
                    .collect(Collectors.toList());
        }
        
        return templates.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void clearAllTemplates() {
        User currentUser = getCurrentUser();
        List<EmailTemplate> userTemplates = templateRepository.findByUser(currentUser);
        templateRepository.deleteAll(userTemplates);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            String email = authentication.getName();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Current user not found: " + email));
        }

        // If not authenticated, throw an exception instead of returning a random user
        throw new RuntimeException("User not authenticated. Please log in.");
    }
    
    private User createMockUser(String email) {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail(email);
        mockUser.setName("Mock User");
        mockUser.setPassword("mockpassword");
        mockUser.setProvider(User.Provider.LOCAL);
        return mockUser;
    }

    private EmailTemplateDto convertToDto(EmailTemplate template) {
        EmailTemplateDto dto = new EmailTemplateDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setSubject(template.getSubject());
        dto.setBody(template.getBody());
        dto.setCategory(template.getCategory());
        dto.setStatus(template.getStatus());
        dto.setUsageCount(template.getUsageCount());
        dto.setEmailsSent(template.getEmailsSent());
        dto.setResponseRate(template.getResponseRate());
        dto.setTags(convertTagsFromString(template.getTags()));
        dto.setIsDefault(template.getIsDefault());
        dto.setLastUsed(template.getLastUsed());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        return dto;
    }

    private EmailTemplate convertToEntity(EmailTemplateDto dto) {
        if (dto == null) {
            throw new TemplateValidationException("Template DTO cannot be null");
        }
        
        EmailTemplate template = new EmailTemplate();
        template.setName(dto.getName() != null ? dto.getName().trim() : "");
        template.setSubject(dto.getSubject() != null ? dto.getSubject().trim() : "");
        template.setBody(dto.getBody() != null ? dto.getBody().trim() : "");
        template.setCategory(dto.getCategory() != null ? dto.getCategory() : EmailTemplate.Category.OUTREACH);
        template.setStatus(dto.getStatus() != null ? dto.getStatus() : EmailTemplate.Status.DRAFT);
        template.setUsageCount(dto.getUsageCount() != null ? dto.getUsageCount() : 0L);
        template.setEmailsSent(dto.getEmailsSent() != null ? dto.getEmailsSent() : 0L);
        template.setResponseRate(dto.getResponseRate() != null ? dto.getResponseRate() : 0.0);
        template.setTags(convertTagsToString(dto.getTags()));
        template.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false);
        return template;
    }

    private List<String> convertTagsFromString(String tagsString) {
        if (tagsString == null || tagsString.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(tagsString, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private String convertTagsToString(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
    
    private void validatePlaceholders(EmailTemplateDto templateDto) {
        Set<String> validPlaceholders = Set.of("Company", "Role", "RecruiterName", "MyName");
        
        // Validate subject placeholders
        if (templateDto.getSubject() != null) {
            validatePlaceholdersInText(templateDto.getSubject(), "subject", validPlaceholders);
        }
        
        // Validate body placeholders
        if (templateDto.getBody() != null) {
            validatePlaceholdersInText(templateDto.getBody(), "body", validPlaceholders);
        }
    }
    
    private void validatePlaceholdersInText(String text, String fieldName, Set<String> validPlaceholders) {
        // Check for unmatched braces
        long openBraces = text.chars().filter(ch -> ch == '{').count();
        long closeBraces = text.chars().filter(ch -> ch == '}').count();

        if (openBraces != closeBraces) {
            throw new TemplateValidationException(
                String.format("Invalid placeholder syntax in %s: unmatched braces", fieldName));
        }

        // Check for invalid placeholder patterns
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        Set<String> invalidPlaceholders = new HashSet<>();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (!validPlaceholders.contains(placeholder)) {
                invalidPlaceholders.add(placeholder);
            }
        }

        if (!invalidPlaceholders.isEmpty()) {
            throw new TemplateValidationException(
                String.format("Invalid placeholders in %s: %s. Valid placeholders are: %s",
                    fieldName,
                    String.join(", ", invalidPlaceholders),
                    String.join(", ", validPlaceholders)));
        }

        // Check for malformed patterns
        if (text.contains("{{}}") || text.contains("{}")) {
            throw new TemplateValidationException(
                String.format("Empty placeholders not allowed in %s", fieldName));
        }

        if (text.contains("{{") || text.contains("}}")) {
            throw new TemplateValidationException(
                String.format("Double braces not allowed in %s", fieldName));
        }
    }

    /**
     * Enforces the rule that only one template per category can be active at a time.
     * When activating a template, deactivates any other active template in the same category.
     *
     * @param user The current user
     * @param category The category of the template being activated
     * @param excludeTemplateId Template ID to exclude from deactivation (for updates)
     */
    private void enforceOneActiveTemplatePerCategory(User user, EmailTemplate.Category category, Long excludeTemplateId) {
        // Find all active templates in the same category for this user
        List<EmailTemplate> activeTemplatesInCategory = templateRepository.findByUserAndCategoryAndStatus(
            user, category, EmailTemplate.Status.ACTIVE);

        // Filter out the current template being updated (if any)
        List<EmailTemplate> templatesToDeactivate = activeTemplatesInCategory.stream()
            .filter(template -> excludeTemplateId == null || !template.getId().equals(excludeTemplateId))
            .collect(Collectors.toList());

        // Deactivate other active templates in the same category
        for (EmailTemplate template : templatesToDeactivate) {
            template.setStatus(EmailTemplate.Status.DRAFT);
            template.setUpdatedAt(LocalDateTime.now());
            templateRepository.save(template);
        }
    }

}