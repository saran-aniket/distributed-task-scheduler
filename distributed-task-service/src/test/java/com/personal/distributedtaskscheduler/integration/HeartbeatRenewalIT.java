package com.personal.distributedtaskscheduler.integration;

import com.personal.distributedtaskscheduler.AbstractIntegrationTest;
import com.personal.distributedtaskscheduler.service.DistributedLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class HeartbeatRenewalIT extends AbstractIntegrationTest {

    @Autowired
    private DistributedLockService distributedLockService;

    @Test
    @Timeout(60)
    void renewalKeepsLeaseAliveBeyondInitialTtlUntilOwnerFinishes() throws Exception {
        String lockKey = "heartbeat-renewal";
        assertThat(distributedLockService.tryLock(lockKey, Duration.ZERO, Duration.ofSeconds(3))).isPresent();

        java.util.concurrent.ScheduledExecutorService renewer = Executors.newSingleThreadScheduledExecutor();
        renewer.scheduleAtFixedRate(
                () -> distributedLockService.renewLock(lockKey, Duration.ofSeconds(3)),
                1,
                1,
                TimeUnit.SECONDS
        );

        try {
            CompletableFuture<Optional<?>> nodeBAttempt = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return (Optional<?>) distributedLockService.tryLock(lockKey, Duration.ofMillis(200), Duration.ofSeconds(3));
            });

            Thread.sleep(10_000);

            assertThat(nodeBAttempt.get(7, TimeUnit.SECONDS)).isEmpty();
        } finally {
            renewer.shutdownNow();
            distributedLockService.unlock(lockKey);
        }

        assertThat(distributedLockService.tryLock(lockKey, Duration.ofMillis(200), Duration.ofSeconds(3))).isPresent();
        distributedLockService.unlock(lockKey);
    }
}
