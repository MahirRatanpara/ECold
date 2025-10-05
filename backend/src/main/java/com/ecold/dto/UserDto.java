package com.ecold.dto;

import com.ecold.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDto {
    private String id;
    private String email;
    private String name;
    private String profilePicture;
    private User.Provider provider;
    private LocalDateTime createdAt;
    private LocalDateTime tokenExpiresAt;
}
