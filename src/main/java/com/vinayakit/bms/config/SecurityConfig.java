package com.vinayakit.bms.config;

import com.vinayakit.bms.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwkSetUri(
                                "https://www.googleapis.com/oauth2/v3/certs")
                                .jwtAuthenticationConverter(jwtService)))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/actuator/health").permitAll()
                        // Admin only
                        .requestMatchers("/api/v1/reports/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/customers/**").hasRole("ADMIN")
                        // All authenticated users
                        .requestMatchers("/api/v1/accounts/**").authenticated()
                        .requestMatchers("/api/v1/transactions/**").authenticated()
                        .requestMatchers("/api/v1/recurring-payments/**").authenticated()
                        .requestMatchers("/api/v1/fraud/**").authenticated()
                        .requestMatchers("/api/v1/loans/**").authenticated()
                        // Anything else - block by default
                        .anyRequest().denyAll()
                ).build();
    }
}
