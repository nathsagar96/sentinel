package com.sentinel.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.sentinel.config.AppProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties(
                new AppProperties.JwtProperties(
                        "defaultDevSecretKeyThatIsAtLeast32CharactersLong", Duration.ofMinutes(15), Duration.ofDays(7)),
                new AppProperties.CorsProperties(List.of("http://localhost:5173")),
                new AppProperties.OAuth2Properties("http://localhost:5173/oauth2/redirect"),
                false);
        jwtTokenProvider = new JwtTokenProvider(appProperties);
    }

    @Test
    void testGenerateAccessTokenAndExtractUserId() {
        String token = jwtTokenProvider.generateAccessToken(123L, "test@example.com", "Test User");
        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token, "access")).isTrue();
        assertThat(jwtTokenProvider.validateToken(token, "refresh")).isFalse();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(123L);
    }

    @Test
    void testGenerateRefreshTokenAndExtractUserId() {
        String token = jwtTokenProvider.generateRefreshToken(456L);
        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token, "refresh")).isTrue();
        assertThat(jwtTokenProvider.validateToken(token, "access")).isFalse();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(456L);
    }

    @Test
    void testInvalidToken() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.string", "access"))
                .isFalse();
        assertThat(jwtTokenProvider.validateToken("invalid.token.string", "refresh"))
                .isFalse();
    }
}
