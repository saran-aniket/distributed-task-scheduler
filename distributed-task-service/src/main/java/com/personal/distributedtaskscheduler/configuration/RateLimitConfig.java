package com.personal.distributedtaskscheduler.configuration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    public ProxyManager<String> apiKeyRateLimitProxyManager(RedissonClient redissonClient) {
        if (!(redissonClient instanceof Redisson redisson)) {
            throw new IllegalStateException("Expected a Redisson client implementation for Bucket4j");
        }
        return RedissonBasedProxyManager.builderFor(redisson.getCommandExecutor())
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofDays(1)))
                .build();
    }
}
