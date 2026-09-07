package com.personal.distributedtaskscheduler.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class DistributedLockService {
    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);
    @Value("${spring.redisson-lock-prefix:distributed_lock_}")
    private static String LOCK_PREFIX;
    private final RedissonClient redissonClient;

    public DistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public boolean tryExecuteWithLock(String lockKey, Duration waitTime, Duration leaseTime) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        try {
            if (lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)) {
                log.info("Acquired lock for key: {}", lockKey);
                return true;
            } else {
                log.info("Could not acquire lock for key: {}", lockKey);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }finally {
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
                log.info("Released lock for key: {}", lockKey);
            }
        }
    }
}
