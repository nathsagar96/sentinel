package com.sentinel.oauth2;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class OAuth2UserInfoExtractorRegistry {
    private final Map<String, OAuth2UserInfoExtractor> extractors;

    public OAuth2UserInfoExtractorRegistry(List<OAuth2UserInfoExtractor> extractorList) {
        this.extractors =
                extractorList.stream().collect(Collectors.toMap(OAuth2UserInfoExtractor::getRegistrationId, e -> e));
    }

    public OAuth2UserInfoExtractor getExtractor(String registrationId) {
        OAuth2UserInfoExtractor extractor = extractors.get(registrationId);
        if (extractor == null) {
            throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        }
        return extractor;
    }
}
