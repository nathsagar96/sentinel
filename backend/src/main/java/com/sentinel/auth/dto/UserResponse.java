package com.sentinel.auth.dto;

import com.sentinel.user.entity.AuthProvider;
import com.sentinel.user.entity.User;

public record UserResponse(Long id, String email, String name, String avatarUrl, AuthProvider provider) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getAvatarUrl(), user.getProvider());
    }
}
