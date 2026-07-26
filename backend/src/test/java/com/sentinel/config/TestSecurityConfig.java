package com.sentinel.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(
                        (request, response, filterChain) -> {
                            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                                SecurityContextHolder.getContext()
                                        .setAuthentication(
                                                new UsernamePasswordAuthenticationToken(1L, null, List.of()));
                            }
                            filterChain.doFilter(request, response);
                        },
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
