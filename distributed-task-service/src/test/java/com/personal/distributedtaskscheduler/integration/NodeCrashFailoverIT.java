package com.personal.distributedtaskscheduler.integration;

import com.personal.distributedtaskscheduler.AbstractIntegrationTest;
import com.personal.distributedtaskscheduler.service.DistributedLockService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NodeCrashFailoverIT extends AbstractIntegrationTest {

    @Autowired
    private DistributedLockService distributedLockService;

    @Test
    @Timeout(10)
    void expiredLease_allowsAnotherNodeToReclaimLock() throws Exception {
        String lockKey = "execution-crash";

        assertThat(distributedLockService.tryLock(lockKey, Duration.ZERO, Duration.ofSeconds(2))).isPresent();

        Thread.sleep(2_500);

        Optional<?> nodeBLock = distributedLockService.tryLock(lockKey, Duration.ofMillis(200), Duration.ofSeconds(2));
        assertThat(nodeBLock).isPresent();
        distributedLockService.unlock(lockKey);
    }
}
