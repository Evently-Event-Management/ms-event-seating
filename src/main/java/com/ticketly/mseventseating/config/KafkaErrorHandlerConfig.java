package com.ticketly.mseventseating.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.ticketly.mseventseating.dto.event.OrderUpdatedEventDto;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.RecordDeserializationException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.messaging.Message;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaErrorHandlerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:ms-event-seating}")
    private String defaultGroupId;

    private final ObjectMapper objectMapper;
    
    public KafkaErrorHandlerConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Bean
    public ConsumerFactory<String, Object> defaultConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, defaultGroupId);

        // Configure JsonDeserializer properties through the properties map
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OrderUpdatedEventDto.class.getName());

        // Important: Skip deserialization errors instead of retrying infinitely
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());

        // Create a JsonDeserializer with our custom ObjectMapper that handles empty UUID strings
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(objectMapper);
        
        // Use ErrorHandlingDeserializer to wrap the JsonDeserializer
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(jsonDeserializer)
        );
    }

    @Bean
    public CommonErrorHandler commonErrorHandler() {
        return getDefaultErrorHandler();
    }

    @Bean
    public ConsumerAwareListenerErrorHandler kafkaErrorHandler() {
        return (Message<?> message, ListenerExecutionFailedException exception, Consumer<?, ?> consumer) -> {
            // Extract info from the failed record if possible
            if (message.getPayload() instanceof ConsumerRecord<?, ?> record) {
                String topic = record.topic();
                long offset = record.offset();
                int partition = record.partition();

                if (isDeserializationException(exception)) {
                    log.error("Deserialization error for topic {}, partition {}, offset {}. " +
                                    "Ignoring message and continuing. Error: {}",
                            topic, partition, offset, exception.getMessage());

                    // If it's specifically a DeserializationException, we can extract the raw data
                    if (exception.getCause() instanceof DeserializationException deserEx) {
                        byte[] data = deserEx.getData();
                        if (data != null) {
                            log.debug("Problematic payload (base64): {}",
                                    java.util.Base64.getEncoder().encodeToString(data));
                        }
                    }
                } else {
                    log.error("Error processing record from topic {}, partition {}, offset {}. " +
                                    "Error: {}",
                            topic, partition, offset, exception.getMessage());
                }
            } else {
                log.error("Error processing message: {}", exception.getMessage(), exception);
            }

            // Return an empty Object instead of null to satisfy @NonNullApi
            return new Object();
        };
    }

    @Value("${spring.kafka.dead-letter-topic:ticketly.order.dlq}")
    private String deadLetterTopic;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(defaultConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        DefaultErrorHandler errorHandler = getDefaultErrorHandler();

        // Add non-retry-able exceptions (permanent failures)
        errorHandler.addNotRetryableExceptions(
            IllegalArgumentException.class,
            IllegalStateException.class,
            // Add deserialization related exceptions as non-retryable
            DeserializationException.class,
            RecordDeserializationException.class,
            JsonMappingException.class,
            InvalidFormatException.class,
            UnrecognizedPropertyException.class
        );

        // Configure dead letter publishing for deserialization errors
        errorHandler.setRetryListeners(
            (record, ex, deliveryAttempt) -> {
                if (isDeserializationException(ex)) {
                    log.warn("Deserialization error detected on record to {}-{} at offset {}. " +
                            "Message will be skipped after retries.",
                            record.topic(), record.partition(), record.offset());
                }
            }
        );

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
    
    // Bean to configure DeadLetterPublishingRecoverer if needed in the future
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaOperations<Object, Object> template) {
        return new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(deadLetterTopic, 0));
    }

    // Helper method for error handler creation
    private static @NotNull DefaultErrorHandler getDefaultErrorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(5000L, 2.0);
        backOff.setMaxInterval(300000L);
        backOff.setMaxElapsedTime(1800000L);

        return new DefaultErrorHandler(
            (record, exception) -> {
                if (isDeserializationException(exception)) {
                    // For deserialization errors, log but don't try to recover - just skip the message
                    log.warn("Skipping non-deserializable message. Topic: {}, Partition: {}, Offset: {}, Exception: {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage());
                } else {
                    // For other errors, log as error as we might retry
                    log.error("Processing failed for record. Topic: {}, Partition: {}, Offset: {}, Exception: {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage());
                }
            },
            backOff
        );
    }

    /**
     * Helper method to check if the exception is related to deserialization
     */
    private static boolean isDeserializationException(Exception exception) {
        Throwable cause = exception;

        // If it's a listener execution exception, get the cause
        if (exception instanceof ListenerExecutionFailedException) {
            cause = exception.getCause();
        }

        // Check if the exception or any of its causes is a deserialization exception
        while (cause != null) {
            if (cause instanceof DeserializationException ||
                cause instanceof RecordDeserializationException ||
                cause instanceof JsonMappingException) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }
}
