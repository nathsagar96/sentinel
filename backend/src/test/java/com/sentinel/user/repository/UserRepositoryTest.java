package com.sentinel.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sentinel.user.entity.AuthProvider;
import com.sentinel.user.entity.User;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
class UserRepositoryTest {

    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @BeforeAll
    static void init() {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindByEmail() {
        User user = User.builder()
                .email("test@example.com")
                .name("Test User")
                .password("password123")
                .provider(AuthProvider.LOCAL)
                .build();

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User");
        assertThat(found.get().getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldCheckIfExistsByEmail() {
        User user = User.builder()
                .email("exist@example.com")
                .name("Exist User")
                .provider(AuthProvider.GOOGLE)
                .providerId("google-123")
                .build();

        userRepository.save(user);

        assertThat(userRepository.existsByEmail("exist@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("notfound@example.com")).isFalse();
    }
}
