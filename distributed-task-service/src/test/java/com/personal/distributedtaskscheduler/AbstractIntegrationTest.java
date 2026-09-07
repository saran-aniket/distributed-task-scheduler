package com.personal.distributedtaskscheduler;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = DistributedTaskSchedulerApplication.class)
@Testcontainers
@ActiveProfiles("dev")
public abstract class AbstractIntegrationTest {

    private static final int REDIS_PORT = 6379;

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("task_scheduler_test")
            .withUsername("postgres")
            .withPassword("postgres");

    static {
        postgres.start();
    }

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(REDIS_PORT));

        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("scheduler.batch-size", () -> 10);
        registry.add("scheduler.dispatch-interval-ms", () -> "5000ms");
        registry.add("scheduler.poll-interval-ms", () -> 5000);
        registry.add("scheduler.jitter-ms", () -> 0);
        registry.add("spring.task.scheduling.enabled", () -> false);
    }
}