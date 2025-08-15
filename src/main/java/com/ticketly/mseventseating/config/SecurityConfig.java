package com.ticketly.mseventseating.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Applies the `corsConfigurationSource` bean
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/v1/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }
//    @Bean
//    public SecurityFilterChain debuggingFilterChain(HttpSecurity http) throws Exception {
//        http
//                // Apply the corsConfigurationSource bean
//                .cors(Customizer.withDefaults())
//                // Disable CSRF
//                .csrf(AbstractHttpConfigurer::disable)
//                // Temporarily permit absolutely all requests
//                .authorizeHttpRequests(authorize -> authorize
//                        .anyRequest().permitAll()
//                );
//        return http.build();
//    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return jwtConverter;
    }

//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//        // Define the allowed origins (your frontend URLs)
//        configuration.setAllowedOrigins(List.of("http://localhost:8090"));
//        // Define the allowed HTTP methods
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
//        // Define the allowed headers from the client
//        configuration.setAllowedHeaders(List.of("*"));
//        // Define the exposed headers (headers that browsers are allowed to access)
//        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
//        // Allow credentials (e.g., cookies, authorization headers)
//        configuration.setAllowCredentials(true);
//        // Max age for CORS preflight requests caching (in seconds)
//        configuration.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        // Apply this configuration to all endpoints in your application
//        source.registerCorsConfiguration("/**", configuration);
//        return source;
//    }
}
