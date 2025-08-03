package com.ticketly.mseventseating.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.organization.OrganizationRequest;
import com.ticketly.mseventseating.dto.organization.OrganizationResponse;
import com.ticketly.mseventseating.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrganizationService organizationService;
    
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private OrganizationRequest testRequest;
    private OrganizationResponse testResponse;
    private final String USER_ID = "test-user-id";
    private final UUID ORG_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testRequest = OrganizationRequest.builder()
                .name("Test Organization")
                .website("https://test-org.com")
                .build();

        testResponse = OrganizationResponse.builder()
                .id(ORG_ID)
                .name("Test Organization")
                .website("https://test-org.com")
                .logoUrl("https://bucket.s3.amazonaws.com/logo.jpg")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void getAllOrganizations_ShouldReturnListOfOrganizations() throws Exception {
        // Arrange
        List<OrganizationResponse> organizations = Collections.singletonList(testResponse);
        when(organizationService.getAllOrganizationsForUser(USER_ID)).thenReturn(organizations);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/organizations")
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(ORG_ID.toString())))
                .andExpect(jsonPath("$[0].name", is("Test Organization")))
                .andExpect(jsonPath("$[0].website", is("https://test-org.com")));
    }

    @Test
    void getOrganizationById_ShouldReturnOrganization() throws Exception {
        // Arrange
        when(organizationService.getOrganizationById(ORG_ID, USER_ID)).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/organizations/{id}", ORG_ID)
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ORG_ID.toString())))
                .andExpect(jsonPath("$.name", is("Test Organization")))
                .andExpect(jsonPath("$.website", is("https://test-org.com")));
    }

    @Test
    void createOrganization_ShouldCreateAndReturnOrganization() throws Exception {
        // Arrange
        when(organizationService.createOrganization(any(OrganizationRequest.class), eq(USER_ID), any(Jwt.class)))
                .thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/organizations")
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(ORG_ID.toString())))
                .andExpect(jsonPath("$.name", is("Test Organization")))
                .andExpect(jsonPath("$.website", is("https://test-org.com")));
    }

    @Test
    void updateOrganization_ShouldUpdateAndReturnOrganization() throws Exception {
        // Arrange
        when(organizationService.updateOrganization(eq(ORG_ID), any(OrganizationRequest.class), eq(USER_ID)))
                .thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/v1/organizations/{id}", ORG_ID)
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ORG_ID.toString())))
                .andExpect(jsonPath("$.name", is("Test Organization")))
                .andExpect(jsonPath("$.website", is("https://test-org.com")));
    }

    @Test
    void uploadLogo_ShouldUploadAndReturnOrganization() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", 
                "logo.jpg", 
                "image/jpeg", 
                "test content".getBytes()
        );
        
        when(organizationService.uploadLogo(eq(ORG_ID), any(), eq(USER_ID))).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.multipart("/v1/organizations/{id}/logo", ORG_ID)
                .file(file)
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ORG_ID.toString())))
                .andExpect(jsonPath("$.logoUrl", is("https://bucket.s3.amazonaws.com/logo.jpg")));
    }

    @Test
    void deleteOrganization_ShouldDeleteAndReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(organizationService).deleteOrganization(ORG_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/v1/organizations/{id}", ORG_ID)
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeLogo_ShouldRemoveLogoAndReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(organizationService).removeLogo(ORG_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/v1/organizations/{id}/logo", ORG_ID)
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .with(csrf()))
                .andExpect(status().isNoContent());

        // Verify service method was called with correct parameters
        verify(organizationService, times(1)).removeLogo(ORG_ID, USER_ID);
    }
}
