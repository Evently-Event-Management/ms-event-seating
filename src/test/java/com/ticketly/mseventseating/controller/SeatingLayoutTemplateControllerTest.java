package com.ticketly.mseventseating.controller;

import com.ticketly.mseventseating.dto.layout_template.LayoutDataDTO;
import com.ticketly.mseventseating.dto.layout_template.SeatingLayoutTemplateDTO;
import com.ticketly.mseventseating.dto.layout_template.SeatingLayoutTemplateRequest;
import com.ticketly.mseventseating.service.seating_layout.SeatingLayoutTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SeatingLayoutTemplateControllerTest {

    @Mock
    private SeatingLayoutTemplateService seatingLayoutTemplateService;

    @InjectMocks
    private SeatingLayoutTemplateController seatingLayoutTemplateController;

    private UUID templateId;
    private UUID organizationId;
    private String userId;
    private Jwt jwt;
    private SeatingLayoutTemplateRequest validRequest;
    private SeatingLayoutTemplateDTO templateDTO;
    private Page<SeatingLayoutTemplateDTO> templatePage;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        userId = "user-123";

        // Create mock JWT with subject (user ID)
        HashMap<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");
        HashMap<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);
        jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(60), headers, claims);

        // Create mock layout data
        LayoutDataDTO.Layout layout = new LayoutDataDTO.Layout();
        layout.setBlocks(new ArrayList<>());

        LayoutDataDTO layoutData = new LayoutDataDTO();
        layoutData.setName("Test Layout");
        layoutData.setLayout(layout);

        // Create valid request
        validRequest = new SeatingLayoutTemplateRequest();
        validRequest.setName("Test Template");
        validRequest.setOrganizationId(organizationId);
        validRequest.setLayoutData(layoutData);

        // Create template DTO
        templateDTO = SeatingLayoutTemplateDTO.builder()
                .id(templateId)
                .name("Test Template")
                .organizationId(organizationId)
                .layoutData(layoutData)
                .updatedAt(OffsetDateTime.now())
                .build();

        // Create list of template DTOs
        List<SeatingLayoutTemplateDTO> templateDTOs = Collections.singletonList(templateDTO);

        // Create page of template DTOs
        templatePage = new PageImpl<>(templateDTOs, PageRequest.of(0, 6), 1);
    }

    @Test
    void getAllTemplatesByOrganization_ShouldReturnTemplates() {
        // Arrange
        int page = 0;
        int size = 6;
        when(seatingLayoutTemplateService.getAllTemplatesByOrganizationId(organizationId, userId, page, size))
                .thenReturn(templatePage);

        // Act
        ResponseEntity<Page<SeatingLayoutTemplateDTO>> response =
            seatingLayoutTemplateController.getAllTemplatesByOrganization(organizationId, page, size, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(templateDTO, response.getBody().getContent().getFirst());
        verify(seatingLayoutTemplateService).getAllTemplatesByOrganizationId(organizationId, userId, page, size);
    }

    @Test
    void getTemplateById_ShouldReturnTemplate() {
        // Arrange
        when(seatingLayoutTemplateService.getTemplateById(templateId, userId)).thenReturn(templateDTO);

        // Act
        ResponseEntity<SeatingLayoutTemplateDTO> response = seatingLayoutTemplateController.getTemplateById(templateId, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(templateDTO, response.getBody());
        verify(seatingLayoutTemplateService).getTemplateById(templateId, userId);
    }

    @Test
    void createTemplate_ShouldReturnCreatedTemplate() {
        // Arrange
        when(seatingLayoutTemplateService.createTemplate(eq(validRequest), eq(userId), any(Jwt.class)))
                .thenReturn(templateDTO);

        // Act
        ResponseEntity<SeatingLayoutTemplateDTO> response =
            seatingLayoutTemplateController.createTemplate(validRequest, jwt);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(templateDTO, response.getBody());
        verify(seatingLayoutTemplateService).createTemplate(eq(validRequest), eq(userId), any(Jwt.class));
    }

    @Test
    void updateTemplate_ShouldReturnUpdatedTemplate() {
        // Arrange
        when(seatingLayoutTemplateService.updateTemplate(templateId, validRequest, userId))
                .thenReturn(templateDTO);

        // Act
        ResponseEntity<SeatingLayoutTemplateDTO> response =
            seatingLayoutTemplateController.updateTemplate(templateId, validRequest, jwt);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(templateDTO, response.getBody());
        verify(seatingLayoutTemplateService).updateTemplate(templateId, validRequest, userId);
    }

    @Test
    void deleteTemplate_ShouldReturnNoContent() {
        // Arrange
        doNothing().when(seatingLayoutTemplateService).deleteTemplate(templateId, userId);

        // Act
        ResponseEntity<Void> response = seatingLayoutTemplateController.deleteTemplate(templateId, jwt);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(seatingLayoutTemplateService).deleteTemplate(templateId, userId);
    }
}
