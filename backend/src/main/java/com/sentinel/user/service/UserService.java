package com.sentinel.user.service;

import com.sentinel.exception.ResourceNotFoundException;
import com.sentinel.oauth2.OAuth2UserInfo;
import com.sentinel.user.entity.User;
import com.sentinel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public User findOrCreateUser(OAuth2UserInfo userInfo, String email) {
        return userRepository
                .findByEmail(email)
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> createNewUser(userInfo, email));
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        if (user.getName() == null) {
            user.setName(userInfo.name());
        }
        if (user.getAvatarUrl() == null) {
            user.setAvatarUrl(userInfo.avatarUrl());
        }
        if (user.getProvider() != userInfo.provider()) {
            user.setProvider(userInfo.provider());
            user.setProviderId(userInfo.providerId());
        }
        return userRepository.save(user);
    }

    private User createNewUser(OAuth2UserInfo userInfo, String email) {
        User user = User.builder()
                .email(email)
                .name(userInfo.name())
                .avatarUrl(userInfo.avatarUrl())
                .provider(userInfo.provider())
                .providerId(userInfo.providerId())
                .build();
        return userRepository.save(user);
    }
}
