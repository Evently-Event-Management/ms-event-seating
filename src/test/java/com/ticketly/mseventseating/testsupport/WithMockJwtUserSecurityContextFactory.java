// Place this in the same test support package
package com.ticketly.mseventseating.testsupport;

import com.ticketly.mseventseating.config.KeycloakRealmRoleConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class WithMockJwtUserSecurityContextFactory implements WithSecurityContextFactory<WithMockJwtUser> {

    // You can autowire your real converter to ensure the test logic matches production
    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Override
    public SecurityContext createSecurityContext(WithMockJwtUser annotation) {
        List<String> roles = List.of(annotation.roles());

        // 1. Create a mock JWT with the desired claims
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject(annotation.username())
                .claim("realm_access", Map.of("roles", roles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        // 2. Use your actual converter to create the authorities
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // 3. Create the Authentication object
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);

        // 4. Create and return a security context
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        return context;
    }
}