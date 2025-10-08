package com.ticketly.mseventseating.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.util.UUID;

@Configuration
public class JacksonConfig {

    /**
     * Custom UUID deserializer that handles empty strings by converting them to null
     */
    private static class EmptyStringToNullUuidDeserializer extends JsonDeserializer<UUID> {
        @Override
        public UUID deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value == null || value.isEmpty()) {
                return null;
            }
            return UUID.fromString(value);
        }
    }

    /**
     * This is the primary, default, and SAFE ObjectMapper for the application.
     * Spring will use this bean for all web-related JSON serialization/deserialization
     * (e.g., handling @RequestBody). It does NOT have default typing enabled,
     * which mitigates the insecure deserialization vulnerability.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Hibernate6Module());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ✅ Add this line
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Register custom deserializer for UUIDs to handle empty strings
        SimpleModule module = new SimpleModule();
        module.addDeserializer(UUID.class, new EmptyStringToNullUuidDeserializer());
        mapper.registerModule(module);
        
        return mapper;
    }

    /**
     * This is a specialized ObjectMapper created ONLY for Redis caching.
     * It has default typing enabled, which is necessary for the cache to
     * correctly deserialize Java objects. It should NOT be used for handling
     * any untrusted external input.
     */
    @Bean
    @Qualifier("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Hibernate6Module());
        // This is the "unsafe" setting, only to be used for trusted, internal caching.
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
