package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.organization.OrganizationRequest;
import com.ticketly.mseventseating.dto.organization.OrganizationResponse;
import com.ticketly.mseventseating.service.organization.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrganizationControllerTest {

    @Mock
    private OrganizationService organizationService;

    @InjectMocks
    private OrganizationController organizationController;

    private UUID organizationId;
    private String userId;
    private Jwt jwt;
    private OrganizationRequest validRequest;
    private OrganizationResponse mockResponse;
    private List<OrganizationResponse> mockResponses;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = "user-123";

        // Create mock JWT with subject (user ID)
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), headers, claims);

        // Set up a valid organization request
        validRequest = OrganizationRequest.builder()
                .name("Test Organization")
                .website("https://example.com")
                .build();

        // Set up a mock organization response
        mockResponse = OrganizationResponse.builder()
                .id(organizationId)
                .name("Test Organization")
                .logoUrl("http://example.com/logo.png")
                .website("https://example.com")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // Create a list of mock responses
        mockResponses = Collections.singletonList(mockResponse);
    }

    @Test
    void getAllOrganizationsByOwner_ShouldReturnOrganizations() {
        // Arrange
        when(organizationService.getAllOrganizationsForUser(userId)).thenReturn(mockResponses);

        // Act
        ResponseEntity<List<OrganizationResponse>> response = organizationController.getAllOrganizationsByOwner(jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponses, response.getBody());
        verify(organizationService).getAllOrganizationsForUser(userId);
    }

    @Test
    void getOrganizationsByUserId_AdminEndpoint_ShouldReturnOrganizations() {
        // Arrange
        when(organizationService.getAllOrganizationsForUser(userId)).thenReturn(mockResponses);

        // Act
        ResponseEntity<List<OrganizationResponse>> response = organizationController.getOrganizationsByUserId(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponses, response.getBody());
        verify(organizationService).getAllOrganizationsForUser(userId);
    }

    @Test
    void getOrganizationById_ShouldReturnOrganization() {
        // Arrange
        when(organizationService.getOrganizationByIdOwner(organizationId, userId)).thenReturn(mockResponse);

        // Act
        ResponseEntity<OrganizationResponse> response = organizationController.getOrganizationById(organizationId, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(organizationService).getOrganizationByIdOwner(organizationId, userId);
    }

    @Test
    void getOrganizationByIdAdmin_ShouldReturnOrganization() {
        // Arrange
        when(organizationService.getOrganizationById(organizationId)).thenReturn(mockResponse);

        // Act
        ResponseEntity<OrganizationResponse> response = organizationController.getOrganizationByIdAdmin(organizationId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(organizationService).getOrganizationById(organizationId);
    }

    @Test
    void createOrganization_ShouldReturnCreatedOrganization() {
        // Arrange
        when(organizationService.createOrganization(eq(validRequest), eq(userId), any(Jwt.class)))
                .thenReturn(mockResponse);

        // Act
        ResponseEntity<OrganizationResponse> response = organizationController.createOrganization(validRequest, jwt);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(organizationService).createOrganization(eq(validRequest), eq(userId), any(Jwt.class));
    }

    @Test
    void updateOrganization_ShouldReturnUpdatedOrganization() {
        // Arrange
        when(organizationService.updateOrganization(organizationId, validRequest, userId)).thenReturn(mockResponse);

        // Act
        ResponseEntity<OrganizationResponse> response = organizationController.updateOrganization(
                organizationId, validRequest, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(organizationService).updateOrganization(organizationId, validRequest, userId);
    }

    @Test
    void uploadLogo_ShouldReturnUpdatedOrganization() throws IOException {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "file", "logo.png", MediaType.IMAGE_PNG_VALUE, "test image content".getBytes()
        );
        when(organizationService.uploadLogo(organizationId, file, userId)).thenReturn(mockResponse);

        // Act
        ResponseEntity<OrganizationResponse> response = organizationController.uploadLogo(organizationId, file, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(organizationService).uploadLogo(organizationId, file, userId);
    }

    @Test
    void removeLogo_ShouldReturnNoContent() {
        // Arrange
        doNothing().when(organizationService).removeLogo(organizationId, userId);

        // Act
        ResponseEntity<Void> response = organizationController.removeLogo(organizationId, jwt);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(organizationService).removeLogo(organizationId, userId);
    }

    @Test
    void deleteOrganization_ShouldReturnNoContent() {
        // Arrange
        doNothing().when(organizationService).deleteOrganization(organizationId, userId);

        // Act
        ResponseEntity<Void> response = organizationController.deleteOrganization(organizationId, jwt);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(organizationService).deleteOrganization(organizationId, userId);
    }
}
