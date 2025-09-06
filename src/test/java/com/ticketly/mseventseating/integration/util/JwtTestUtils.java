package com.ticketly.mseventseating.integration.util;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class JwtTestUtils {

    private static final RSAKey rsaKey;
    private static final JWKSet jwkSet;

    // Generate a key pair once for all tests
    static {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair keyPair = gen.generateKeyPair();

            rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
            jwkSet = new JWKSet(rsaKey);
        } catch (Exception e) {
            throw new RuntimeException("Could not generate RSA key pair", e);
        }
    }

    public static String getJwt(WireMockExtension wireMock, String subject, String tier) {
        // 1. Configure WireMock to serve the JWKSet and OIDC configuration
        setupWireMock(wireMock);

        // 2. Build the JWT claims
        String baseUrl = "http://localhost:" + wireMock.getPort();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(baseUrl + "/realms/event-ticketing")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("tier", tier) // Custom claim for subscription tier
                .claim("scope", "openid profile email")
                .claim("typ", "Bearer")
                .claim("azp", "test-client")
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build();
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);

        try {
            signedJWT.sign(new RSASSASigner(rsaKey));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Could not sign JWT", e);
        }
    }

    private static void setupWireMock(WireMockExtension wireMock) {
        String baseUrl = "http://localhost:" + wireMock.getPort();
        
        // Endpoint for OIDC discovery
        wireMock.stubFor(get(urlPathEqualTo("/realms/event-ticketing/.well-known/openid-configuration"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                        {
                          "issuer": "%s/realms/event-ticketing",
                          "jwks_uri": "%s/realms/event-ticketing/protocol/openid-connect/certs"
                        }
                        """.formatted(baseUrl, baseUrl))
                ));

        // Endpoint for JWKS (the public keys)
        wireMock.stubFor(get(urlPathEqualTo("/realms/event-ticketing/protocol/openid-connect/certs"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(jwkSet.toJSONObject().toString())
                ));
    }
}