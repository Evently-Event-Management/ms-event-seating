//package com.ticketly.mseventseating.config;
//
//import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.annotation.EnableKafka;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.core.ConsumerFactory;
//import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
//
//import java.util.Map;
//
///**
// * Explicit Kafka consumer configuration to override Spring Boot's autoconfiguration.
// * This can help resolve complex classpath or bean initialization issues.
// */
//@EnableKafka
//@Configuration
//public class KafkaConsumerConfig {
//
//    /**
//     * Creates the factory responsible for producing Kafka Consumer instances.
//     * It uses the spring.kafka.consumer.* properties from your application.yml
//     * file, which are conveniently bundled into the KafkaProperties object.
//     *
//     * @param kafkaProperties The properties automatically mapped from application.yml.
//     * @return A configured ConsumerFactory.
//     */
//    @Bean
//    public ConsumerFactory<String, Object> consumerFactory(KafkaProperties kafkaProperties) {
//        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
//        return new DefaultKafkaConsumerFactory<>(props);
//    }
//
//    /**
//     * Creates the listener container factory, which is responsible for creating
//     * the message listener containers for methods annotated with @KafkaListener.
//     *
//     * @param consumerFactory The ConsumerFactory bean defined above.
//     * @return A configured ConcurrentKafkaListenerContainerFactory.
//     */
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
//            ConsumerFactory<String, Object> consumerFactory) {
//        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
//        factory.setConsumerFactory(consumerFactory);
//        return factory;
//    }
//}