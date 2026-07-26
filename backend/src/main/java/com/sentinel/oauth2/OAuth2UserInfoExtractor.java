package com.sentinel.oauth2;

import java.util.Map;

public interface OAuth2UserInfoExtractor {
    String getRegistrationId();

    OAuth2UserInfo extract(Map<String, Object> attributes);
}
