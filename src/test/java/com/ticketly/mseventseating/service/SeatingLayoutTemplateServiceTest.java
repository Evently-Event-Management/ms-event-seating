package com.ticketly.mseventseating.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.config.AppLimitsConfig;
import com.ticketly.mseventseating.dto.layout_template.LayoutDataDTO;
import com.ticketly.mseventseating.dto.layout_template.SeatingLayoutTemplateDTO;
import com.ticketly.mseventseating.dto.layout_template.SeatingLayoutTemplateRequest;
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
    private SeatingLayoutTemplateOwnershipService templateOwnershipService;

    @Mock
    private LimitService limitService;

    @Mock
    private OrganizationService organizationService;

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
    int DEFAULT_GAP = 25;

    @Mock
    private AppLimitsConfig.SeatingLayoutConfig seatingLayoutConfig;

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
    }

    @Test
    void getAllTemplatesByOrganizationId_ShouldReturnPaginatedTemplates_WhenUserHasAccess() {
        // Given
        int page = 0;
        int size = 10;
        List<SeatingLayoutTemplate> templates = Collections.singletonList(template);
        Page<SeatingLayoutTemplate> templatePage = new PageImpl<>(templates, PageRequest.of(page, size), templates.size());

        when(ownershipService.isOwner(organizationId, userId)).thenReturn(true);
        when(seatingLayoutTemplateRepository.findByOrganizationId(eq(organizationId), any(Pageable.class))).thenReturn(templatePage);

        // When
        Page<SeatingLayoutTemplateDTO> result = seatingLayoutTemplateService.getAllTemplatesByOrganizationId(organizationId, userId, page, size);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getContent().size());
        assertEquals(templateId, result.getContent().getFirst().getId());
        verify(ownershipService).isOwner(organizationId, userId);
    }

    @Test
    void getAllTemplatesByOrganizationId_ShouldThrowException_WhenUserDoesNotHaveAccess() {
        // Given
        when(ownershipService.isOwner(organizationId, userId)).thenReturn(false);

        // When/Then
        assertThrows(AuthorizationDeniedException.class,
                () -> seatingLayoutTemplateService.getAllTemplatesByOrganizationId(organizationId, userId, 0, 10));
        verify(ownershipService).isOwner(organizationId, userId);
        // Verify repository is never called
        verify(seatingLayoutTemplateRepository, never()).findByOrganizationId(any(UUID.class), any(Pageable.class));
    }

    @Test
    void getTemplateById_ShouldReturnTemplate_WhenUserHasAccess() {
        // Given
        when(templateOwnershipService.isOwner(templateId, userId)).thenReturn(true);
        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When
        SeatingLayoutTemplateDTO result = seatingLayoutTemplateService.getTemplateById(templateId, userId);

        // Then
        assertEquals(templateId, result.getId());
        assertEquals("Test Template", result.getName());
        assertEquals(organizationId, result.getOrganizationId());
        verify(templateOwnershipService).isOwner(templateId, userId);
    }

    @Test
    void getTemplateById_ShouldThrowException_WhenTemplateNotFound() {
        // Given
        when(templateOwnershipService.isOwner(templateId, userId)).thenReturn(true);
        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class,
                () -> seatingLayoutTemplateService.getTemplateById(templateId, userId));
        verify(templateOwnershipService).isOwner(templateId, userId);
    }

    @Test
    void getTemplateById_ShouldThrowException_WhenUserDoesNotHaveAccess() {
        // Given
        when(templateOwnershipService.isOwner(templateId, userId)).thenReturn(false);

        // When/Then
        assertThrows(AuthorizationDeniedException.class,
                () -> seatingLayoutTemplateService.getTemplateById(templateId, userId));
        verify(templateOwnershipService).isOwner(templateId, userId);
        verify(seatingLayoutTemplateRepository, never()).findById(any(UUID.class));
    }

    @Test
    void createTemplate_ShouldCreateAndReturnTemplate_WhenUserHasAccessAndUnderLimit() {
        // Given
        SeatingLayoutTemplateRequest request = new SeatingLayoutTemplateRequest();
        request.setName("New Template");
        request.setOrganizationId(organizationId);
        request.setLayoutData(sampleLayoutData);

        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(seatingLayoutTemplateRepository.countByOrganizationId(organizationId)).thenReturn(2L);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_SEATING_LAYOUTS_PER_ORG, mockJwt)).thenReturn(5);
        when(seatingLayoutTemplateRepository.save(any(SeatingLayoutTemplate.class))).thenAnswer(invocation -> {
            SeatingLayoutTemplate savedTemplate = invocation.getArgument(0);
            savedTemplate.setId(UUID.randomUUID());
            return savedTemplate;
        });
        when(limitService.getSeatingLayoutConfig()).thenReturn(seatingLayoutConfig);
        when(seatingLayoutConfig.getDefaultGap()).thenReturn(DEFAULT_GAP);

        // When
        SeatingLayoutTemplateDTO result = seatingLayoutTemplateService.createTemplate(request, userId, mockJwt);

        // Then
        assertNotNull(result);
        assertEquals("New Template", result.getName());
        assertEquals(organizationId, result.getOrganizationId());
        assertNotNull(result.getLayoutData());
        verify(organizationService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(limitService).getTierLimit(SubscriptionLimitType.MAX_SEATING_LAYOUTS_PER_ORG, mockJwt);
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

        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(seatingLayoutTemplateRepository.countByOrganizationId(organizationId)).thenReturn(5L);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_SEATING_LAYOUTS_PER_ORG, mockJwt)).thenReturn(5);

        // When/Then
        assertThrows(BadRequestException.class,
                () -> seatingLayoutTemplateService.createTemplate(request, userId, mockJwt));

        verify(organizationService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(limitService).getTierLimit(SubscriptionLimitType.MAX_SEATING_LAYOUTS_PER_ORG, mockJwt);
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

        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // When/Then
        assertThrows(AuthorizationDeniedException.class,
                () -> seatingLayoutTemplateService.createTemplate(request, userId, mockJwt));
        verify(organizationService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(seatingLayoutTemplateRepository, never()).save(any(SeatingLayoutTemplate.class));
        verify(limitService, never()).getTierLimit(any(), any());
    }

    @Test
    void updateTemplate_ShouldUpdateAndReturnTemplate_WhenUserHasAccess() {
        // Given
        SeatingLayoutTemplateRequest request = new SeatingLayoutTemplateRequest();
        request.setName("Updated Template");
        request.setOrganizationId(organizationId);
        request.setLayoutData(sampleLayoutData);

        when(templateOwnershipService.isOwner(templateId, userId)).thenReturn(true);
        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(seatingLayoutTemplateRepository.save(any(SeatingLayoutTemplate.class))).thenReturn(template);

        when(limitService.getSeatingLayoutConfig()).thenReturn(seatingLayoutConfig);
        when(seatingLayoutConfig.getDefaultGap()).thenReturn(DEFAULT_GAP);

        // When
        SeatingLayoutTemplateDTO result = seatingLayoutTemplateService.updateTemplate(templateId, request, userId);

        // Then
        assertNotNull(result);
        assertEquals("Updated Template", template.getName());
        assertEquals(organizationId, result.getOrganizationId());
        verify(templateOwnershipService).isOwner(templateId, userId);
        verify(seatingLayoutTemplateRepository).save(template);
    }

    @Test
    void updateTemplate_ShouldThrowException_WhenTemplateNotFound() {
        // Given
        SeatingLayoutTemplateRequest request = new SeatingLayoutTemplateRequest();
        request.setName("Updated Template");
        request.setOrganizationId(organizationId);

        when(templateOwnershipService.isOwner(templateId, userId)).thenReturn(true);
        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class,
                () -> seatingLayoutTemplateService.updateTemplate(templateId, request, userId));
        verify(templateOwnershipService).isOwner(templateId, userId);
        verify(seatingLayoutTemplateRepository, never()).save(any(SeatingLayoutTemplate.class));
    }

    @Test
    void updateTemplate_ShouldThrowException_WhenUserDoesNotHaveAccess() {
        // Given
        SeatingLayoutTemplateRequest request = new SeatingLayoutTemplateRequest();
        request.setName("Updated Template");
        request.setOrganizationId(organizationId);

        when(templateOwnershipService.isOwner(templateId, userId)).thenReturn(false);

        // When/Then
        assertThrows(AuthorizationDeniedException.class,
                () -> seatingLayoutTemplateService.updateTemplate(templateId, request, userId));
        verify(templateOwnershipService).isOwner(templateId, userId);
        verify(seatingLayoutTemplateRepository, never()).findById(any());
        verify(seatingLayoutTemplateRepository, never()).save(any(SeatingLayoutTemplate.class));
    }

    @Test
    void deleteTemplate_ShouldDeleteTemplate_WhenUserHasAccess() {
        // Given
        when(templateOwnershipService.isOwner(templateId, userId)).thenReturn(true);
        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When
        seatingLayoutTemplateService.deleteTemplate(templateId, userId);

        // Then
        verify(templateOwnershipService).isOwner(templateId, userId);
        verify(seatingLayoutTemplateRepository).delete(template);
        verify(templateOwnershipService).evictTemplateCacheById(templateId);
    }

    @Test
    void deleteTemplate_ShouldThrowException_WhenTemplateNotFound() {
        // Given
        when(templateOwnershipService.isOwner(templateId, userId)).thenReturn(true);
        when(seatingLayoutTemplateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class,
                () -> seatingLayoutTemplateService.deleteTemplate(templateId, userId));
        verify(templateOwnershipService).isOwner(templateId, userId);
        verify(seatingLayoutTemplateRepository, never()).delete(any());
        verify(templateOwnershipService, never()).evictTemplateCacheById(any());
    }

    @Test
    void deleteTemplate_ShouldThrowException_WhenUserDoesNotHaveAccess() {
        // Given
        when(templateOwnershipService.isOwner(templateId, userId)).thenReturn(false);

        // When/Then
        assertThrows(AuthorizationDeniedException.class,
                () -> seatingLayoutTemplateService.deleteTemplate(templateId, userId));
        verify(templateOwnershipService).isOwner(templateId, userId);
        verify(seatingLayoutTemplateRepository, never()).findById(any());
        verify(seatingLayoutTemplateRepository, never()).delete(any());
        verify(templateOwnershipService, never()).evictTemplateCacheById(any());
    }

    @Test
    void normalizeLayoutData_ShouldNormalizeCoordinates_AndReplaceIds() {
        when(limitService.getSeatingLayoutConfig()).thenReturn(seatingLayoutConfig);
        when(seatingLayoutConfig.getDefaultGap()).thenReturn(DEFAULT_GAP);

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
                input.getLayout().getBlocks().getFirst().getPosition().getX() - minX + DEFAULT_GAP,
                result.getLayout().getBlocks().getFirst().getPosition().getX()
        );
        assertEquals(
                input.getLayout().getBlocks().getFirst().getPosition().getY() - minY + DEFAULT_GAP,
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
