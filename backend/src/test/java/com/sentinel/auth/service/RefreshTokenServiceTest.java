package com.sentinel.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentinel.auth.entity.RefreshToken;
import com.sentinel.auth.repository.RefreshTokenRepository;
import com.sentinel.config.AppProperties;
import com.sentinel.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties(
                new AppProperties.JwtProperties(
                        "defaultDevSecretKeyThatIsAtLeast32CharactersLong", Duration.ofMinutes(15), Duration.ofDays(7)),
                new AppProperties.CorsProperties(List.of("http://localhost:5173")),
                new AppProperties.OAuth2Properties("http://localhost:5173/oauth2/redirect"),
                false);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, appProperties);
    }

    private String hash(String raw) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shouldStoreRefreshToken() {
        String rawToken = "some-refresh-token";
        refreshTokenService.storeRefreshToken(1L, rawToken);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getTokenHash()).isEqualTo(hash(rawToken));
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getExpiresAt()).isNotNull();
    }

    @Test
    void shouldVerifyValidToken() {
        String rawToken = "valid-token";
        String tokenHash = hash(rawToken);
        RefreshToken entity = RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plus(Duration.ofHours(1)))
                .build();
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(entity));

        refreshTokenService.verifyRefreshToken(rawToken);
    }

    @Test
    void shouldThrowWhenTokenNotFound() {
        String rawToken = "unknown-token";
        when(refreshTokenRepository.findByTokenHash(hash(rawToken))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken(rawToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void shouldThrowWhenTokenRevoked() {
        String rawToken = "revoked-token";
        String tokenHash = hash(rawToken);
        RefreshToken entity = RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .tokenHash(tokenHash)
                .revoked(true)
                .expiresAt(LocalDateTime.now().plus(Duration.ofHours(1)))
                .build();
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken(rawToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void shouldThrowWhenTokenExpired() {
        String rawToken = "expired-token";
        String tokenHash = hash(rawToken);
        RefreshToken entity = RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .tokenHash(tokenHash)
                .revoked(false)
                .expiresAt(LocalDateTime.now().minus(Duration.ofHours(1)))
                .build();
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> refreshTokenService.verifyRefreshToken(rawToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void shouldRevokeToken() {
        String rawToken = "revoke-me";
        String tokenHash = hash(rawToken);
        RefreshToken entity = RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .tokenHash(tokenHash)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plus(Duration.ofHours(1)))
                .build();
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(entity));

        refreshTokenService.revokeRefreshToken(rawToken);

        assertThat(entity.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(entity);
    }

    @Test
    void shouldRotateTokens() {
        String oldRaw = "old-token";
        String newRaw = "new-token";
        String oldHash = hash(oldRaw);
        RefreshToken oldEntity = RefreshToken.builder()
                .id(1L)
                .userId(1L)
                .tokenHash(oldHash)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plus(Duration.ofHours(1)))
                .build();
        when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(oldEntity));

        refreshTokenService.rotateRefreshToken(oldRaw, newRaw);

        assertThat(oldEntity.isRevoked()).isTrue();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        java.util.List<RefreshToken> saved = captor.getAllValues();
        assertThat(saved.get(0).getTokenHash()).isEqualTo(oldHash);
        assertThat(saved.get(0).isRevoked()).isTrue();
        assertThat(saved.get(1).getUserId()).isEqualTo(1L);
        assertThat(saved.get(1).getTokenHash()).isEqualTo(hash(newRaw));
        assertThat(saved.get(1).isRevoked()).isFalse();
    }
}
