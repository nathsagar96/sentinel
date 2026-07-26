package com.sentinel.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.sentinel.user.entity.AuthProvider;
import com.sentinel.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private OAuth2UserInfoExtractorRegistry extractorRegistry;

    @Mock
    private UserService userService;

    @Mock
    private GitHubEmailFetcher gitHubEmailFetcher;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Mock
    private OAuth2UserRequest userRequest;

    @Mock
    private OAuth2AccessToken accessToken;

    @Test
    void resolveEmail_withProvidedEmail_returnsEmail() {
        OAuth2UserInfo userInfo =
                new OAuth2UserInfo("Test User", "test@example.com", "avatar.jpg", "123", AuthProvider.GOOGLE);

        String result =
                ReflectionTestUtils.invokeMethod(customOAuth2UserService, "resolveEmail", userInfo, userRequest);

        assertThat(result).isEqualTo("test@example.com");
        verifyNoInteractions(gitHubEmailFetcher);
    }

    @Test
    void resolveEmail_withGitHubProviderAndNoEmail_fetchesFromGitHub() {
        OAuth2UserInfo userInfo = new OAuth2UserInfo("Test User", null, "avatar.jpg", "456", AuthProvider.GITHUB);
        when(userRequest.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("mock-token");
        when(gitHubEmailFetcher.fetchPrimaryEmail("mock-token")).thenReturn("github@example.com");

        String result =
                ReflectionTestUtils.invokeMethod(customOAuth2UserService, "resolveEmail", userInfo, userRequest);

        assertThat(result).isEqualTo("github@example.com");
        verify(gitHubEmailFetcher).fetchPrimaryEmail("mock-token");
    }

    @Test
    void resolveEmail_withGitHubProviderAndFetchFails_returnsPlaceholder() {
        OAuth2UserInfo userInfo = new OAuth2UserInfo("Test User", null, "avatar.jpg", "789", AuthProvider.GITHUB);
        when(userRequest.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("mock-token");
        when(gitHubEmailFetcher.fetchPrimaryEmail("mock-token")).thenReturn(null);

        String result =
                ReflectionTestUtils.invokeMethod(customOAuth2UserService, "resolveEmail", userInfo, userRequest);

        assertThat(result).isEqualTo("github-789@placeholder.sentinel.local");
        verify(gitHubEmailFetcher).fetchPrimaryEmail("mock-token");
    }

    @Test
    void resolveEmail_withGoogleProviderAndNoEmail_returnsPlaceholder() {
        OAuth2UserInfo userInfo = new OAuth2UserInfo("Test User", null, "avatar.jpg", "101112", AuthProvider.GOOGLE);

        String result =
                ReflectionTestUtils.invokeMethod(customOAuth2UserService, "resolveEmail", userInfo, userRequest);

        assertThat(result).isEqualTo("google-101112@placeholder.sentinel.local");
        verifyNoInteractions(gitHubEmailFetcher);
    }
}
