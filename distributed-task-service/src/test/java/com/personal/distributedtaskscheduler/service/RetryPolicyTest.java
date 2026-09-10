package com.personal.distributedtaskscheduler.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetryPolicyTest {

    private final RetryPolicy retryPolicy = new RetryPolicy();

    @Test
    void calculateBackoffSeconds_matchesExpectedTableAndCapsHighAttempts() {
        assertThat(retryPolicy.calculateBackoffSeconds(1, 7)).isEqualTo(7);
        assertThat(retryPolicy.calculateBackoffSeconds(2, 7)).isEqualTo(14);
        assertThat(retryPolicy.calculateBackoffSeconds(3, 7)).isEqualTo(28);
        assertThat(retryPolicy.calculateBackoffSeconds(10, 7)).isEqualTo(224);
    }

    @Test
    void shouldRetry_stopsSchedulingOnceAttemptReachesMaxRetries() {
        assertThat(retryPolicy.shouldRetry(1, 3)).isTrue();
        assertThat(retryPolicy.shouldRetry(2, 3)).isTrue();
        assertThat(retryPolicy.shouldRetry(3, 3)).isFalse();
    }
}
