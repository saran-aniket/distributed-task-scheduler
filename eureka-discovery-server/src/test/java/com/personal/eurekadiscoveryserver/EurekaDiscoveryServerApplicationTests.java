package com.personal.eurekadiscoveryserver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestPropertySource(properties = {
    "eureka.instance.lease-expiration-duration-in-seconds=3",
            "eureka.server.eviction-interval-timer-in-ms=1000",
            "eureka.client.register-with-eureka=false",
            "eureka.client.fetch-registry=false"
})
class EurekaDiscoveryServerApplicationTests {

    @Test
    void contextLoads() {
    }

    @LocalServerPort
    private int eurekaPort;

    private ConfigurableApplicationContext clientContext;

    @AfterEach
    void tearDown() {
        if (clientContext != null) {
            clientContext.close();
        }
    }

    @Test
    void clientRegistersAsUpAndIsEvictedAfterShutdown() {
        clientContext = new SpringApplication(TestEurekaClientApplication.class).run(
                "--server.port=0",
                "--spring.application.name=test-eureka-client",
                "--eureka.client.serviceUrl.defaultZone=http://localhost:" + eurekaPort + "/eureka/",
                "--eureka.client.register-with-eureka=true",
                "--eureka.client.fetch-registry=true",
                "--eureka.client.registry-fetch-interval-seconds=1",
                "--eureka.instance.lease-renewal-interval-in-seconds=1",
                "--eureka.instance.lease-expiration-duration-in-seconds=3",
                "--management.endpoints.enabled-by-default=false"
        );

        await().atMost(ofSeconds(10)).untilAsserted(() ->
                assertThat(fetchApplication()).contains("\"name\":\"TEST-EUREKA-CLIENT\"")
                        .contains("\"status\":\"UP\"")
        );

        clientContext.close();
        clientContext = null;

        await().atMost(ofSeconds(10)).untilAsserted(() ->
                assertThat(fetchRegistry()).doesNotContain("\"name\":\"TEST-EUREKA-CLIENT\"")
        );
    }

    private String fetchRegistry() {
        return eurekaClient()
                .get()
                .uri("/eureka/apps")
                .header("Accept", "application/json")
                .retrieve()
                .body(String.class);
    }

    private String fetchApplication() {
        return eurekaClient()
                .get()
                .uri(UriComponentsBuilder.fromPath("/eureka/apps/{name}").build("TEST-EUREKA-CLIENT"))
                .header("Accept", "application/json")
                .retrieve()
                .body(String.class);
    }

    private RestClient eurekaClient() {
        return RestClient.create("http://localhost:" + eurekaPort);
    }

    @SpringBootApplication
    @EnableEurekaServer
    static class TestEurekaServerApplication {
    }

    @SpringBootApplication
    @EnableDiscoveryClient
    static class TestEurekaClientApplication {
    }

}
