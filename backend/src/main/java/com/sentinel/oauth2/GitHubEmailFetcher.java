package com.sentinel.oauth2;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubEmailFetcher {
    private final RestClient restClient;

    public String fetchPrimaryEmail(String accessToken) {
        try {
            List<Map<String, Object>> emails = restClient
                    .get()
                    .uri("https://api.github.com/user/emails")
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken);
                        headers.set("Accept", "application/vnd.github+json");
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (emails != null) {
                for (Map<String, Object> entry : emails) {
                    Object primaryObj = entry.get("primary");
                    Object verifiedObj = entry.get("verified");
                    if (primaryObj instanceof Boolean primary
                            && primary
                            && verifiedObj instanceof Boolean verified
                            && verified) {
                        Object emailObj = entry.get("email");
                        if (emailObj instanceof String email) {
                            return email;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch primary email from GitHub: {}", e.getMessage());
        }
        return null;
    }
}
