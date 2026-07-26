package com.sentinel.oauth2;

import com.sentinel.auth.dto.UserResponse;
import com.sentinel.auth.jwt.CookieUtils;
import com.sentinel.auth.service.AuthService;
import com.sentinel.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final CookieUtils cookieUtils;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long userId = (Long) oAuth2User.getAttributes().get("internal_user_id");

        UserResponse user = authService.findById(userId);
        AuthService.AuthTokens tokens = authService.issueTokens(user.id(), user.email(), user.name());

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
