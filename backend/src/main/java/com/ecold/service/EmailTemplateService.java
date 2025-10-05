package com.ecold.service;

import com.ecold.dto.EmailTemplateDto;
import com.ecold.entity.EmailTemplate;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface EmailTemplateService {
    List<EmailTemplateDto> getAllTemplates();
    List<EmailTemplateDto> getTemplatesByCategory(EmailTemplate.Category category);
    List<EmailTemplateDto> getTemplatesByStatus(EmailTemplate.Status status);
    EmailTemplateDto getTemplateById(String id);
    EmailTemplateDto createTemplate(EmailTemplateDto templateDto);
    EmailTemplateDto updateTemplate(String id, EmailTemplateDto templateDto);
    void deleteTemplate(String id);
    EmailTemplateDto duplicateTemplate(String id);
    void archiveTemplate(String id);
    void activateTemplate(String id);
    Map<String, Object> getTemplateStats();
    void incrementUsage(String id);
    List<EmailTemplateDto> searchTemplates(String query, EmailTemplate.Category category, EmailTemplate.Status status);
    void clearAllTemplates();
}