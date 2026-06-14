package com.interview.platform.dto;

import com.interview.platform.enums.Role;
import com.interview.platform.models.User;

import java.util.UUID;

public record UserDTO(
    UUID id,
    String email,
    String fullName,
    String avatarUrl,
    Role role
) {
    public static UserDTO from(User user) {
        return new UserDTO(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getAvatarUrl(),
            user.getRole()
        );
    }
}
