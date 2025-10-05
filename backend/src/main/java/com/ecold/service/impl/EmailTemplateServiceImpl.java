package com.ecold.service.impl;

import com.ecold.dto.EmailTemplateDto;
import com.ecold.entity.EmailTemplate;
import com.ecold.entity.User;
import com.ecold.exception.TemplateValidationException;
import com.ecold.repository.firestore.EmailTemplateFirestoreRepository;
import com.ecold.repository.firestore.UserFirestoreRepository;
import com.ecold.service.EmailTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private final EmailTemplateFirestoreRepository templateFirestoreRepository;
    private final UserFirestoreRepository userFirestoreRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<EmailTemplateDto> getAllTemplates() {
        try {
            User currentUser = getCurrentUser();
            List<EmailTemplate> templates = templateFirestoreRepository.findByUser(currentUser.getId());
            return templates.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching templates", e);
        }
    }

    @Override
    public List<EmailTemplateDto> getTemplatesByCategory(EmailTemplate.Category category) {
        try {
            User currentUser = getCurrentUser();
            List<EmailTemplate> templates = templateFirestoreRepository.findByUserAndCategory(currentUser.getId(), category.name());
            return templates.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching templates by category", e);
        }
    }

    @Override
    public List<EmailTemplateDto> getTemplatesByStatus(EmailTemplate.Status status) {
        try {
            User currentUser = getCurrentUser();
            List<EmailTemplate> templates = templateFirestoreRepository.findByUserAndStatus(currentUser.getId(), status.name());
            return templates.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching templates by status", e);
        }
    }

    @Override
    public EmailTemplateDto getTemplateById(String id) {
        try {
            User currentUser = getCurrentUser();
            EmailTemplate template = templateFirestoreRepository.findById(currentUser.getId(), id)
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

            return convertToDto(template);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching template by id", e);
        }
    }

    @Override
    public EmailTemplateDto createTemplate(EmailTemplateDto templateDto) {
        try {
            if (templateDto == null) {
                throw new TemplateValidationException("Template data cannot be null");
            }

            // Additional placeholder validation at service level
            validatePlaceholders(templateDto);

            User currentUser = getCurrentUser();

            // Check if template with same name already exists for this user
            if (templateDto.getName() != null && !templateDto.getName().trim().isEmpty()) {
                Optional<EmailTemplate> existingTemplate = templateFirestoreRepository.findByUserAndName(currentUser.getId(), templateDto.getName().trim());
                if (existingTemplate.isPresent()) {
                    throw new TemplateValidationException("Template with name '" + templateDto.getName().trim() + "' already exists");
                }
            }

            EmailTemplate template = convertToEntity(templateDto);
            template.setUserId(currentUser.getId());
            template.setCreatedAt(Timestamp.now());
            template.setUpdatedAt(Timestamp.now());

            // Enforce one active template per category rule
            if (template.getStatusEnum() == EmailTemplate.Status.ACTIVE) {
                enforceOneActiveTemplatePerCategory(currentUser, template.getCategoryEnum(), null);
            }

            EmailTemplate saved = templateFirestoreRepository.save(currentUser.getId(), template);
            return convertToDto(saved);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error creating template", e);
        }
    }

    @Override
    public EmailTemplateDto updateTemplate(String id, EmailTemplateDto templateDto) {
        try {
            // Validate placeholders before updating
            validatePlaceholders(templateDto);

            User currentUser = getCurrentUser();
            EmailTemplate existingTemplate = templateFirestoreRepository.findById(currentUser.getId(), id)
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

            // Check if name is being changed and if new name already exists
            if (!existingTemplate.getName().equals(templateDto.getName())) {
                Optional<EmailTemplate> nameConflict = templateFirestoreRepository.findByUserAndName(currentUser.getId(), templateDto.getName());
                if (nameConflict.isPresent() && !nameConflict.get().getId().equals(id)) {
                    throw new RuntimeException("Template with name '" + templateDto.getName() + "' already exists");
                }
            }

            existingTemplate.setName(templateDto.getName());
            existingTemplate.setSubject(templateDto.getSubject());
            existingTemplate.setBody(templateDto.getBody());
            existingTemplate.setCategoryEnum(templateDto.getCategory());
            existingTemplate.setStatusEnum(templateDto.getStatus());
            existingTemplate.setTags(convertTagsToString(templateDto.getTags()));
            existingTemplate.setUpdatedAt(Timestamp.now());

            // Enforce one active template per category rule
            if (templateDto.getStatus() == EmailTemplate.Status.ACTIVE) {
                enforceOneActiveTemplatePerCategory(currentUser, templateDto.getCategory(), id);
            }

            EmailTemplate updated = templateFirestoreRepository.save(currentUser.getId(), existingTemplate);
            return convertToDto(updated);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error updating template", e);
        }
    }

    @Override
    public void deleteTemplate(String id) {
        try {
            User currentUser = getCurrentUser();
            EmailTemplate template = templateFirestoreRepository.findById(currentUser.getId(), id)
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

            templateFirestoreRepository.delete(currentUser.getId(), id);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error deleting template", e);
        }
    }

    @Override
    public EmailTemplateDto duplicateTemplate(String id) {
        try {
            User currentUser = getCurrentUser();
            EmailTemplate originalTemplate = templateFirestoreRepository.findById(currentUser.getId(), id)
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

            if (!originalTemplate.getUserId().equals(currentUser.getId())) {
                throw new RuntimeException("Access denied: Template belongs to another user");
            }

            EmailTemplate duplicatedTemplate = new EmailTemplate();
            duplicatedTemplate.setUserId(currentUser.getId());
            duplicatedTemplate.setName(originalTemplate.getName() + " (Copy)");
            duplicatedTemplate.setSubject(originalTemplate.getSubject());
            duplicatedTemplate.setBody(originalTemplate.getBody());
            duplicatedTemplate.setCategoryEnum(originalTemplate.getCategoryEnum());
            duplicatedTemplate.setStatusEnum(EmailTemplate.Status.DRAFT);
            duplicatedTemplate.setUsageCount(0L);
            duplicatedTemplate.setEmailsSent(0L);
            duplicatedTemplate.setResponseRate(0.0);
            duplicatedTemplate.setTags(originalTemplate.getTags());
            duplicatedTemplate.setCreatedAt(Timestamp.now());
            duplicatedTemplate.setUpdatedAt(Timestamp.now());

            EmailTemplate saved = templateFirestoreRepository.save(currentUser.getId(), duplicatedTemplate);
            return convertToDto(saved);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error duplicating template", e);
        }
    }

    @Override
    public void archiveTemplate(String id) {
        try {
            User currentUser = getCurrentUser();
            EmailTemplate template = templateFirestoreRepository.findById(currentUser.getId(), id)
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

            template.setStatusEnum(EmailTemplate.Status.ARCHIVED);
            template.setUpdatedAt(Timestamp.now());
            templateFirestoreRepository.save(currentUser.getId(), template);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error archiving template", e);
        }
    }

    @Override
    public void activateTemplate(String id) {
        try {
            User currentUser = getCurrentUser();
            EmailTemplate template = templateFirestoreRepository.findById(currentUser.getId(), id)
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

            // Enforce one active template per category rule
            enforceOneActiveTemplatePerCategory(currentUser, template.getCategoryEnum(), id);

            template.setStatusEnum(EmailTemplate.Status.ACTIVE);
            template.setUpdatedAt(Timestamp.now());
            templateFirestoreRepository.save(currentUser.getId(), template);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error activating template", e);
        }
    }

    @Override
    public Map<String, Object> getTemplateStats() {
        try {
            User currentUser = getCurrentUser();

            long totalTemplates = templateFirestoreRepository.countByUser(currentUser.getId());
            long activeTemplates = templateFirestoreRepository.countByUserAndStatus(currentUser.getId(), EmailTemplate.Status.ACTIVE.name());
            long draftTemplates = templateFirestoreRepository.countByUserAndStatus(currentUser.getId(), EmailTemplate.Status.DRAFT.name());
            long archivedTemplates = templateFirestoreRepository.countByUserAndStatus(currentUser.getId(), EmailTemplate.Status.ARCHIVED.name());

            List<EmailTemplate> templates = templateFirestoreRepository.findByUser(currentUser.getId());
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
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching template stats", e);
        }
    }

    @Override
    public void incrementUsage(String id) {
        try {
            User currentUser = getCurrentUser();
            EmailTemplate template = templateFirestoreRepository.findById(currentUser.getId(), id)
                    .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

            template.setUsageCount(template.getUsageCount() + 1);
            template.setLastUsed(Timestamp.now());
            template.setUpdatedAt(Timestamp.now());
            templateFirestoreRepository.save(currentUser.getId(), template);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error incrementing usage", e);
        }
    }

    @Override
    public List<EmailTemplateDto> searchTemplates(String query, EmailTemplate.Category category, EmailTemplate.Status status) {
        try {
            User currentUser = getCurrentUser();
            List<EmailTemplate> templates;

            if (category != null && status != null) {
                templates = templateFirestoreRepository.findByUserAndCategoryAndStatus(currentUser.getId(), category.name(), status.name());
            } else if (category != null) {
                templates = templateFirestoreRepository.findByUserAndCategory(currentUser.getId(), category.name());
            } else if (status != null) {
                templates = templateFirestoreRepository.findByUserAndStatus(currentUser.getId(), status.name());
            } else {
                templates = templateFirestoreRepository.findByUser(currentUser.getId());
            }

            if (query != null && !query.trim().isEmpty()) {
                String lowerQuery = query.toLowerCase().trim();
                templates = templates.stream()
                        .filter(template ->
                            template.getName().toLowerCase().contains(lowerQuery) ||
                            template.getSubject().toLowerCase().contains(lowerQuery) ||
                            template.getBody().toLowerCase().contains(lowerQuery) ||
                            template.getCategory().toLowerCase().contains(lowerQuery) ||
                            (template.getTags() != null && template.getTags().toLowerCase().contains(lowerQuery))
                        )
                        .collect(Collectors.toList());
            }

            return templates.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error searching templates", e);
        }
    }

    @Override
    public void clearAllTemplates() {
        try {
            User currentUser = getCurrentUser();
            List<EmailTemplate> userTemplates = templateFirestoreRepository.findByUser(currentUser.getId());
            for (EmailTemplate template : userTemplates) {
                templateFirestoreRepository.delete(currentUser.getId(), template.getId());
            }
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error clearing templates", e);
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

            // If not authenticated, throw an exception instead of returning a random user
            throw new RuntimeException("User not authenticated. Please log in.");
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching current user", e);
        }
    }

    private EmailTemplateDto convertToDto(EmailTemplate template) {
        EmailTemplateDto dto = new EmailTemplateDto();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setSubject(template.getSubject());
        dto.setBody(template.getBody());
        dto.setCategory(template.getCategoryEnum());
        dto.setStatus(template.getStatusEnum());
        dto.setUsageCount(template.getUsageCount());
        dto.setEmailsSent(template.getEmailsSent());
        dto.setResponseRate(template.getResponseRate());
        dto.setTags(convertTagsFromString(template.getTags()));
        dto.setIsDefault(template.getIsDefault());
        dto.setLastUsed(convertToLocalDateTime(template.getLastUsed()));
        dto.setCreatedAt(convertToLocalDateTime(template.getCreatedAt()));
        dto.setUpdatedAt(convertToLocalDateTime(template.getUpdatedAt()));
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
        template.setCategoryEnum(dto.getCategory() != null ? dto.getCategory() : EmailTemplate.Category.OUTREACH);
        template.setStatusEnum(dto.getStatus() != null ? dto.getStatus() : EmailTemplate.Status.DRAFT);
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
     * Helper method to convert Firestore Timestamp to LocalDateTime
     */
    private java.time.LocalDateTime convertToLocalDateTime(com.google.cloud.Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                java.time.ZoneId.systemDefault()
        );
    }

    /**
     * Enforces the rule that only one template per category can be active at a time.
     * When activating a template, deactivates any other active template in the same category.
     *
     * @param user The current user
     * @param category The category of the template being activated
     * @param excludeTemplateId Template ID to exclude from deactivation (for updates)
     */
    private void enforceOneActiveTemplatePerCategory(User user, EmailTemplate.Category category, String excludeTemplateId) {
        try {
            // Find all active templates in the same category for this user
            List<EmailTemplate> activeTemplatesInCategory = templateFirestoreRepository.findByUserAndCategoryAndStatus(
                user.getId(), category.name(), EmailTemplate.Status.ACTIVE.name());

            // Filter out the current template being updated (if any)
            List<EmailTemplate> templatesToDeactivate = activeTemplatesInCategory.stream()
                .filter(template -> excludeTemplateId == null || !template.getId().equals(excludeTemplateId))
                .collect(Collectors.toList());

            // Deactivate other active templates in the same category
            for (EmailTemplate template : templatesToDeactivate) {
                template.setStatusEnum(EmailTemplate.Status.DRAFT);
                template.setUpdatedAt(Timestamp.now());
                templateFirestoreRepository.save(user.getId(), template);
            }
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error enforcing one active template per category", e);
        }
    }
}
