package com.sentinel.oauth2;

import com.sentinel.user.entity.AuthProvider;
import java.util.Map;

public record OAuth2UserInfo(String name, String email, String avatarUrl, String providerId, AuthProvider provider) {
    public static String asString(Map<String, Object> attrs, String key) {
        Object val = attrs.get(key);
        return val instanceof String s ? s : val != null ? String.valueOf(val) : null;
    }
}
