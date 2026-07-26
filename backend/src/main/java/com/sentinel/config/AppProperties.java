package com.sentinel.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid JwtProperties jwt,
        @Valid CorsProperties cors,
        @Valid OAuth2Properties oauth2,
        boolean secureCookies) {
    public record JwtProperties(
            @NotBlank String secret,
            @NotNull Duration accessTokenExpiry,
            @NotNull Duration refreshTokenExpiry) {}

    public record CorsProperties(List<@NotBlank String> allowedOrigins) {}

    public record OAuth2Properties(@NotBlank String redirectUri) {}
}
