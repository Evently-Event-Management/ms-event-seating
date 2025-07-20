package com.example.mseventseating.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/public/hello")
    public Map<String, String> publicEndpoint() {
        return Collections.singletonMap("message", "Public endpoint - Hello!");
    }

    @GetMapping("/protected/hello")
    public Map<String, Object> protectedEndpoint(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "message", "Protected endpoint - Hello!",
            "username", jwt.getClaimAsString("preferred_username"),
            "scope", jwt.getClaimAsString("scope")
        );
    }
}
