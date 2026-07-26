package com.sentinel.auth.service;

import com.sentinel.auth.dto.LoginRequest;
import com.sentinel.auth.dto.SignupRequest;
import com.sentinel.auth.dto.UserResponse;
import com.sentinel.exception.BadRequestException;
import com.sentinel.exception.DuplicateResourceException;
import com.sentinel.exception.ResourceNotFoundException;
import com.sentinel.user.entity.AuthProvider;
import com.sentinel.user.entity.User;
import com.sentinel.user.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Signup attempt with duplicate email for domain: ***@{}", normalizedEmail.replaceAll(".*@", ""));
            throw new DuplicateResourceException("Email already registered");
        }

        User user = User.builder()
                .name(request.name())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .provider(AuthProvider.LOCAL)
                .build();

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (user.getPassword() == null) {
            String providerDisplay =
                    switch (user.getProvider()) {
                        case GOOGLE -> "Google";
                        case GITHUB -> "GitHub";
                        default -> user.getProvider().name();
                    };
            throw new BadRequestException(
                    "This account uses " + providerDisplay + ". Please sign in with " + providerDisplay + ".",
                    providerDisplay);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn(
                    "AUTH_FAILURE email_domain=***@{} reason=invalid_credentials",
                    normalizedEmail.replaceAll(".*@", ""));
            throw new BadRequestException("Invalid email or password");
        }

        log.info("AUTH_SUCCESS user_id={} provider=LOCAL", user.getId());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserResponse.from(user);
    }
}
