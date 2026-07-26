package com.sentinel.oauth2;

import com.sentinel.user.entity.AuthProvider;
import java.util.Map;

public record OAuth2UserInfo(String name, String email, String avatarUrl, String providerId, AuthProvider provider) {
    public static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" ->
                new OAuth2UserInfo(
                        asString(attributes, "name"),
                        asString(attributes, "email"),
                        asString(attributes, "picture"),
                        asString(attributes, "sub"),
                        AuthProvider.GOOGLE);
            case "github" ->
                new OAuth2UserInfo(
                        asString(attributes, "name") != null
                                ? asString(attributes, "name")
                                : asString(attributes, "login"),
                        asString(attributes, "email"),
                        asString(attributes, "avatar_url"),
                        asString(attributes, "id"),
                        AuthProvider.GITHUB);
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };
    }

    private static String asString(Map<String, Object> attrs, String key) {
        Object val = attrs.get(key);
        return val instanceof String s ? s : val != null ? String.valueOf(val) : null;
    }
}
