package com.personal.distributedtaskscheduler.security;


import com.personal.distributedtaskscheduler.entity.ApiKey;
import com.personal.distributedtaskscheduler.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter{

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws IOException, ServletException {

        String requestUri = request.getRequestURI();
        if ("/actuator/health".equals(requestUri)
                || "/auth/login".equals(requestUri)
                || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-KEY");
        if (apiKey == null || !apiKey.contains(".")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key format");
            return;
        }

        String[] apiKeyParts = apiKey.split("\\.", 2);
        String prefix = apiKeyParts[0];
        String secret = apiKeyParts[1];

        ApiKey apiKeyInstance = apiKeyRepository.findApiKeyByKeyPrefix(prefix);

        if (apiKeyInstance == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Invalid API Key");
            return;
        }

        boolean isKeyValid = new BCryptPasswordEncoder().matches(secret, apiKeyInstance.getKeyHash());
        if (!isKeyValid) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Invalid API Key");
            return;
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + apiKeyInstance.getRole().name());
        ApiKeyAuthToken authToken = new ApiKeyAuthToken(
                apiKeyInstance.getTenant().getId().toString(),
                List.of(authority),
                apiKeyInstance.getKeyPrefix());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

}
