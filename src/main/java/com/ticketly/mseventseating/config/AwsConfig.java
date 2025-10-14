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

import java.net.URI;

@Configuration
public class AwsConfig {

    // A single endpoint for all local AWS services (LocalStack)
    @Value("${aws.local.endpoint:#{null}}")
    private String localEndpoint;

    @Value("${aws.region}")
    private String region;

    // Credentials from environment variables
    @Value("${aws.credentials.access-key:#{null}}")
    private String accessKeyId;

    @Value("${aws.credentials.secret-key:#{null}}")
    private String secretAccessKey;

    // Credentials for local development
    @Value("${aws.local.access-key:test}")
    private String localAccessKey;

    @Value("${aws.local.secret-key:test}")
    private String localSecretKey;

    /**
     * Configures the S3Client bean.
     * In a 'dev' or 'test' profile, it points to LocalStack.
     * In any other profile (like 'prod'), it uses AWS credentials from environment variables.
     */
    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region));

        if (localEndpoint != null && !localEndpoint.isBlank()) {
            // For local development with LocalStack
            builder.endpointOverride(URI.create(localEndpoint))
                    .forcePathStyle(true)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(localAccessKey, localSecretKey)));
        } else if (accessKeyId != null && secretAccessKey != null) {
            // For production with explicit credentials
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        }
        // If no credentials provided, fall back to default AWS credential provider chain

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
            // For local development with LocalStack
            S3Configuration s3Configuration = S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build();

            builder.endpointOverride(URI.create(localEndpoint))
                    .serviceConfiguration(s3Configuration)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(localAccessKey, localSecretKey)));
        } else if (accessKeyId != null && secretAccessKey != null) {
            // For production with explicit credentials
            S3Configuration s3Configuration = S3Configuration.builder()
                    .pathStyleAccessEnabled(false)
                    .build();

            builder.serviceConfiguration(s3Configuration)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        }
        // If no credentials provided, fall back to default AWS credential provider chain

        return builder.build();
    }
}
