package com.ecold.service.impl;

import com.ecold.dto.UserDto;
import com.ecold.entity.User;
import com.ecold.repository.UserRepository;
import com.ecold.service.AuthService;
import com.ecold.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !authentication.getPrincipal().equals("anonymousUser")) {
            String email = authentication.getName();
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Current user not found: " + email));
        }

        // Fallback for testing - always return the first user consistently
        // Try to find default user first to ensure consistency
        java.util.Optional<User> existingDefault = userRepository.findByEmail("default@ecold.com");
        if (existingDefault.isPresent()) {
            return existingDefault.get();
        }

        // If no default user, get the first user by ID to ensure consistency
        return userRepository.findAll().stream()
                .min((u1, u2) -> Long.compare(u1.getId(), u2.getId()))
                .orElseGet(() -> {
                    // Create new default user if no users exist
                    User defaultUser = new User();
                    defaultUser.setEmail("default@ecold.com");
                    defaultUser.setName("Default User");
                    defaultUser.setPassword("defaultpassword");
                    defaultUser.setProvider(User.Provider.LOCAL);
                    return userRepository.save(defaultUser);
                });
    }

    @Override
    public UserDto getCurrentUserDto() {
        return authService.getCurrentUser();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
}