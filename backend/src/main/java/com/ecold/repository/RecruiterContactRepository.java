package com.ecold.repository;

import com.ecold.entity.RecruiterContact;
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
public interface RecruiterContactRepository extends JpaRepository<RecruiterContact, Long> {
    Page<RecruiterContact> findByUser(User user, Pageable pageable);
    List<RecruiterContact> findByUserAndStatus(User user, RecruiterContact.ContactStatus status);
    Page<RecruiterContact> findByUserAndStatus(User user, RecruiterContact.ContactStatus status, Pageable pageable);
    Optional<RecruiterContact> findByUserAndEmail(User user, String email);
    boolean existsByUserAndEmail(User user, String email);
    
    // New methods for CSV import functionality - keep for backward compatibility but prefer user-specific ones
    Page<RecruiterContact> findByStatus(RecruiterContact.ContactStatus status, Pageable pageable);
    List<RecruiterContact> findByStatus(RecruiterContact.ContactStatus status);
    Optional<RecruiterContact> findByEmailIgnoreCase(String email);
    Long countByStatus(RecruiterContact.ContactStatus status);
    Long countByUserAndStatus(User user, RecruiterContact.ContactStatus status);
    
    // Method to find orphaned recruiters (those without a user assigned)
    List<RecruiterContact> findByUserIsNull();
    
    @Query("SELECT rc FROM RecruiterContact rc WHERE rc.user = :user AND rc.lastContactedAt IS NULL")
    List<RecruiterContact> findUncontactedByUser(@Param("user") User user);
    
    @Query("SELECT rc FROM RecruiterContact rc WHERE rc.user = :user AND rc.lastContactedAt < :date")
    List<RecruiterContact> findContactedBeforeDate(@Param("user") User user, @Param("date") LocalDateTime date);
    
    // Search methods
    @Query("SELECT rc FROM RecruiterContact rc WHERE rc.user = :user AND " +
           "(LOWER(rc.recruiterName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rc.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rc.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rc.jobRole) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<RecruiterContact> findByUserAndSearchTerm(@Param("user") User user, @Param("search") String search, Pageable pageable);
    
    @Query("SELECT rc FROM RecruiterContact rc WHERE rc.user = :user AND " +
           "rc.status = :status AND " +
           "(LOWER(rc.recruiterName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rc.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rc.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rc.jobRole) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<RecruiterContact> findByUserAndStatusAndSearchTerm(@Param("user") User user, @Param("status") RecruiterContact.ContactStatus status, 
                                                            @Param("search") String search, Pageable pageable);
    
    // Company filtering methods
    Page<RecruiterContact> findByUserAndCompanyName(User user, String companyName, Pageable pageable);
    
    @Query("SELECT rc FROM RecruiterContact rc WHERE rc.user = :user AND " +
           "rc.companyName = :company AND " +
           "(LOWER(rc.recruiterName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rc.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rc.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rc.jobRole) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<RecruiterContact> findByUserAndCompanyAndSearchTerm(@Param("user") User user, @Param("company") String company, 
                                                            @Param("search") String search, Pageable pageable);
    
    @Query("SELECT rc FROM RecruiterContact rc WHERE rc.user = :user AND " +
           "rc.status = :status AND rc.companyName = :company")
    Page<RecruiterContact> findByUserAndStatusAndCompany(@Param("user") User user, @Param("status") RecruiterContact.ContactStatus status,
                                                         @Param("company") String company, Pageable pageable);
    
    @Query("SELECT rc FROM RecruiterContact rc WHERE rc.user = :user AND " +
           "rc.status = :status AND rc.companyName = :company AND " +
           "(LOWER(rc.recruiterName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rc.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rc.companyName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(rc.jobRole) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<RecruiterContact> findByUserAndStatusAndCompanyAndSearchTerm(@Param("user") User user, @Param("status") RecruiterContact.ContactStatus status,
                                                                      @Param("company") String company,
                                                                      @Param("search") String search, Pageable pageable);
    
}