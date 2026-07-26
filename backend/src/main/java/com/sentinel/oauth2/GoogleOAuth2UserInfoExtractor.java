package com.sentinel.oauth2;

import com.sentinel.user.entity.AuthProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuth2UserInfoExtractor implements OAuth2UserInfoExtractor {
    @Override
    public String getRegistrationId() {
        return "google";
    }

    @Override
    public OAuth2UserInfo extract(Map<String, Object> attributes) {
        return new OAuth2UserInfo(
                OAuth2UserInfo.asString(attributes, "name"),
                OAuth2UserInfo.asString(attributes, "email"),
                OAuth2UserInfo.asString(attributes, "picture"),
                OAuth2UserInfo.asString(attributes, "sub"),
                AuthProvider.GOOGLE);
    }
}
