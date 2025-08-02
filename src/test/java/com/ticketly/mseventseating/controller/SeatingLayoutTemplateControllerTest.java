package com.ticketly.mseventseating.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.LayoutDataDTO;
import com.ticketly.mseventseating.dto.SeatingLayoutTemplateDTO;
import com.ticketly.mseventseating.dto.SeatingLayoutTemplateRequest;
import com.ticketly.mseventseating.service.SeatingLayoutTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeatingLayoutTemplateController.class)
class SeatingLayoutTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SeatingLayoutTemplateService seatingLayoutTemplateService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private SeatingLayoutTemplateRequest testRequest;
    private SeatingLayoutTemplateDTO testResponse;
    private final String USER_ID = "test-user-id";
    private final UUID TEMPLATE_ID = UUID.randomUUID();
    private final UUID ORGANIZATION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Create a simple layout data for testing
        LayoutDataDTO layoutData = LayoutDataDTO.builder()
                .name("Test Layout")
                .layout(LayoutDataDTO.Layout.builder()
                        .blocks(Collections.singletonList(
                                LayoutDataDTO.Block.builder()
                                        .id("block-1")
                                        .name("Block 1")
                                        .type("seated_grid")
                                        .position(LayoutDataDTO.Position.builder()
                                                .x(10.0)
                                                .y(20.0)
                                                .build())
                                        .rows(10)
                                        .columns(15)
                                        .startRowLabel("A")
                                        .startColumnLabel(1)
                                        .build()
                        ))
                        .build())
                .build();

        testRequest = SeatingLayoutTemplateRequest.builder()
                .name("Test Template")
                .organizationId(ORGANIZATION_ID)
                .layoutData(layoutData)
                .build();

        testResponse = SeatingLayoutTemplateDTO.builder()
                .id(TEMPLATE_ID)
                .organizationId(ORGANIZATION_ID)
                .name("Test Template")
                .layoutData(layoutData)
                .build();
    }

    @Test
    void getAllTemplatesByOrganization_ShouldReturnListOfTemplates() throws Exception {
        // Arrange
        List<SeatingLayoutTemplateDTO> templates = Collections.singletonList(testResponse);
        when(seatingLayoutTemplateService.getAllTemplatesByOrganizationId(ORGANIZATION_ID, USER_ID))
                .thenReturn(templates);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/seating-templates/organization/{organizationId}", ORGANIZATION_ID)
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(TEMPLATE_ID.toString())))
                .andExpect(jsonPath("$[0].name", is("Test Template")))
                .andExpect(jsonPath("$[0].organizationId", is(ORGANIZATION_ID.toString())));

        // Verify the service was called
        verify(seatingLayoutTemplateService).getAllTemplatesByOrganizationId(ORGANIZATION_ID, USER_ID);
    }

    @Test
    void getTemplateById_ShouldReturnTemplate() throws Exception {
        // Arrange
        when(seatingLayoutTemplateService.getTemplateById(TEMPLATE_ID, USER_ID)).thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/seating-templates/{id}", TEMPLATE_ID)
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(TEMPLATE_ID.toString())))
                .andExpect(jsonPath("$.name", is("Test Template")))
                .andExpect(jsonPath("$.organizationId", is(ORGANIZATION_ID.toString())));

        // Verify the service was called
        verify(seatingLayoutTemplateService).getTemplateById(TEMPLATE_ID, USER_ID);
    }

    @Test
    void createTemplate_ShouldCreateAndReturnTemplate() throws Exception {
        // Arrange
        when(seatingLayoutTemplateService.createTemplate(any(SeatingLayoutTemplateRequest.class), eq(USER_ID)))
                .thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/seating-templates")
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(TEMPLATE_ID.toString())))
                .andExpect(jsonPath("$.name", is("Test Template")))
                .andExpect(jsonPath("$.organizationId", is(ORGANIZATION_ID.toString())));

        // Verify the service was called
        verify(seatingLayoutTemplateService).createTemplate(any(SeatingLayoutTemplateRequest.class), eq(USER_ID));
    }

    @Test
    void updateTemplate_ShouldUpdateAndReturnTemplate() throws Exception {
        // Arrange
        when(seatingLayoutTemplateService.updateTemplate(eq(TEMPLATE_ID), any(SeatingLayoutTemplateRequest.class), eq(USER_ID)))
                .thenReturn(testResponse);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.put("/v1/seating-templates/{id}", TEMPLATE_ID)
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(TEMPLATE_ID.toString())))
                .andExpect(jsonPath("$.name", is("Test Template")))
                .andExpect(jsonPath("$.organizationId", is(ORGANIZATION_ID.toString())));

        // Verify the service was called
        verify(seatingLayoutTemplateService).updateTemplate(eq(TEMPLATE_ID), any(SeatingLayoutTemplateRequest.class), eq(USER_ID));
    }

    @Test
    void deleteTemplate_ShouldDeleteAndReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(seatingLayoutTemplateService).deleteTemplate(TEMPLATE_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/v1/seating-templates/{id}", TEMPLATE_ID)
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .with(csrf()))
                .andExpect(status().isNoContent());

        // Verify the service was called
        verify(seatingLayoutTemplateService).deleteTemplate(TEMPLATE_ID, USER_ID);
    }

    @Test
    void getAllTemplatesByOrganization_EmptyList_ShouldReturnEmptyArray() throws Exception {
        // Arrange
        when(seatingLayoutTemplateService.getAllTemplatesByOrganizationId(ORGANIZATION_ID, USER_ID))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/seating-templates/organization/{organizationId}", ORGANIZATION_ID)
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createTemplate_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Create invalid request (missing required fields)
        SeatingLayoutTemplateRequest invalidRequest = SeatingLayoutTemplateRequest.builder()
                .name("") // Empty name, should fail validation
                .build();

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/seating-templates")
                .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // Verify the service was never called
        verify(seatingLayoutTemplateService, never()).createTemplate(any(), any());
    }
}
