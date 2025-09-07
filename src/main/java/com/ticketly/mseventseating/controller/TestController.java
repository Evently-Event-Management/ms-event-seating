package com.ticketly.mseventseating.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class TestController {

    /**
     * Public endpoint that returns a simple hello message.
     *
     * @return A map containing a hello message.
     */
    @GetMapping("/public/hello")
    public Map<String, String> publicEndpoint() {
        return Collections.singletonMap("message", "Public endpoint - Hello!");
    }

    /**
     * Protected endpoint that returns a hello message and user info from JWT.
     *
     * @param jwt The authenticated user's JWT.
     * @return A map containing a hello message and user info.
     */
    @GetMapping("/protected/hello")
    public Map<String, Object> protectedEndpoint(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "message", "Protected endpoint - Hello!",
            "username", jwt.getClaimAsString("preferred_username"),
            "scope", jwt.getClaimAsString("scope"),
                "uid", jwt.getClaimAsString("sub")
        );
    }
}
