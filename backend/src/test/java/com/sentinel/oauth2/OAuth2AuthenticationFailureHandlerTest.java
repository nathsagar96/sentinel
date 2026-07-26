package com.sentinel.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sentinel.config.AppProperties;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationFailureHandlerTest {

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private OAuth2AuthenticationFailureHandler failureHandler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldRedirectToLoginWithErrorParam() throws IOException {
        AppProperties.OAuth2Properties oauth2Props =
                new AppProperties.OAuth2Properties("http://localhost/oauth2/redirect");
        when(appProperties.oauth2()).thenReturn(oauth2Props);

        AuthenticationException exception = mock(AuthenticationException.class);

        failureHandler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost/login?error=oauth2_failure");
    }

    @Test
    void shouldPreserveSchemeAndHost() throws IOException {
        AppProperties.OAuth2Properties oauth2Props = new AppProperties.OAuth2Properties("https://myapp.com/callback");
        when(appProperties.oauth2()).thenReturn(oauth2Props);

        AuthenticationException exception = mock(AuthenticationException.class);

        failureHandler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl()).isEqualTo("https://myapp.com/login?error=oauth2_failure");
    }

    @Test
    void shouldHandleRedirectUriWithPort() throws IOException {
        AppProperties.OAuth2Properties oauth2Props =
                new AppProperties.OAuth2Properties("http://localhost:8080/callback");
        when(appProperties.oauth2()).thenReturn(oauth2Props);

        AuthenticationException exception = mock(AuthenticationException.class);

        failureHandler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:8080/login?error=oauth2_failure");
    }
}
