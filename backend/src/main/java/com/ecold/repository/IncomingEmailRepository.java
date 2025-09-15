package com.ecold.repository;

import com.ecold.entity.IncomingEmail;
import com.ecold.entity.User;
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
public interface IncomingEmailRepository extends JpaRepository<IncomingEmail, Long> {
    Page<IncomingEmail> findByUser(User user, Pageable pageable);
    Page<IncomingEmail> findByUserAndCategory(User user, IncomingEmail.EmailCategory category, Pageable pageable);
    List<IncomingEmail> findByUserAndIsReadFalse(User user);
    List<IncomingEmail> findByUserAndIsProcessedFalse(User user);
    Optional<IncomingEmail> findByUserAndMessageId(User user, String messageId);
    boolean existsByUserAndMessageId(User user, String messageId);
    
    @Query("SELECT COUNT(ie) FROM IncomingEmail ie WHERE ie.user = :user AND ie.category = :category AND ie.isRead = false")
    Long countUnreadByUserAndCategory(@Param("user") User user, @Param("category") IncomingEmail.EmailCategory category);
    
    @Query("SELECT ie FROM IncomingEmail ie WHERE ie.user = :user AND ie.receivedAt >= :since ORDER BY ie.receivedAt DESC")
    List<IncomingEmail> findRecentEmails(@Param("user") User user, @Param("since") LocalDateTime since);
    
    @Query("SELECT ie FROM IncomingEmail ie WHERE ie.user = :user AND ie.priority = :priority AND ie.isRead = false")
    List<IncomingEmail> findUnreadByPriority(@Param("user") User user, @Param("priority") IncomingEmail.EmailPriority priority);
}