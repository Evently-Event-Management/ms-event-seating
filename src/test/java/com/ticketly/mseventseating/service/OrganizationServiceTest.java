package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.config.AppLimitsConfig;
import com.ticketly.mseventseating.dto.organization.OrganizationRequest;
import com.ticketly.mseventseating.dto.organization.OrganizationResponse;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.SubscriptionLimitType;
import org.springframework.security.authorization.AuthorizationDeniedException;
import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private OrganizationOwnershipService ownershipService;

    @Mock
    private LimitService limitService;

    @Mock
    private AppLimitsConfig.OrganizationConfig organizationConfig;

    @InjectMocks
    private OrganizationService organizationService;

    private final String USER_ID = "test-user-id";
    private final UUID ORG_ID = UUID.randomUUID();
    private final String ORG_NAME = "Test Organization";
    private final String ORG_WEBSITE = "https://test-org.com";
    private final String LOGO_URL = "organization-logos/logo-uuid.jpg";
    private final String PRESIGNED_URL = "https://bucket.s3.amazonaws.com/organization-logos/logo-uuid.jpg";

    private Organization organization;
    private Jwt jwt;
    private OrganizationRequest organizationRequest;

    @BeforeEach
    void setUp() {
        organization = Organization.builder()
                .id(ORG_ID)
                .name(ORG_NAME)
                .website(ORG_WEBSITE)
                .userId(USER_ID)
                .logoUrl(LOGO_URL)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now())
                .build();

        // Set up JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", USER_ID);

        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claims(c -> c.putAll(claims))
                .build();

        organizationRequest = new OrganizationRequest(ORG_NAME, ORG_WEBSITE);

    }

    @Test
    void getAllOrganizationsForUser_shouldReturnUserOrganizations() {
        // Arrange
        Organization org2 = Organization.builder()
                .id(UUID.randomUUID())
                .name("Second Org")
                .userId(USER_ID)
                .build();

        List<Organization> organizations = Arrays.asList(organization, org2);
        when(organizationRepository.findByUserId(USER_ID)).thenReturn(organizations);
        when(s3StorageService.generatePresignedUrl(LOGO_URL, 60)).thenReturn(PRESIGNED_URL);

        // Act
        List<OrganizationResponse> result = organizationService.getAllOrganizationsForUser(USER_ID);

        // Assert
        assertEquals(2, result.size());
        assertEquals(ORG_ID, result.getFirst().getId());
        assertEquals(ORG_NAME, result.get(0).getName());
        assertEquals(PRESIGNED_URL, result.get(0).getLogoUrl());
        assertEquals("Second Org", result.get(1).getName());
        verify(organizationRepository).findByUserId(USER_ID);
    }

    @Test
    void getOrganizationById_whenUserIsOwner_shouldReturnOrganizationAndUser() {
        // Arrange
        when(ownershipService.isOwner(ORG_ID, USER_ID)).thenReturn(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(s3StorageService.generatePresignedUrl(LOGO_URL, 60)).thenReturn(PRESIGNED_URL);

        // Act
        OrganizationResponse result = organizationService.getOrganizationByIdOwner(ORG_ID, USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(ORG_ID, result.getId());
        assertEquals(ORG_NAME, result.getName());
        assertEquals(ORG_WEBSITE, result.getWebsite());
        assertEquals(PRESIGNED_URL, result.getLogoUrl());
        verify(ownershipService).isOwner(ORG_ID, USER_ID);
    }

    @Test
    void getOrganizationById_AndUser_whenUserIsNotOwner_shouldThrowAuthorizationDeniedException() {
        // Arrange
        when(ownershipService.isOwner(ORG_ID, USER_ID)).thenReturn(false);

        // Act & Assert
        assertThrows(AuthorizationDeniedException.class, () ->
                organizationService.getOrganizationByIdOwner(ORG_ID, USER_ID));

        verify(ownershipService).isOwner(ORG_ID, USER_ID);
        verifyNoMoreInteractions(organizationRepository);
    }

    @Test
    void createOrganization_shouldCreateAndReturnOrganization() {
        // Arrange
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt)).thenReturn(5);
        when(organizationRepository.countByUserId(USER_ID)).thenReturn(2L);

        when(organizationRepository.save(any(Organization.class))).thenReturn(organization);

        // Act
        OrganizationResponse result = organizationService.createOrganization(organizationRequest, USER_ID, jwt);

        // Assert
        assertNotNull(result);
        assertEquals(ORG_ID, result.getId());
        assertEquals(ORG_NAME, result.getName());

        ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(orgCaptor.capture());

        Organization capturedOrg = orgCaptor.getValue();
        assertEquals(ORG_NAME, capturedOrg.getName());
        assertEquals(ORG_WEBSITE, capturedOrg.getWebsite());
        assertEquals(USER_ID, capturedOrg.getUserId());
    }

    @Test
    void createOrganization_whenExceedingLimit_shouldThrowBadRequestException() {
        // Arrange
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, jwt)).thenReturn(3);
        when(organizationRepository.countByUserId(USER_ID)).thenReturn(3L);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                organizationService.createOrganization(organizationRequest, USER_ID, jwt));

        assertTrue(exception.getMessage().contains("maximum limit of 3 organizations"));
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void updateOrganization_whenUserIsOwner_shouldUpdateAndReturnOrganization() {
        // Arrange
        when(ownershipService.isOwner(ORG_ID, USER_ID)).thenReturn(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any(Organization.class))).thenReturn(organization);
        when(s3StorageService.generatePresignedUrl(LOGO_URL, 60)).thenReturn(PRESIGNED_URL);

        String newName = "Updated Organization";
        String newWebsite = "https://updated-org.com";
        OrganizationRequest updateRequest = new OrganizationRequest(newName, newWebsite);

        // Act
        OrganizationResponse result = organizationService.updateOrganization(ORG_ID, updateRequest, USER_ID);

        // Assert
        assertNotNull(result);

        ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(ownershipService).isOwner(ORG_ID, USER_ID);
        verify(organizationRepository).save(orgCaptor.capture());

        Organization updatedOrg = orgCaptor.getValue();
        assertEquals(newName, updatedOrg.getName());
        assertEquals(newWebsite, updatedOrg.getWebsite());
    }

    @Test
    void updateOrganization_whenUserIsNotOwner_shouldThrowAuthorizationDeniedException() {
        // Arrange
        when(ownershipService.isOwner(ORG_ID, USER_ID)).thenReturn(false);

        // Act & Assert
        assertThrows(AuthorizationDeniedException.class, () ->
                organizationService.updateOrganization(ORG_ID, organizationRequest, USER_ID));

        verify(ownershipService).isOwner(ORG_ID, USER_ID);
        verifyNoInteractions(s3StorageService);
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void uploadLogo_whenUserIsOwnerAndValidFile_shouldUpdateLogo() throws IOException {
        // Arrange
        when(ownershipService.isOwner(ORG_ID, USER_ID)).thenReturn(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));

        MockMultipartFile logoFile = new MockMultipartFile(
                "logo", "logo.jpg", "image/jpeg", "test image content".getBytes());

        when(s3StorageService.uploadFile(logoFile, "organization-logos")).thenReturn("new-logo-url.jpg");
        when(organizationRepository.save(any(Organization.class))).thenReturn(organization);
        when(s3StorageService.generatePresignedUrl(any(), eq(60))).thenReturn(PRESIGNED_URL);
        when(limitService.getOrganizationConfig()).thenReturn(organizationConfig);
        when(organizationConfig.getMaxLogoSize()).thenReturn(5 * 1024 * 1024L); // 5 MB

        // Act
        OrganizationResponse result = organizationService.uploadLogo(ORG_ID, logoFile, USER_ID);

        // Assert
        assertNotNull(result);

        verify(ownershipService).isOwner(ORG_ID, USER_ID);
        verify(s3StorageService).deleteFile(LOGO_URL); // Should delete old logo
        verify(s3StorageService).uploadFile(logoFile, "organization-logos");
        verify(limitService).getOrganizationConfig(); // Verify the method was called

        ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(orgCaptor.capture());
        assertEquals("new-logo-url.jpg", orgCaptor.getValue().getLogoUrl());
    }

    @Test
    void removeLogo_whenUserIsOwner_shouldRemoveLogo() {
        // Arrange
        when(ownershipService.isOwner(ORG_ID, USER_ID)).thenReturn(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));

        // Act
        organizationService.removeLogo(ORG_ID, USER_ID);

        // Assert
        verify(ownershipService).isOwner(ORG_ID, USER_ID);
        verify(s3StorageService).deleteFile(LOGO_URL);

        ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(orgCaptor.capture());
        assertNull(orgCaptor.getValue().getLogoUrl());
    }

    @Test
    void deleteOrganization_whenUserIsOwner_shouldDeleteOrganization() {
        // Arrange
        when(ownershipService.isOwner(ORG_ID, USER_ID)).thenReturn(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));

        // Act
        organizationService.deleteOrganization(ORG_ID, USER_ID);

        // Assert
        verify(ownershipService).isOwner(ORG_ID, USER_ID);
        verify(s3StorageService).deleteFile(LOGO_URL);
        verify(organizationRepository).delete(organization);
        verify(ownershipService).evictOrganizationCacheById(ORG_ID);
    }

    @Test
    void deleteOrganization_whenUserIsNotOwner_shouldThrowAuthorizationDeniedException() {
        // Arrange
        when(ownershipService.isOwner(ORG_ID, USER_ID)).thenReturn(false);

        // Act & Assert
        assertThrows(AuthorizationDeniedException.class, () ->
                organizationService.deleteOrganization(ORG_ID, USER_ID));

        verify(ownershipService).isOwner(ORG_ID, USER_ID);
        verifyNoInteractions(s3StorageService);
        verify(organizationRepository, never()).delete(any());
    }

    @Test
    void getOrganizationById_adminAccess_shouldReturnOrganization() {
        // Arrange
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(s3StorageService.generatePresignedUrl(LOGO_URL, 60)).thenReturn(PRESIGNED_URL);

        // Act
        OrganizationResponse result = organizationService.getOrganizationById(ORG_ID);

        // Assert
        assertNotNull(result);
        assertEquals(ORG_ID, result.getId());
        assertEquals(ORG_NAME, result.getName());
        assertEquals(ORG_WEBSITE, result.getWebsite());
        assertEquals(PRESIGNED_URL, result.getLogoUrl());

        // Verify repository was called but not ownership service (admin access)
        verify(organizationRepository).findById(ORG_ID);
        verifyNoInteractions(ownershipService);
    }

    @Test
    void getOrganizationById_whenNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> organizationService.getOrganizationById(ORG_ID));

        assertTrue(exception.getMessage().contains("Organization not found with id"));
        verify(organizationRepository).findById(ORG_ID);
    }

    @Test
    void verifyOwnershipAndGetOrganization_whenUserIsOwner_shouldReturnOrganization() {
        // Arrange
        when(ownershipService.isOwner(ORG_ID, USER_ID)).thenReturn(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));

        // Act
        Organization result = organizationService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(ORG_ID, result.getId());
        assertEquals(ORG_NAME, result.getName());
        assertEquals(USER_ID, result.getUserId());

        verify(ownershipService).isOwner(ORG_ID, USER_ID);
        verify(organizationRepository).findById(ORG_ID);
    }

    @Test
    void verifyOwnershipAndGetOrganization_whenUserIsNotOwner_shouldThrowAuthorizationDeniedException() {
        // Arrange
        when(ownershipService.isOwner(ORG_ID, USER_ID)).thenReturn(false);

        // Act & Assert
        AuthorizationDeniedException exception = assertThrows(AuthorizationDeniedException.class,
                () -> organizationService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID));

        assertTrue(exception.getMessage().contains("User does not have access"));
        verify(ownershipService).isOwner(ORG_ID, USER_ID);
        verifyNoInteractions(organizationRepository);
    }

    @Test
    void verifyOwnershipAndGetOrganization_whenOrganizationNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(ownershipService.isOwner(ORG_ID, USER_ID)).thenReturn(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> organizationService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID));

        assertTrue(exception.getMessage().contains("Organization not found"));
        verify(ownershipService).isOwner(ORG_ID, USER_ID);
        verify(organizationRepository).findById(ORG_ID);
    }

    @Test
    void uploadLogo_withInvalidFileType_shouldThrowBadRequestException() {
        // Arrange

        MockMultipartFile invalidFile = new MockMultipartFile(
                "document", "document.pdf", "application/pdf", "test document content".getBytes());

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> organizationService.uploadLogo(ORG_ID, invalidFile, USER_ID));

        assertTrue(exception.getMessage().contains("Invalid file type"));
        verify(ownershipService, never()).isOwner(any(), any());
        verify(organizationRepository, never()).findById(any());
        verifyNoMoreInteractions(s3StorageService);
    }

    @Test
    void uploadLogo_withExceededFileSize_shouldThrowBadRequestException() {
        // Arrange
        when(limitService.getOrganizationConfig()).thenReturn(organizationConfig);
        when(organizationConfig.getMaxLogoSize()).thenReturn(10L); // Very small limit

        MockMultipartFile largeFile = new MockMultipartFile(
                "logo", "logo.jpg", "image/jpeg", "test image content".getBytes());

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> organizationService.uploadLogo(ORG_ID, largeFile, USER_ID));

        assertTrue(exception.getMessage().contains("File size exceeds"));
        verify(limitService).getOrganizationConfig();
        verifyNoMoreInteractions(s3StorageService);
        verify(ownershipService, never()).isOwner(any(), any());
    }
}
