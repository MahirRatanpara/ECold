package com.ecold.repository;

import com.ecold.entity.Resume;
import com.ecold.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUser(User user);
    Optional<Resume> findByUserAndIsDefaultTrue(User user);
    Optional<Resume> findByUserAndName(User user, String name);
}