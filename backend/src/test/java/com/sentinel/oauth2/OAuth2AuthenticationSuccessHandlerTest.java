package com.sentinel.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentinel.auth.jwt.CookieUtils;
import com.sentinel.auth.service.AuthService;
import com.sentinel.auth.service.AuthService.AuthTokens;
import com.sentinel.config.AppProperties;
import com.sentinel.user.entity.User;
import com.sentinel.user.service.UserService;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private CookieUtils cookieUtils;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler successHandler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldSetCookiesOnSuccessfulAuthentication() throws IOException {
        Authentication authentication = mock(Authentication.class);
        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("internal_user_id", 1L));

        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
        when(userService.findById(1L)).thenReturn(user);

        AuthTokens tokens = new AuthTokens("access-token-123", "refresh-token-456");
        when(authService.issueTokens(user.getId(), user.getEmail(), user.getName()))
                .thenReturn(tokens);

        ResponseCookie accessCookie =
                ResponseCookie.from("access_token", "access-token-123").build();
        ResponseCookie refreshCookie =
                ResponseCookie.from("refresh_token", "refresh-token-456").build();
        when(cookieUtils.createAccessTokenCookie("access-token-123")).thenReturn(accessCookie);
        when(cookieUtils.createRefreshTokenCookie("refresh-token-456")).thenReturn(refreshCookie);

        AppProperties.OAuth2Properties oauth2Props =
                new AppProperties.OAuth2Properties("http://localhost:3000/oauth2/redirect");
        when(appProperties.oauth2()).thenReturn(oauth2Props);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getHeaders("Set-Cookie"))
                .containsExactlyInAnyOrder(accessCookie.toString(), refreshCookie.toString());
    }

    @Test
    void shouldRedirectToConfiguredUri() throws IOException {
        Authentication authentication = mock(Authentication.class);
        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("internal_user_id", 1L));

        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
        when(userService.findById(1L)).thenReturn(user);

        AuthTokens tokens = new AuthTokens("access", "refresh");
        when(authService.issueTokens(anyLong(), anyString(), anyString())).thenReturn(tokens);
        when(cookieUtils.createAccessTokenCookie(anyString()))
                .thenReturn(ResponseCookie.from("access", "token").build());
        when(cookieUtils.createRefreshTokenCookie(anyString()))
                .thenReturn(ResponseCookie.from("refresh", "token").build());

        AppProperties.OAuth2Properties oauth2Props =
                new AppProperties.OAuth2Properties("http://localhost:3000/oauth2/redirect");
        when(appProperties.oauth2()).thenReturn(oauth2Props);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/oauth2/redirect");
    }

    @Test
    void shouldExtractUserIdFromOAuth2Attributes() throws IOException {
        Authentication authentication = mock(Authentication.class);
        OAuth2User oAuth2User = mock(OAuth2User.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("internal_user_id", 99L));

        User user = User.builder()
                .id(99L)
                .email("test@example.com")
                .name("Test User")
                .build();
        when(userService.findById(99L)).thenReturn(user);

        AuthTokens tokens = new AuthTokens("access", "refresh");
        when(authService.issueTokens(anyLong(), anyString(), anyString())).thenReturn(tokens);
        when(cookieUtils.createAccessTokenCookie(anyString()))
                .thenReturn(ResponseCookie.from("access", "token").build());
        when(cookieUtils.createRefreshTokenCookie(anyString()))
                .thenReturn(ResponseCookie.from("refresh", "token").build());

        AppProperties.OAuth2Properties oauth2Props =
                new AppProperties.OAuth2Properties("http://localhost:3000/oauth2/redirect");
        when(appProperties.oauth2()).thenReturn(oauth2Props);

        successHandler.onAuthenticationSuccess(request, response, authentication);

        verify(userService).findById(99L);
        verify(authService).issueTokens(99L, "test@example.com", "Test User");
    }
}
