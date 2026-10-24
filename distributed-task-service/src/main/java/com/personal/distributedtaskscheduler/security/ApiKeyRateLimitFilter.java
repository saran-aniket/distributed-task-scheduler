package com.personal.distributedtaskscheduler.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class ApiKeyRateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_BUCKET_PREFIX = "api-key-rate-limit";

    private final ProxyManager<String> proxyManager;
    private final BucketConfiguration bucketConfiguration;

    public ApiKeyRateLimitFilter(ProxyManager<String> proxyManager,
                                @Value("${app.rate-limit.requests-per-minute:100}") long requestsPerMinute) {
        this.proxyManager = proxyManager;
        this.bucketConfiguration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(requestsPerMinute, Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws IOException, ServletException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof ApiKeyAuthToken apiKeyAuthToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = RATE_LIMIT_BUCKET_PREFIX + ":" + apiKeyAuthToken.getKeyPrefix();
        BucketProxy bucket = proxyManager.builder().build(bucketKey, bucketConfiguration);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(resolveRetryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please retry later.\"}");
    }

    private long resolveRetryAfterSeconds() {
        if (bucketConfiguration.getBandwidths().length == 0) {
            return 1L;
        }

        var bandwidth = bucketConfiguration.getBandwidths()[0];
        long refillTokens = bandwidth.getRefillTokens();
        if (refillTokens <= 0) {
            return 1L;
        }

        long nanosPerToken = bandwidth.getRefillPeriodNanos() / refillTokens;
        return Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(nanosPerToken));
    }
}
