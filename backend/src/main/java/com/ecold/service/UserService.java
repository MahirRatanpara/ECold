package com.ecold.service;

import com.ecold.entity.User;
import com.ecold.dto.UserDto;

public interface UserService {
    User getCurrentUser();
    UserDto getCurrentUserDto();
    User getUserById(String id);
    User getUserByEmail(String email);
}