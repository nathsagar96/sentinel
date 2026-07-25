package com.sentinel.oauth2;

import com.sentinel.user.entity.AuthProvider;
import java.util.Map;

public record OAuth2UserInfo(String name, String email, String avatarUrl, String providerId, AuthProvider provider) {
    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" ->
                new OAuth2UserInfo(
                        (String) attributes.get("name"),
                        (String) attributes.get("email"),
                        (String) attributes.get("picture"),
                        (String) attributes.get("sub"),
                        AuthProvider.GOOGLE);
            case "github" ->
                new OAuth2UserInfo(
                        attributes.get("name") != null
                                ? (String) attributes.get("name")
                                : (String) attributes.get("login"),
                        (String) attributes.get("email"),
                        (String) attributes.get("avatar_url"),
                        String.valueOf(attributes.get("id")),
                        AuthProvider.GITHUB);
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };
    }
}
