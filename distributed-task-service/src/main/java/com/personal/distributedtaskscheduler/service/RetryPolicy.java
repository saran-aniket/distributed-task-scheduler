package com.personal.distributedtaskscheduler.service;

import org.springframework.stereotype.Component;

@Component
public class RetryPolicy {

    private static final int MAX_BACKOFF_MULTIPLIER = 32;

    public int calculateBackoffSeconds(int attemptNumber, int backoffSeconds) {
        int multiplier = Math.min(1 << Math.max(attemptNumber - 1, 0), MAX_BACKOFF_MULTIPLIER);
        return multiplier * backoffSeconds;
    }

    public boolean shouldRetry(int attemptNumber, int maxRetries) {
        return attemptNumber < maxRetries;
    }
}
