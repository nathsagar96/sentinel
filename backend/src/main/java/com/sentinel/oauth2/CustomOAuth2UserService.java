package com.sentinel.oauth2;

import com.sentinel.user.entity.User;
import com.sentinel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfo.of(registrationId, oAuth2User.getAttributes());

        User user = userRepository.findByEmail(userInfo.email())
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> createNewUser(userInfo));

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("internal_user_id", user.getId());

        return new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                attributes,
                userRequest.getClientRegistration().getProviderDetails()
                        .getUserInfoEndpoint().getUserNameAttributeName()
        );
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        user.setName(userInfo.name());
        user.setAvatarUrl(userInfo.avatarUrl());
        if (user.getProvider() != userInfo.provider()) {
            user.setProvider(userInfo.provider());
            user.setProviderId(userInfo.providerId());
        }
        return userRepository.save(user);
    }

    private User createNewUser(OAuth2UserInfo userInfo) {
        User user = User.builder()
                .email(userInfo.email())
                .name(userInfo.name())
                .avatarUrl(userInfo.avatarUrl())
                .provider(userInfo.provider())
                .providerId(userInfo.providerId())
                .build();
        return userRepository.save(user);
    }
}
