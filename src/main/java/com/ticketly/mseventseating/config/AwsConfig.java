package com.ticketly.mseventseating.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.scheduler.SchedulerClient;
import software.amazon.awssdk.services.scheduler.SchedulerClientBuilder;

import java.net.URI;

@Configuration
public class AwsConfig {

    // A single endpoint for all local AWS services (LocalStack)
    @Value("${aws.local.endpoint:#{null}}")
    private String localEndpoint;

    @Value("${aws.region}")
    private String region;

    // Credentials for local development
    @Value("${aws.local.access-key:test}")
    private String accessKey;

    @Value("${aws.local.secret-key:test}")
    private String secretKey;

    /**
     * Configures the S3Client bean.
     * In a 'dev' or 'test' profile, it points to LocalStack.
     * In any other profile (like 'prod'), it uses default AWS credentials and endpoints.
     */
    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region));

        if (localEndpoint != null && !localEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(localEndpoint))
                    .forcePathStyle(true)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)));
        }

        return builder.build();
    }

    /**
     * Configures the S3Presigner bean for generating presigned URLs.
     * Points to LocalStack for local development.
     */
    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region));

        if (localEndpoint != null && !localEndpoint.isBlank()) {
            S3Configuration s3Configuration = S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build();

            builder.endpointOverride(URI.create(localEndpoint))
                    .serviceConfiguration(s3Configuration)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)));
        }

        return builder.build();
    }

    /**
     * Configures the SchedulerClient bean for EventBridge Scheduler.
     * Points to LocalStack for local development.
     */
    @Bean
    public SchedulerClient schedulerClient() {
        SchedulerClientBuilder builder = SchedulerClient.builder()
                .region(Region.of(region));

        if (localEndpoint != null && !localEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(localEndpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)));
        }

        return builder.build();
    }
}
