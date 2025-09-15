package com.ecold.service;

import com.ecold.dto.EmailTemplateDto;
import com.ecold.entity.EmailTemplate;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface EmailTemplateService {
    List<EmailTemplateDto> getAllTemplates();
    List<EmailTemplateDto> getTemplatesByCategory(EmailTemplate.Category category);
    List<EmailTemplateDto> getTemplatesByStatus(EmailTemplate.Status status);
    EmailTemplateDto getTemplateById(Long id);
    EmailTemplateDto createTemplate(EmailTemplateDto templateDto);
    EmailTemplateDto updateTemplate(Long id, EmailTemplateDto templateDto);
    void deleteTemplate(Long id);
    EmailTemplateDto duplicateTemplate(Long id);
    void archiveTemplate(Long id);
    void activateTemplate(Long id);
    Map<String, Object> getTemplateStats();
    void incrementUsage(Long id);
    List<EmailTemplateDto> searchTemplates(String query, EmailTemplate.Category category, EmailTemplate.Status status);
    void clearAllTemplates();
}