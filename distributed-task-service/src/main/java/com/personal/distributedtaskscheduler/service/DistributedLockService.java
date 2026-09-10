package com.personal.distributedtaskscheduler.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class DistributedLockService {
    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);
    @Value("${spring.redisson-lock-prefix:distributed_lock_}")
    private String LOCK_PREFIX;
    private final RedissonClient redissonClient;

    public DistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public Optional<RLock> tryLock(String lockKey, Duration waitTime, Duration leaseTime) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        try {
            if (lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS)) {
                log.info("Acquired lock for key: {}", lockKey);
                return Optional.of(lock);
            } else {
                log.info("Could not acquire lock for key: {}", lockKey);
                return Optional.empty();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    public boolean tryExecuteWithLock(String lockKey, Duration waitTime, Duration leaseTime) {
        Optional<RLock> lock = tryLock(lockKey, waitTime, leaseTime);
        lock.ifPresent(ignored -> unlock(lockKey));
        return lock.isPresent();
    }

    public void renewLock(String lockKey, Duration leaseTime) {
        redissonClient.getBucket(LOCK_PREFIX + lockKey).expire(leaseTime);
        log.info("Renewed lock for key: {}", lockKey);
    }

    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("Released lock for key: {}", lockKey);
        }
    }
}
