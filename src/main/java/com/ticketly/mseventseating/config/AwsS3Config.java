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
public class AwsS3Config {

    // Make endpoint optional by defaulting to null if not found
    @Value("${aws.s3.endpoint:#{null}}")
    private String endpoint;

    @Value("${aws.s3.region}")
    private String region;

    // Make keys optional for production
    @Value("${aws.s3.access-key:#{null}}")
    private String accessKey;

    @Value("${aws.s3.secret-key:#{null}}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region));

        // Check if an endpoint is configured (for dev/localstack)
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint))
                    .forcePathStyle(true)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)));
        }
        // If no endpoint is set, the SDK uses default provider chain for credentials
        // and standard AWS endpoint. This is the production behavior.

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(region));

        // Check if an endpoint is configured (for dev/localstack)
        if (endpoint != null) {
            S3Configuration s3Configuration = S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build();

            builder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(s3Configuration)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)));
        }
        // If no endpoint, SDK defaults are used for production.

        return builder.build();
    }
}