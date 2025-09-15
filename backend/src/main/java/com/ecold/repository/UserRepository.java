package com.ecold.repository;

import com.ecold.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(User.Provider provider, String providerId);
    Optional<User> findByRefreshToken(String refreshToken);
    boolean existsByEmail(String email);
}