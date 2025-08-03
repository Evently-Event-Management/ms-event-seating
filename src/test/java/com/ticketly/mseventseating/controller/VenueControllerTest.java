package com.ticketly.mseventseating.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.config.SecurityConfig;
import com.ticketly.mseventseating.dto.venue.VenueRequest;
import com.ticketly.mseventseating.dto.venue.VenueResponse;
import com.ticketly.mseventseating.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Use @WebMvcTest to test only the controller layer
@WebMvcTest(VenueController.class)
// Import SecurityConfig to apply our security rules to the test environment
@Import(SecurityConfig.class)
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc; // A utility to simulate HTTP requests

    @MockitoBean // Creates a mock of VenueService, replacing the real bean
    private VenueService venueService;

    @Autowired
    private ObjectMapper objectMapper; // For converting Java objects to JSON strings

    private String userId;
    private VenueResponse venueResponse;
    private VenueRequest venueRequest;
    private UUID venueId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        userId = "user-auth-id-123";
        venueId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        // Create a sample response object to be returned by the mocked service
        venueResponse = VenueResponse.builder()
                .id(venueId)
                .name("Test Venue")
                .address("123 Test St")
                .organizationId(organizationId)
                .build();

        // Create a sample request object for POST/PUT tests
        venueRequest = new VenueRequest();
        venueRequest.setName("Test Venue");
        venueRequest.setAddress("123 Test St");
        venueRequest.setOrganizationId(organizationId);
    }

    // Helper to create a mock JWT token with a specific subject (user ID)
    private Jwt createMockJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", userId)
                .build();
    }

    @Test
    void getAllVenues_ShouldReturnVenueList() throws Exception {
        // Arrange
        List<VenueResponse> venueList = Collections.singletonList(venueResponse);
        when(venueService.getAllVenuesForUser(userId)).thenReturn(venueList);

        // Act & Assert
        mockMvc.perform(get("/v1/venues")
                        .with(jwt().jwt(createMockJwt()))) // Simulate an authenticated request with our JWT
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is(venueResponse.getName())));
    }

    @Test
    void getVenuesByOrganization_ShouldReturnVenueList() throws Exception {
        // Arrange
        List<VenueResponse> venueList = Collections.singletonList(venueResponse);
        when(venueService.getAllVenuesByOrganization(organizationId, userId)).thenReturn(venueList);

        // Act & Assert
        mockMvc.perform(get("/v1/venues/organization/{organizationId}", organizationId)
                        .with(jwt().jwt(createMockJwt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(venueId.toString())));
    }

    @Test
    void getVenueById_ShouldReturnVenue() throws Exception {
        // Arrange
        when(venueService.getVenueById(venueId, userId)).thenReturn(venueResponse);

        // Act & Assert
        mockMvc.perform(get("/v1/venues/{id}", venueId)
                        .with(jwt().jwt(createMockJwt())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(venueResponse.getName())))
                .andExpect(jsonPath("$.address", is(venueResponse.getAddress())));
    }

    @Test
    void createVenue_WithValidRequest_ShouldReturnCreatedVenue() throws Exception {
        // Arrange
        when(venueService.createVenue(any(VenueRequest.class), eq(userId))).thenReturn(venueResponse);

        // Act & Assert
        mockMvc.perform(post("/v1/venues")
                        .with(jwt().jwt(createMockJwt()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(venueRequest))) // Convert request object to JSON
                .andExpect(status().isCreated()) // Expect HTTP 201
                .andExpect(jsonPath("$.id", is(venueId.toString())));
    }

    @Test
    void updateVenue_WithValidRequest_ShouldReturnUpdatedVenue() throws Exception {
        // Arrange
        when(venueService.updateVenue(eq(venueId), any(VenueRequest.class), eq(userId))).thenReturn(venueResponse);

        // Act & Assert
        mockMvc.perform(put("/v1/venues/{id}", venueId)
                        .with(jwt().jwt(createMockJwt()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(venueRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(venueId.toString())));
    }

    @Test
    void deleteVenue_ShouldReturnNoContent() throws Exception {
        // Arrange
        // Mock the service's void method
        doNothing().when(venueService).deleteVenue(venueId, userId);

        // Act & Assert
        mockMvc.perform(delete("/v1/venues/{id}", venueId)
                        .with(jwt().jwt(createMockJwt())))
                .andExpect(status().isNoContent()); // Expect HTTP 204
    }
}
