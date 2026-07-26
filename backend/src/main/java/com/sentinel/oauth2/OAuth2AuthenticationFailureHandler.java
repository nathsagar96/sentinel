package com.sentinel.oauth2;

import com.sentinel.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final AppProperties appProperties;

    @Override
    public void onAuthenticationFailure(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            AuthenticationException exception)
            throws IOException {
        log.error("OAuth2 authentication failed: {}", exception.getMessage());

        UriComponents redirectUri = UriComponentsBuilder.fromUriString(
                        appProperties.oauth2().redirectUri())
                .build();

        String loginUrl = UriComponentsBuilder.newInstance()
                .scheme(redirectUri.getScheme())
                .host(redirectUri.getHost())
                .port(redirectUri.getPort())
                .path("/login")
                .queryParam("error", "oauth2_failure")
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, loginUrl);
    }
}
