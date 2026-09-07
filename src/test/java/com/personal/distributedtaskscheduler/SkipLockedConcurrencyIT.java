package com.personal.distributedtaskscheduler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SkipLockedConcurrencyIT extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void skipLockedQuery_returnsDueJobToExactlyOneConcurrentTransaction() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(true);
            insertTenant(connection, tenantId);
            insertDueJob(connection, jobId, tenantId);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            CompletableFuture<List<UUID>> first = queryDueJobs(executorService, ready, start);
            CompletableFuture<List<UUID>> second = queryDueJobs(executorService, ready, start);

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<UUID> firstResult = first.get(5, TimeUnit.SECONDS);
            List<UUID> secondResult = second.get(5, TimeUnit.SECONDS);

            assertThat(firstResult.size() + secondResult.size()).isEqualTo(1);
            assertThat(firstResult.contains(jobId) || secondResult.contains(jobId)).isTrue();
            assertThat(firstResult.isEmpty() || secondResult.isEmpty()).isTrue();
        } finally {
            executorService.shutdownNow();
            try (Connection cleanup = openConnection()) {
                cleanup.setAutoCommit(true);
                cleanup.prepareStatement("DELETE FROM job_executions").executeUpdate();
                try (PreparedStatement deleteJob = cleanup.prepareStatement("DELETE FROM jobs WHERE id = ?")) {
                    deleteJob.setObject(1, jobId);
                    deleteJob.executeUpdate();
                }
                try (PreparedStatement deleteTenant = cleanup.prepareStatement("DELETE FROM tenants WHERE id = ?")) {
                    deleteTenant.setObject(1, tenantId);
                    deleteTenant.executeUpdate();
                }
            }
        }
    }

    private CompletableFuture<List<UUID>> queryDueJobs(
            ExecutorService executorService,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                ready.countDown();
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();

                List<UUID> jobIds = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM jobs WHERE status = ? AND next_fire_time <= ? " +
                                "ORDER BY next_fire_time LIMIT ? FOR UPDATE SKIP LOCKED"
                )) {
                    statement.setString(1, "ACTIVE");
                    statement.setTimestamp(2, Timestamp.from(Instant.now()));
                    statement.setInt(3, 1);

                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            jobIds.add((UUID) resultSet.getObject("id"));
                        }
                    }
                }

                if (!jobIds.isEmpty()) {
                    Thread.sleep(300);
                }
                connection.commit();
                return jobIds;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private void insertTenant(Connection connection, UUID tenantId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO tenants (id, first_name, last_name, email, created_at) VALUES (?, ?, ?, ?, now())"
        )) {
            statement.setObject(1, tenantId);
            statement.setString(2, "Concurrent");
            statement.setString(3, "Tester");
            statement.setString(4, "skip-locked-" + tenantId + "@example.com");
            statement.executeUpdate();
        }
    }

    private void insertDueJob(Connection connection, UUID jobId, UUID tenantId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO jobs (id, tenant_id, name, job_type, cron_expression, webhook_url, status, next_fire_time, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, now())"
        )) {
            statement.setObject(1, jobId);
            statement.setObject(2, tenantId);
            statement.setString(3, "due-job");
            statement.setString(4, "HTTP_CALLBACK");
            statement.setString(5, "0 * * * * *");
            statement.setString(6, "https://example.com/hook");
            statement.setString(7, "ACTIVE");
            statement.setTimestamp(8, Timestamp.from(Instant.now().minusSeconds(30)));
            statement.executeUpdate();
        }
    }
}
