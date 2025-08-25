package com.ticketly.mseventseating.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class KeycloakConfig {

    @Value("${keycloak.auth-server-url:http://auth.ticketly.com:8080}")
    private String authServerUrl;

    @Value("${keycloak.realm:master}")
    private String realm;

    @Value("${keycloak.resource:events-service}")
    private String clientId;

    @Value("${keycloak.credentials.secret:}")
    private String clientSecret;

    @Bean
    public Keycloak keycloakAdminClient() {
        log.info("Initializing Keycloak admin client with server URL: {}, realm: {}", authServerUrl, realm);

        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
    }

}
