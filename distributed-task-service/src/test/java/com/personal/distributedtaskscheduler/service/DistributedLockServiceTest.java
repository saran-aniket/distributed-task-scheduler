package com.personal.distributedtaskscheduler.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class DistributedLockServiceTest {

    private static final int REDIS_PORT = 6379;

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(REDIS_PORT);

    private DistributedLockService distributedLockService;
    private RedissonClient redissonClient;

    @BeforeEach
    void setUp() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://%s:%d".formatted(redis.getHost(), redis.getMappedPort(REDIS_PORT)));
        redissonClient = Redisson.create(config);
        distributedLockService = new DistributedLockService(redissonClient);
        ReflectionTestUtils.setField(distributedLockService, "LOCK_PREFIX", "test_lock_");
    }

    @AfterEach
    void tearDown() {
        redissonClient.shutdown();
    }

    @Test
    @Timeout(10)
    void lockLifecycle_acquireConflictUnlockAndReacquireBehaveAsExpected() throws Exception {
        String lockKey = "job-123";
        Duration waitTime = Duration.ofMillis(200);
        Duration leaseTime = Duration.ofSeconds(5);

        Optional<RLock> firstLock = distributedLockService.tryLock(lockKey, waitTime, leaseTime);
        assertThat(firstLock).isPresent();

        CompletableFuture<Boolean> secondAttempt = CompletableFuture.supplyAsync(() ->
                distributedLockService.tryLock(lockKey, waitTime, leaseTime).isPresent()
        );

        assertThat(secondAttempt.get(2, TimeUnit.SECONDS)).isFalse();

        distributedLockService.unlock(lockKey);

        Optional<RLock> reacquiredLock = distributedLockService.tryLock(lockKey, waitTime, leaseTime);
        assertThat(reacquiredLock).isPresent();
        distributedLockService.unlock(lockKey);
    }
}
