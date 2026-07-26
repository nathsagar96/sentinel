package com.sentinel.oauth2;

import com.sentinel.user.entity.AuthProvider;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GitHubOAuth2UserInfoExtractor implements OAuth2UserInfoExtractor {
    @Override
    public String getRegistrationId() {
        return "github";
    }

    @Override
    public OAuth2UserInfo extract(Map<String, Object> attributes) {
        String name = OAuth2UserInfo.asString(attributes, "name");
        if (name == null) {
            name = OAuth2UserInfo.asString(attributes, "login");
        }
        return new OAuth2UserInfo(
                name,
                OAuth2UserInfo.asString(attributes, "email"),
                OAuth2UserInfo.asString(attributes, "avatar_url"),
                String.valueOf(attributes.get("id")),
                AuthProvider.GITHUB);
    }
}
