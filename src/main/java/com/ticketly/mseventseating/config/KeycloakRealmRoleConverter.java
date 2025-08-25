package com.ticketly.mseventseating.config; // Or any appropriate package

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Extract roles from realm_access
        final Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && !realmAccess.isEmpty()) {
            final Object rolesObject = realmAccess.get("roles");
            if (rolesObject instanceof List<?> rawRoles) {
                List<GrantedAuthority> realmRoles = rawRoles.stream()
                        .filter(role -> role instanceof String)
                        .map(role -> (String) role)
                        .map(roleName -> "ROLE_" + roleName)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
                authorities.addAll(realmRoles);
            }
        }

        // Extract scopes from scope claim
        String scopeClaimValue = jwt.getClaimAsString("scope");
        if (scopeClaimValue != null && !scopeClaimValue.isEmpty()) {
            List<GrantedAuthority> scopeAuthorities =
                    Stream.of(scopeClaimValue.split(" "))
                            .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                            .collect(Collectors.toList());
            authorities.addAll(scopeAuthorities);
        }

        return authorities;
    }
}