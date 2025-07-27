package com.ticketly.mseventseating.config; // Or any appropriate package

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // ✅ Use type-safe accessor to get the map, avoids the first warning
        final Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

        if (realmAccess == null || realmAccess.isEmpty()) {
            return Collections.emptyList();
        }

        final Object rolesObject = realmAccess.get("roles");

        // ✅ Check if the roles object is actually a List
        if (!(rolesObject instanceof List<?> rawRoles)) {
            return Collections.emptyList();
        }

        // ✅ Safely cast and process the list, avoids the second warning
        return rawRoles.stream()
                .filter(role -> role instanceof String) // Ensure element is a String
                .map(role -> (String) role)             // Cast to String
                .map(roleName -> "ROLE_" + roleName)    // Prefix with ROLE_
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}