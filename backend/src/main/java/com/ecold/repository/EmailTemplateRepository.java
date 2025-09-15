package com.ecold.repository;

import com.ecold.entity.EmailTemplate;
import com.ecold.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    List<EmailTemplate> findByUser(User user);
    List<EmailTemplate> findByUserAndCategory(User user, EmailTemplate.Category category);
    List<EmailTemplate> findByUserAndStatus(User user, EmailTemplate.Status status);
    List<EmailTemplate> findByUserAndCategoryAndStatus(User user, EmailTemplate.Category category, EmailTemplate.Status status);
    Optional<EmailTemplate> findByUserAndName(User user, String name);
    long countByUser(User user);
    long countByUserAndStatus(User user, EmailTemplate.Status status);
}