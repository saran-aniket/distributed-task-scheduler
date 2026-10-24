package com.personal.distributedtaskscheduler.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityFilterChainConfig {

    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final ApiKeyRateLimitFilter apiKeyRateLimitFilter;

    public SecurityFilterChainConfig(ApiKeyAuthFilter apiKeyAuthFilter,
                                    ApiKeyRateLimitFilter apiKeyRateLimitFilter) {
        this.apiKeyAuthFilter = apiKeyAuthFilter;
        this.apiKeyRateLimitFilter = apiKeyRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/auth/login").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new CustomAuthEntryPoint()))
                .sessionManagement(AbstractHttpConfigurer::disable)
                .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(apiKeyRateLimitFilter, ApiKeyAuthFilter.class)
                .build();
    }
}
