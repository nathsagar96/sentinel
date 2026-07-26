package com.sentinel.oauth2;

import com.sentinel.auth.jwt.CookieUtils;
import com.sentinel.auth.service.AuthService;
import com.sentinel.config.AppProperties;
import com.sentinel.user.entity.User;
import com.sentinel.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final UserService userService;
    private final CookieUtils cookieUtils;
    private final AppProperties appProperties;

    public OAuth2AuthenticationSuccessHandler(
            @Lazy AuthService authService,
            UserService userService,
            CookieUtils cookieUtils,
            AppProperties appProperties) {
        this.authService = authService;
        this.userService = userService;
        this.cookieUtils = cookieUtils;
        this.appProperties = appProperties;
    }

    @Override
    public void onAuthenticationSuccess(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Object userIdObj = oAuth2User.getAttributes().get(CookieUtils.INTERNAL_USER_ID_ATTR);
        if (!(userIdObj instanceof Long userId)) {
            log.error("Internal user ID not found in OAuth2 attributes");
            throw new IllegalStateException("Internal user ID missing from OAuth2 authentication attributes");
        }

        User user = userService.findById(userId);
        AuthService.AuthTokens tokens = authService.issueTokens(user.getId(), user.getEmail(), user.getName());

        response.addHeader(
                "Set-Cookie",
                cookieUtils.createAccessTokenCookie(tokens.accessToken()).toString());
        response.addHeader(
                "Set-Cookie",
                cookieUtils.createRefreshTokenCookie(tokens.refreshToken()).toString());

        String redirectUrl = appProperties.oauth2().redirectUri();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
