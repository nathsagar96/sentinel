package com.sentinel.oauth2;

import com.sentinel.user.entity.AuthProvider;
import com.sentinel.user.entity.User;
import com.sentinel.user.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Override
    public OAuth2User loadUser(@NonNull OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfo.of(registrationId, oAuth2User.getAttributes());

        String email = userInfo.email();
        if (email == null && AuthProvider.GITHUB == userInfo.provider()) {
            email = fetchPrimaryEmailFromGitHub(userRequest.getAccessToken().getTokenValue());
        }
        if (email == null) {
            email = userInfo.provider().name().toLowerCase() + "-" + userInfo.providerId()
                    + "@placeholder.sentinel.local";
            log.warn("Email not available from {} OAuth2; generated placeholder: {}", userInfo.provider(), email);
        }

        String resolvedEmail = email;
        User user = userRepository
                .findByEmail(resolvedEmail)
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> createNewUser(userInfo, resolvedEmail));

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("internal_user_id", user.getId());

        return new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                attributes,
                userRequest
                        .getClientRegistration()
                        .getProviderDetails()
                        .getUserInfoEndpoint()
                        .getUserNameAttributeName());
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

    private String fetchPrimaryEmailFromGitHub(String accessToken) {
        try {
            List<Map<String, Object>> emails = restTemplate
                    .exchange(
                            "https://api.github.com/user/emails",
                            HttpMethod.GET,
                            new HttpEntity<>(createHeaders(accessToken)),
                            new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .getBody();

            if (emails != null) {
                for (Map<String, Object> entry : emails) {
                    Boolean primary = (Boolean) entry.get("primary");
                    Boolean verified = (Boolean) entry.get("verified");
                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                        return (String) entry.get("email");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch primary email from GitHub: {}", e.getMessage());
        }
        return null;
    }

    private HttpHeaders createHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.set("Accept", "application/vnd.github+json");
        return headers;
    }
}
