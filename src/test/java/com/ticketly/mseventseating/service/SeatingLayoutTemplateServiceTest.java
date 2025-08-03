package com.ticketly.mseventseating.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.LayoutDataDTO;
import com.ticketly.mseventseating.dto.SeatingLayoutTemplateDTO;
import com.ticketly.mseventseating.dto.SeatingLayoutTemplateRequest;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.SeatingLayoutTemplate;
import com.ticketly.mseventseating.model.SubscriptionLimitType;
import com.ticketly.mseventseating.repository.SeatingLayoutTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatingLayoutTemplateServiceTest {

    @Mock
    private SeatingLayoutTemplateRepository seatingLayoutTemplateRepository;

    @Mock
    private OrganizationOwnershipService ownershipService;

    @Mock
    private SubscriptionTierService subscriptionTierService;

    @Mock
    private Jwt mockJwt;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SeatingLayoutTemplateService seatingLayoutTemplateService;

    private Organization organization;
    private SeatingLayoutTemplate template;
    private UUID organizationId;
    private UUID templateId;
    private String userId;
    private LayoutDataDTO sampleLayoutData;

    @BeforeEach
    void setUp() throws IOException {
        organizationId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        userId = UUID.randomUUID().toString();

        organization = new Organization();
        organization.setId(organizationId);
        organization.setName("Test Organization");
        organization.setUserId(userId);

        String jsonContent = new String(Files.readAllBytes(
                Paths.get("src/test/resources/seating_layout.json")));
        sampleLayoutData = objectMapper.readValue(jsonContent, LayoutDataDTO.class);

        template = new SeatingLayoutTemplate();
        template.setId(templateId);
        template.setName("Test Template");
        template.setOrganization(organization);
        template.setLayoutData(objectMapper.writeValueAsString(sampleLayoutData));

        // Set default page size for the service
        ReflectionTestUtils.setField(seatingLayoutTemplateService, "defaultPageSize", 6);
    }

    @Test
    void getAllTemplatesByOrganizationId_ShouldReturnPaginatedTemplates_WhenUserHasAccess() {
        // Given
        int page = 0;
        int size = 10;
        List<SeatingLayoutTemplate> templates = Collections.singletonList(template);
        Page<SeatingLayoutTemplate> templatePage = new PageImpl<>(templates, PageRequest.of(page, size), templates.size());

        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(seatingLayoutTemplateRepository.findByOrganizationId(eq(organizationId), any(Pageable.class))).thenReturn(templatePage);

        // When
        Page<SeatingLayoutTemplateDTO> result = seatingLayoutTemplateService.getAllTemplatesByOrganizationId(organizationId, userId, page, size);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getContent().size());
        assertEquals(templateId, result.getContent().getFirst().getId());
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
    }

    @Test
    void getAllTemplatesByOrganizationId_ShouldPropagateException_WhenUserDoesNotHaveAccess() {
        // Given
        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // When/Then
        assertThrows(AuthorizationDeniedException.class,
                () -> seatingLayoutTemplateService.getAllTemplatesByOrganizationId(organizationId, userId, 0, 10));
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
        // Verify repository is never called
        verify(seatingLayoutTemplateRepository, never()).findByOrganizationId(any(UUID.class), any(Pageable.class));
    }

    @Test
    void getTemplateById_ShouldReturnTemplate_WhenUserHasAccess() {
        // Given
        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);

        // When
        SeatingLayoutTemplateDTO result = seatingLayoutTemplateService.getTemplateById(templateId, userId);

        // Then
        assertEquals(templateId, result.getId());
        assertEquals("Test Template", result.getName());
        assertEquals(organizationId, result.getOrganizationId());
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
    }

    @Test
    void getTemplateById_ShouldThrowException_WhenTemplateNotFound() {
        // Given
        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, 
            () -> seatingLayoutTemplateService.getTemplateById(templateId, userId));
        verify(ownershipService, never()).verifyOwnershipAndGetOrganization(any(), any());
    }

    @Test
    void getTemplateById_ShouldPropagateException_WhenUserDoesNotHaveAccess() {
        // Given
        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // When/Then
        assertThrows(AuthorizationDeniedException.class, 
            () -> seatingLayoutTemplateService.getTemplateById(templateId, userId));
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
    }

    @Test
    void createTemplate_ShouldCreateAndReturnTemplate_WhenUserHasAccessAndUnderLimit() {
        // Given
        SeatingLayoutTemplateRequest request = new SeatingLayoutTemplateRequest();
        request.setName("New Template");
        request.setOrganizationId(organizationId);
        request.setLayoutData(sampleLayoutData);

        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(seatingLayoutTemplateRepository.countByOrganizationId(organizationId)).thenReturn(2L);
        when(subscriptionTierService.getLimit(SubscriptionLimitType.MAX_SEATING_LAYOUTS_PER_ORG,mockJwt)).thenReturn(5);
        when(seatingLayoutTemplateRepository.save(any(SeatingLayoutTemplate.class))).thenAnswer(invocation -> {
            SeatingLayoutTemplate savedTemplate = invocation.getArgument(0);
            savedTemplate.setId(UUID.randomUUID());
            return savedTemplate;
        });

        // When
        SeatingLayoutTemplateDTO result = seatingLayoutTemplateService.createTemplate(request, userId, mockJwt);

        // Then
        assertNotNull(result);
        assertEquals("New Template", result.getName());
        assertEquals(organizationId, result.getOrganizationId());
        assertNotNull(result.getLayoutData());
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(subscriptionTierService).getLimit(SubscriptionLimitType.MAX_SEATING_LAYOUTS_PER_ORG,mockJwt);
        verify(seatingLayoutTemplateRepository).countByOrganizationId(organizationId);
        verify(seatingLayoutTemplateRepository).save(any(SeatingLayoutTemplate.class));
    }

    @Test
    void createTemplate_ShouldThrowException_WhenUserExceedsTierLimit() {
        // Given
        SeatingLayoutTemplateRequest request = new SeatingLayoutTemplateRequest();
        request.setName("New Template");
        request.setOrganizationId(organizationId);
        request.setLayoutData(sampleLayoutData);

        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(seatingLayoutTemplateRepository.countByOrganizationId(organizationId)).thenReturn(5L);
        when(subscriptionTierService.getLimit(SubscriptionLimitType.MAX_SEATING_LAYOUTS_PER_ORG,mockJwt)).thenReturn(5);

        // When/Then
        assertThrows(BadRequestException.class,
            () -> seatingLayoutTemplateService.createTemplate(request, userId, mockJwt));

        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(subscriptionTierService).getLimit(SubscriptionLimitType.MAX_SEATING_LAYOUTS_PER_ORG,mockJwt);
        verify(seatingLayoutTemplateRepository).countByOrganizationId(organizationId);
        verify(seatingLayoutTemplateRepository, never()).save(any(SeatingLayoutTemplate.class));
    }

    @Test
    void createTemplate_ShouldPropagateException_WhenUserDoesNotHaveAccess() {
        // Given
        SeatingLayoutTemplateRequest request = new SeatingLayoutTemplateRequest();
        request.setName("New Template");
        request.setOrganizationId(organizationId);
        request.setLayoutData(sampleLayoutData);

        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // When/Then
        assertThrows(AuthorizationDeniedException.class, 
            () -> seatingLayoutTemplateService.createTemplate(request, userId, mockJwt));
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(seatingLayoutTemplateRepository, never()).save(any(SeatingLayoutTemplate.class));
        verify(subscriptionTierService, never()).getLimit(any(),any());
    }

    @Test
    void updateTemplate_ShouldUpdateAndReturnTemplate_WhenUserHasAccess() {
        // Given
        SeatingLayoutTemplateRequest request = new SeatingLayoutTemplateRequest();
        request.setName("Updated Template");
        request.setOrganizationId(organizationId);
        request.setLayoutData(sampleLayoutData);

        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(seatingLayoutTemplateRepository.save(any(SeatingLayoutTemplate.class))).thenReturn(template);

        // When
        SeatingLayoutTemplateDTO result = seatingLayoutTemplateService.updateTemplate(templateId, request, userId);

        // Then
        assertNotNull(result);
        assertEquals("Updated Template", template.getName());
        assertEquals(organizationId, result.getOrganizationId());
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(seatingLayoutTemplateRepository).save(template);
    }

    @Test
    void updateTemplate_ShouldThrowException_WhenTemplateNotFound() {
        // Given
        SeatingLayoutTemplateRequest request = new SeatingLayoutTemplateRequest();
        request.setName("Updated Template");
        request.setOrganizationId(organizationId);

        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, 
            () -> seatingLayoutTemplateService.updateTemplate(templateId, request, userId));
        verify(seatingLayoutTemplateRepository, never()).save(any(SeatingLayoutTemplate.class));
    }

    @Test
    void updateTemplate_ShouldPropagateException_WhenUserDoesNotHaveAccess() {
        // Given
        SeatingLayoutTemplateRequest request = new SeatingLayoutTemplateRequest();
        request.setName("Updated Template");
        request.setOrganizationId(organizationId);

        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // When/Then
        assertThrows(AuthorizationDeniedException.class, 
            () -> seatingLayoutTemplateService.updateTemplate(templateId, request, userId));
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(seatingLayoutTemplateRepository, never()).save(any(SeatingLayoutTemplate.class));
    }

    @Test
    void deleteTemplate_ShouldDeleteTemplate_WhenUserHasAccess() {
        // Given
        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);

        // When
        seatingLayoutTemplateService.deleteTemplate(templateId, userId);

        // Then
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(seatingLayoutTemplateRepository).delete(template);
    }

    @Test
    void deleteTemplate_ShouldThrowException_WhenTemplateNotFound() {
        // Given
        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, 
            () -> seatingLayoutTemplateService.deleteTemplate(templateId, userId));
        verify(ownershipService, never()).verifyOwnershipAndGetOrganization(any(), any());
        verify(seatingLayoutTemplateRepository, never()).deleteById(any());
    }

    @Test
    void deleteTemplate_ShouldPropagateException_WhenUserDoesNotHaveAccess() {
        // Given
        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(ownershipService.verifyOwnershipAndGetOrganization(organizationId, userId))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // When/Then
        assertThrows(AuthorizationDeniedException.class, 
            () -> seatingLayoutTemplateService.deleteTemplate(templateId, userId));
        verify(ownershipService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(seatingLayoutTemplateRepository, never()).deleteById(any());
    }

    @Test
    void normalizeLayoutData_ShouldNormalizeCoordinates_AndReplaceIds() {
        // Use reflection to access the private method
        LayoutDataDTO input = sampleLayoutData;
        
        // Create a method that exposes the private method for testing
        LayoutDataDTO result = invokeNormalizeLayoutData(input);

        // Verify coordinates are normalized
        double minX = input.getLayout().getBlocks().stream()
                .mapToDouble(block -> block.getPosition().getX())
                .min()
                .orElse(0.0);
                
        double minY = input.getLayout().getBlocks().stream()
                .mapToDouble(block -> block.getPosition().getY())
                .min()
                .orElse(0.0);
                
        // Check the first block's position is normalized correctly
        assertEquals(
            input.getLayout().getBlocks().getFirst().getPosition().getX() - minX,
            result.getLayout().getBlocks().getFirst().getPosition().getX()
        );
        assertEquals(
            input.getLayout().getBlocks().getFirst().getPosition().getY() - minY,
            result.getLayout().getBlocks().getFirst().getPosition().getY()
        );

        // Verify all blocks have new UUIDs (not datetime-based IDs)
        for (LayoutDataDTO.Block block : result.getLayout().getBlocks()) {
            assertNotNull(block.getId());
            assertEquals(36, block.getId().length()); // UUID length is 36 including hyphens
            // Ensure it doesn't contain the original datetime-based ID prefix
            assertFalse(block.getId().startsWith("blk_"));
        }
        
        // Verify block properties are preserved
        assertEquals(input.getLayout().getBlocks().size(), result.getLayout().getBlocks().size());
        
        // Find and check a seated grid block
        Optional<LayoutDataDTO.Block> seatedGridBlock = result.getLayout().getBlocks().stream()
            .filter(b -> "seated_grid".equals(b.getType()))
            .findFirst();
        assertTrue(seatedGridBlock.isPresent());
        assertEquals(5, seatedGridBlock.get().getRows());
        assertEquals(10, seatedGridBlock.get().getColumns());
        
        // Find and check a standing capacity block
        Optional<LayoutDataDTO.Block> standingBlock = result.getLayout().getBlocks().stream()
            .filter(b -> "standing_capacity".equals(b.getType()))
            .findFirst();
        assertTrue(standingBlock.isPresent());
        assertEquals(100, standingBlock.get().getCapacity());
    }
    
    @Test
    void normalizeLayoutData_ShouldReturnOriginal_WhenInputIsNull() {
        LayoutDataDTO result = invokeNormalizeLayoutData(null);
        assertNull(result);
    }
    
    @Test
    void normalizeLayoutData_ShouldReturnOriginal_WhenLayoutIsNull() {
        LayoutDataDTO input = new LayoutDataDTO();
        LayoutDataDTO result = invokeNormalizeLayoutData(input);
        assertSame(input, result);
    }
    
    @Test
    void normalizeLayoutData_ShouldReturnOriginal_WhenBlocksAreNull() {
        LayoutDataDTO input = new LayoutDataDTO();
        LayoutDataDTO.Layout layout = new LayoutDataDTO.Layout();
        input.setLayout(layout);
        
        LayoutDataDTO result = invokeNormalizeLayoutData(input);
        assertSame(input, result);
    }

    private LayoutDataDTO invokeNormalizeLayoutData(LayoutDataDTO layoutData) {
        return ReflectionTestUtils.invokeMethod(
            seatingLayoutTemplateService, 
            "normalizeLayoutData", 
            layoutData
        );
    }
}
