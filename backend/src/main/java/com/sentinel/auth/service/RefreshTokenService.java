package com.sentinel.auth.service;

import com.sentinel.auth.entity.RefreshToken;
import com.sentinel.auth.repository.RefreshTokenRepository;
import com.sentinel.config.AppProperties;
import com.sentinel.exception.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppProperties appProperties;

    @Transactional
    public void storeRefreshToken(Long userId, String rawToken) {
        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plus(appProperties.jwt().refreshTokenExpiry()))
                .build();
        refreshTokenRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public void verifyRefreshToken(String rawToken) {
        RefreshToken entity = refreshTokenRepository
                .findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (entity.isRevoked()) {
            throw new BadRequestException("Invalid refresh token");
        }
        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid refresh token");
        }
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        RefreshToken entity = refreshTokenRepository
                .findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        entity.setRevoked(true);
        refreshTokenRepository.save(entity);
    }

    @Transactional
    public void rotateRefreshToken(String oldRawToken, String newRawToken) {
        RefreshToken oldEntity = refreshTokenRepository
                .findByTokenHash(hash(oldRawToken))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        oldEntity.setRevoked(true);
        refreshTokenRepository.save(oldEntity);

        RefreshToken newEntity = RefreshToken.builder()
                .userId(oldEntity.getUserId())
                .tokenHash(hash(newRawToken))
                .expiresAt(LocalDateTime.now().plus(appProperties.jwt().refreshTokenExpiry()))
                .build();
        refreshTokenRepository.save(newEntity);
    }

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
