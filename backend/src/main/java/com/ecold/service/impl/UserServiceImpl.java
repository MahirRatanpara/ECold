package com.ecold.service.impl;

import com.ecold.dto.UserDto;
import com.ecold.entity.User;
import com.ecold.repository.firestore.UserFirestoreRepository;
import com.ecold.service.AuthService;
import com.ecold.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserFirestoreRepository userFirestoreRepository;
    private final AuthService authService;

    @Override
    public User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
                String email = authentication.getName();
                return userFirestoreRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Current user not found: " + email));
            }

            // Fallback for testing - always return the first user consistently
            // Try to find default user first to ensure consistency
            java.util.Optional<User> existingDefault = userFirestoreRepository.findByEmail("default@ecold.com");
            if (existingDefault.isPresent()) {
                return existingDefault.get();
            }

            // If no default user, get the first user by ID to ensure consistency
            return userFirestoreRepository.findAll().stream()
                    .min((u1, u2) -> u1.getId().compareTo(u2.getId()))
                    .orElseGet(() -> {
                        try {
                            // Create new default user if no users exist
                            User defaultUser = new User();
                            defaultUser.setEmail("default@ecold.com");
                            defaultUser.setName("Default User");
                            defaultUser.setPassword("defaultpassword");
                            defaultUser.setProviderEnum(User.Provider.LOCAL);
                            return userFirestoreRepository.save(defaultUser);
                        } catch (ExecutionException | InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Error creating default user", e);
                        }
                    });
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching current user", e);
        }
    }

    @Override
    public UserDto getCurrentUserDto() {
        return authService.getCurrentUser();
    }

    @Override
    public User getUserById(String id) {
        try {
            return userFirestoreRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching user by id", e);
        }
    }

    @Override
    public User getUserByEmail(String email) {
        try {
            return userFirestoreRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching user by email", e);
        }
    }
}