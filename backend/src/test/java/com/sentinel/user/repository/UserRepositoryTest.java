package com.sentinel.user.repository;

import com.sentinel.user.entity.AuthProvider;
import com.sentinel.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

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
