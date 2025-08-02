//package com.ticketly.mseventseating.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.cache.interceptor.KeyGenerator;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializationContext;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//import java.time.Duration;
//import java.util.UUID;
//
//@Configuration
//@EnableCaching
//public class CacheConfig {
//
//    @Value("${spring.cache.redis.time-to-live:3600}")
//    private long timeToLive;
//
//    @Value("${spring.cache.redis.key-prefix:event-seating-ms::}")
//    private String keyPrefix;
//
//    /**
//     * Configure Redis cache manager with appropriate serializers and TTL
//     */
//    @Bean
//    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
//        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofSeconds(timeToLive))
//                .prefixCacheNameWith(keyPrefix)
//                .serializeKeysWith(
//                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
//                .serializeValuesWith(
//                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
//                .disableCachingNullValues();
//
//        return RedisCacheManager.builder(connectionFactory)
//                .cacheDefaults(cacheConfig)
//                .build();
//    }
//
//    /**
//     * Custom key generator for organization-related caches
//     */
//    @Bean
//    public KeyGenerator organizationKeyGenerator() {
//        return (target, method, params) -> {
//            StringBuilder sb = new StringBuilder();
//            sb.append(target.getClass().getSimpleName());
//            sb.append(":");
//            sb.append(method.getName());
//
//            // Extract organization ID from parameters if present
//            for (Object param : params) {
//                if (param instanceof UUID) {
//                    sb.append(":");
//                    sb.append(param);
//                    break;
//                }
//            }
//
//            return sb.toString();
//        };
//    }
//}