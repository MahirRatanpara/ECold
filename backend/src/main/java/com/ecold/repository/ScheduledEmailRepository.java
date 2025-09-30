package com.ecold.repository;

import com.ecold.entity.ScheduledEmail;
import com.ecold.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledEmailRepository extends JpaRepository<ScheduledEmail, Long> {

    List<ScheduledEmail> findByUserAndStatus(User user, ScheduledEmail.Status status);

    @Query("SELECT se FROM ScheduledEmail se WHERE se.status = :status AND se.scheduleTime <= :currentTime")
    List<ScheduledEmail> findDueScheduledEmails(
        @Param("status") ScheduledEmail.Status status,
        @Param("currentTime") LocalDateTime currentTime
    );

    long countByUserAndStatus(User user, ScheduledEmail.Status status);
}