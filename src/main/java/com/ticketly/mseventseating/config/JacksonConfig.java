package com.ticketly.mseventseating.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

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
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ✅ Add this line
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
        // This is the "unsafe" setting, only to be used for trusted, internal caching.
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        return mapper;
    }
}
