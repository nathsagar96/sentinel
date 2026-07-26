package com.sentinel.oauth2;

import com.sentinel.auth.jwt.CookieUtils;
import com.sentinel.user.entity.AuthProvider;
import com.sentinel.user.entity.User;
import com.sentinel.user.service.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2UserInfoExtractorRegistry extractorRegistry;
    private final UserService userService;
    private final GitHubEmailFetcher gitHubEmailFetcher;

    @Override
    public OAuth2User loadUser(@NonNull OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfoExtractor extractor = extractorRegistry.getExtractor(registrationId);
        OAuth2UserInfo userInfo = extractor.extract(oAuth2User.getAttributes());

        String email = resolveEmail(userInfo, userRequest);
        User user = userService.findOrCreateUser(userInfo, email);

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put(CookieUtils.INTERNAL_USER_ID_ATTR, user.getId());

        return new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                attributes,
                userRequest
                        .getClientRegistration()
                        .getProviderDetails()
                        .getUserInfoEndpoint()
                        .getUserNameAttributeName());
    }

    private String resolveEmail(OAuth2UserInfo userInfo, OAuth2UserRequest userRequest) {
        String email = userInfo.email();
        if (email == null && userInfo.provider() == AuthProvider.GITHUB) {
            email = gitHubEmailFetcher.fetchPrimaryEmail(
                    userRequest.getAccessToken().getTokenValue());
        }
        if (email == null) {
            email = userInfo.provider().name().toLowerCase() + "-" + userInfo.providerId()
                    + "@placeholder.sentinel.local";
            log.warn("Email not available from {} OAuth2; generated placeholder email", userInfo.provider());
        }
        return email;
    }
}
