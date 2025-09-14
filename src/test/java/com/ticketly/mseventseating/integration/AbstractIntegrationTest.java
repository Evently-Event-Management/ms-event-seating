package com.ticketly.mseventseating.integration;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

// Load a specific test profile
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    // WireMock server extension that can be used in tests
    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    static final Network network = Network.newNetwork();

    // === DATABASE CONTAINER ===
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withNetwork(network)
                    .withNetworkAliases("postgres")
                    .withDatabaseName("event_seating")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withCommand("postgres", "-c", "wal_level=logical"); // Essential for Debezium

    // === REDIS CONTAINER ===
    static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:latest"))
                    .withExposedPorts(6379)
                    .withNetwork(network)
                    .withNetworkAliases("redis");

    // === KAFKA CONTAINER ===
    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"))
                    .withNetwork(network)
                    .withNetworkAliases("kafka");

    // === DEBEZIUM CONTAINER ===
    static final GenericContainer<?> debezium =
            new GenericContainer<>(DockerImageName.parse("debezium/connect:2.5"))
                    .withNetwork(network)
                    .withExposedPorts(8083)
                    .withEnv("BOOTSTRAP_SERVERS", "kafka:9092")
                    .withEnv("GROUP_ID", "1")
                    .withEnv("CONFIG_STORAGE_TOPIC", "debezium_configs")
                    .withEnv("OFFSET_STORAGE_TOPIC", "debezium_offsets")
                    .withEnv("STATUS_STORAGE_TOPIC", "debezium_statuses")
                    .dependsOn(kafka)
                    .waitingFor(Wait.forHttp("/").forStatusCode(200));;

    // === S3 (LOCALSTACK) CONTAINER ===
    static final GenericContainer<?> localstack =
            new GenericContainer<>(DockerImageName.parse("localstack/localstack:latest"))
                    .withExposedPorts(4566)
                    .withNetwork(network)
                    .withNetworkAliases("localstack")
                    .withEnv("SERVICES", "s3")
                    .withEnv("DEFAULT_REGION", "ap-south-1")
                    .waitingFor(Wait.forHttp("/_localstack/health").forStatusCode(200));


    // Start all containers before any tests run
    static {
        Startables.deepStart(postgres, redis, kafka, debezium, localstack).join();
    }

    // This method dynamically injects the container properties into the Spring context
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        // Mock OIDC Server Properties (using WireMock)
        String wiremockUrl = "http://localhost:" + wireMockServer.getPort();
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> wiremockUrl + "/realms/event-ticketing");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> wiremockUrl + "/realms/event-ticketing/protocol/openid-connect/certs");

        // Database
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", () -> kafka.getHost() + ":" + kafka.getMappedPort(9093));

        // AWS LocalStack for S3
        String localstackEndpoint = "http://" + localstack.getHost() + ":" + localstack.getMappedPort(4566);
        registry.add("aws.local.endpoint", () -> localstackEndpoint);
        registry.add("aws.local.access-key", () -> "test");
        registry.add("aws.local.secret-key", () -> "test");
    }
}