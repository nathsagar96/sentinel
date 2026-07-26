package com.sentinel.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sentinel.auth.dto.SignupRequest;
import com.sentinel.auth.dto.UserResponse;
import com.sentinel.exception.BadRequestException;
import com.sentinel.exception.DuplicateResourceException;
import com.sentinel.exception.ResourceNotFoundException;
import com.sentinel.user.entity.AuthProvider;
import com.sentinel.user.entity.User;
import com.sentinel.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldSignupSuccessfully() {
        var request = new SignupRequest("Test User", "test@example.com", "password123");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(1L)
                    .name(user.getName())
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .provider(user.getProvider())
                    .build();
        });

        UserResponse response = authService.signup(request);

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Test User");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.provider()).isEqualTo(AuthProvider.LOCAL);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrow_whenEmailAlreadyExists() {
        var request = new SignupRequest("Test User", "existing@example.com", "password123");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginSuccessfully() {
        var request = new com.sentinel.auth.dto.LoginRequest("test@example.com", "password123");
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .password("encoded_password")
                .provider(AuthProvider.LOCAL)
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true);

        UserResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    void shouldThrow_whenUserNotFound_login() {
        var request = new com.sentinel.auth.dto.LoginRequest("notfound@example.com", "password123");
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void shouldThrow_whenPasswordMismatch() {
        var request = new com.sentinel.auth.dto.LoginRequest("test@example.com", "wrongpassword");
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded_password")
                .provider(AuthProvider.LOCAL)
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encoded_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void shouldThrow_whenOAuthUserTriesPasswordLogin() {
        var request = new com.sentinel.auth.dto.LoginRequest("oauth@example.com", "password123");
        User user = User.builder()
                .id(1L)
                .email("oauth@example.com")
                .password(null)
                .provider(AuthProvider.GOOGLE)
                .build();
        when(userRepository.findByEmail("oauth@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Google");
    }

    @Test
    void shouldGetCurrentUserSuccessfully() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .provider(AuthProvider.LOCAL)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = authService.getCurrentUser(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Test User");
    }

    @Test
    void shouldThrow_whenUserNotFound_getCurrentUser() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
