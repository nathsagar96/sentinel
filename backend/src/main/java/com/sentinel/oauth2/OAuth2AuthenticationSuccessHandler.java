package com.sentinel.oauth2;

import com.sentinel.auth.jwt.CookieUtils;
import com.sentinel.auth.jwt.JwtTokenProvider;
import com.sentinel.config.AppProperties;
import com.sentinel.user.entity.User;
import com.sentinel.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtils cookieUtils;
    private final AppProperties appProperties;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Long userId = (Long) oAuth2User.getAttributes().get("internal_user_id");

        User user = userRepository.findById(userId).orElseThrow();

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getName());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        response.addHeader("Set-Cookie", cookieUtils.createAccessTokenCookie(accessToken).toString());
        response.addHeader("Set-Cookie", cookieUtils.createRefreshTokenCookie(refreshToken).toString());

        String redirectUrl = appProperties.oauth2().redirectUri();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
