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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private JwtDecoder jwtDecoder; // Mock JwtDecoder for @WebMvcTest

    private SeatingLayoutTemplateRequest testRequest;
    private SeatingLayoutTemplateDTO testResponse;
    private final String USER_ID = "test-user-id";
    private final UUID TEMPLATE_ID = UUID.randomUUID();
    private final UUID ORGANIZATION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        LayoutDataDTO layoutData = LayoutDataDTO.builder().name("Test Layout").build(); // Simplified for tests

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
    void getAllTemplatesByOrganization_ShouldReturnPaginatedListOfTemplates() throws Exception {
        // Arrange
        int page = 0;
        int size = 6;
        List<SeatingLayoutTemplateDTO> templates = Collections.singletonList(testResponse);
        Page<SeatingLayoutTemplateDTO> templatePage = new PageImpl<>(templates, PageRequest.of(page, size), templates.size());

        when(seatingLayoutTemplateService.getAllTemplatesByOrganizationId(ORGANIZATION_ID, USER_ID, page, size))
                .thenReturn(templatePage);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/v1/seating-templates/organization/{organizationId}", ORGANIZATION_ID)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size))
                        .with(jwt().jwt(builder -> builder.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(TEMPLATE_ID.toString())))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(seatingLayoutTemplateService).getAllTemplatesByOrganizationId(ORGANIZATION_ID, USER_ID, page, size);
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
                .andExpect(jsonPath("$.name", is("Test Template")));

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
                .andExpect(jsonPath("$.id", is(TEMPLATE_ID.toString())));

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
                .andExpect(jsonPath("$.id", is(TEMPLATE_ID.toString())));

        verify(seatingLayoutTemplateService).updateTemplate(eq(TEMPLATE_ID), any(SeatingLayoutTemplateRequest.class), eq(USER_ID));
    }

    @Test
    void deleteTemplate_ShouldDeleteAndReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(seatingLayoutTemplateService).deleteTemplate(TEMPLATE_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.delete("/v1/seating-templates/{id}", TEMPLATE_ID)
                        .with(jwt().jwt(builder -> builder.subject(USER_ID))))
                .andExpect(status().isNoContent());

        verify(seatingLayoutTemplateService).deleteTemplate(TEMPLATE_ID, USER_ID);
    }
}
