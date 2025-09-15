package com.ecold.repository;

import com.ecold.entity.EmailLog;
import com.ecold.entity.RecruiterContact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByRecruiterContact(RecruiterContact recruiterContact);
    List<EmailLog> findByStatus(EmailLog.EmailStatus status);
    Optional<EmailLog> findByMessageId(String messageId);
    
    @Query("SELECT el FROM EmailLog el WHERE el.status = :status AND el.retryCount < :maxRetries")
    List<EmailLog> findRetryableEmails(@Param("status") EmailLog.EmailStatus status, @Param("maxRetries") Integer maxRetries);
    
    @Query("SELECT COUNT(el) FROM EmailLog el WHERE el.recruiterContact.user.id = :userId AND el.sentAt BETWEEN :startDate AND :endDate")
    Long countEmailsSentInPeriod(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}