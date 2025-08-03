package com.ticketly.mseventseating.service;

import com.ticketly.mseventseating.dto.OrganizationRequest;
import com.ticketly.mseventseating.dto.OrganizationResponse;
import com.ticketly.mseventseating.exception.BadRequestException;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

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
    private SubscriptionTierService subscriptionTierService;

    @InjectMocks
    private OrganizationService organizationService;

    private final String USER_ID = "test-user-id";
    private final UUID ORG_ID = UUID.randomUUID();
    private final String ORG_NAME = "Test Organization";
    private final String ORG_WEBSITE = "https://test-org.com";
    private final String LOGO_URL = "organization-logos/logo-uuid.jpg";
    private final String PRESIGNED_URL = "https://bucket.s3.amazonaws.com/organization-logos/logo-uuid.jpg";

    private Organization testOrganization;
    private OrganizationRequest testRequest;
    private Jwt mockJwt;

    @BeforeEach
    void setUp() {
        // Configure service properties
        ReflectionTestUtils.setField(organizationService, "maxLogoSize", 2 * 1024 * 1024); // 2MB

        // Set up test data
        testOrganization = Organization.builder()
                .id(ORG_ID)
                .name(ORG_NAME)
                .website(ORG_WEBSITE)
                .userId(USER_ID)
                .logoUrl(LOGO_URL)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        testRequest = OrganizationRequest.builder()
                .name(ORG_NAME)
                .website(ORG_WEBSITE)
                .build();

        // Mock JWT
        mockJwt = mock(Jwt.class);
    }

    @Test
    void getAllOrganizationsForUser_ShouldReturnListOfOrganizations() {
        // Arrange
        List<Organization> organizations = Collections.singletonList(testOrganization);
        when(organizationRepository.findByUserId(USER_ID)).thenReturn(organizations);
        when(s3StorageService.generatePresignedUrl(LOGO_URL, 60)).thenReturn(PRESIGNED_URL);

        // Act
        List<OrganizationResponse> result = organizationService.getAllOrganizationsForUser(USER_ID);

        // Assert
        assertEquals(1, result.size());
        assertEquals(ORG_ID, result.getFirst().getId());
        assertEquals(ORG_NAME, result.getFirst().getName());
        assertEquals(ORG_WEBSITE, result.getFirst().getWebsite());
        assertEquals(PRESIGNED_URL, result.getFirst().getLogoUrl());

        verify(organizationRepository).findByUserId(USER_ID);
        verify(s3StorageService).generatePresignedUrl(LOGO_URL, 60);
    }

    @Test
    void getOrganizationById_ShouldReturnOrganization_WhenFound() {
        // Arrange
        when(ownershipService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID)).thenReturn(testOrganization);
        when(s3StorageService.generatePresignedUrl(LOGO_URL, 60)).thenReturn(PRESIGNED_URL);

        // Act
        OrganizationResponse result = organizationService.getOrganizationById(ORG_ID, USER_ID);

        // Assert
        assertEquals(ORG_ID, result.getId());
        assertEquals(ORG_NAME, result.getName());
        assertEquals(PRESIGNED_URL, result.getLogoUrl());

        verify(ownershipService).verifyOwnershipAndGetOrganization(ORG_ID, USER_ID);
    }

    @Test
    void getOrganizationById_ShouldPropagateException_WhenNotOwned() {
        // Arrange
        when(ownershipService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // Act & Assert
        assertThrows(AuthorizationDeniedException.class, () ->
                organizationService.getOrganizationById(ORG_ID, USER_ID)
        );

        verify(ownershipService).verifyOwnershipAndGetOrganization(ORG_ID, USER_ID);
    }

    @Test
    void createOrganization_ShouldCreateAndReturnOrganization() {
        // Arrange
        Organization savedOrganization = testOrganization;
        when(organizationRepository.countByUserId(USER_ID)).thenReturn(2L);
        when(subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, mockJwt)).thenReturn(3);
        when(organizationRepository.save(any(Organization.class))).thenReturn(savedOrganization);
        when(s3StorageService.generatePresignedUrl(LOGO_URL, 60)).thenReturn(PRESIGNED_URL);

        // Act
        OrganizationResponse result = organizationService.createOrganization(testRequest, USER_ID, mockJwt);

        // Assert
        assertEquals(ORG_NAME, result.getName());
        assertEquals(ORG_WEBSITE, result.getWebsite());

        ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(organizationCaptor.capture());

        Organization capturedOrg = organizationCaptor.getValue();
        assertEquals(ORG_NAME, capturedOrg.getName());
        assertEquals(ORG_WEBSITE, capturedOrg.getWebsite());
        assertEquals(USER_ID, capturedOrg.getUserId());
    }

    @Test
    void createOrganization_ShouldThrowException_WhenUserExceedsLimit() {
        // Arrange
        when(organizationRepository.countByUserId(USER_ID)).thenReturn(3L);
        when(subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, mockJwt)).thenReturn(3);

        // Act & Assert
        assertThrows(BadRequestException.class, () ->
                organizationService.createOrganization(testRequest, USER_ID, mockJwt)
        );

        verify(organizationRepository).countByUserId(USER_ID);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void createOrganization_ShouldAllowDifferentLimitsBasedOnTier() {
        // Arrange
        Organization savedOrganization = testOrganization;
        when(organizationRepository.countByUserId(USER_ID)).thenReturn(5L);
        when(subscriptionTierService.getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, mockJwt)).thenReturn(10); // Higher tier limit
        when(organizationRepository.save(any(Organization.class))).thenReturn(savedOrganization);
        when(s3StorageService.generatePresignedUrl(LOGO_URL, 60)).thenReturn(PRESIGNED_URL);

        // Act
        OrganizationResponse result = organizationService.createOrganization(testRequest, USER_ID, mockJwt);

        // Assert
        assertEquals(ORG_NAME, result.getName());
        assertEquals(ORG_WEBSITE, result.getWebsite());

        // Verify the tier service was called
        verify(subscriptionTierService).getLimit(SubscriptionLimitType.MAX_ORGANIZATIONS_PER_USER, mockJwt);
    }

    @Test
    void updateOrganization_ShouldUpdateAndReturnOrganization() {
        // Arrange
        String updatedName = "Updated Organization";
        String updatedWebsite = "https://updated-org.com";

        OrganizationRequest updateRequest = OrganizationRequest.builder()
                .name(updatedName)
                .website(updatedWebsite)
                .build();

        when(ownershipService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID)).thenReturn(testOrganization);
        when(organizationRepository.save(any(Organization.class))).thenReturn(
                Organization.builder()
                        .id(ORG_ID)
                        .name(updatedName)
                        .website(updatedWebsite)
                        .userId(USER_ID)
                        .logoUrl(LOGO_URL)
                        .createdAt(OffsetDateTime.now())
                        .updatedAt(OffsetDateTime.now())
                        .build()
        );
        when(s3StorageService.generatePresignedUrl(LOGO_URL, 60)).thenReturn(PRESIGNED_URL);

        // Act
        OrganizationResponse result = organizationService.updateOrganization(ORG_ID, updateRequest, USER_ID);

        // Assert
        assertEquals(updatedName, result.getName());
        assertEquals(updatedWebsite, result.getWebsite());

        verify(ownershipService).verifyOwnershipAndGetOrganization(ORG_ID, USER_ID);

        ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(organizationCaptor.capture());

        Organization capturedOrg = organizationCaptor.getValue();
        assertEquals(updatedName, capturedOrg.getName());
        assertEquals(updatedWebsite, capturedOrg.getWebsite());
    }

    @Test
    void uploadLogo_ShouldUploadAndUpdateOrganization() throws IOException {
        // Arrange
        MockMultipartFile logoFile = new MockMultipartFile(
                "logo",
                "logo.jpg",
                "image/jpeg",
                "test logo content".getBytes()
        );

        when(ownershipService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID)).thenReturn(testOrganization);
        when(s3StorageService.uploadFile(any(MultipartFile.class), eq("organization-logos"))).thenReturn(LOGO_URL);
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);
        when(s3StorageService.generatePresignedUrl(LOGO_URL, 60)).thenReturn(PRESIGNED_URL);

        // Act
        OrganizationResponse result = organizationService.uploadLogo(ORG_ID, logoFile, USER_ID);

        // Assert
        assertEquals(PRESIGNED_URL, result.getLogoUrl());

        verify(ownershipService).verifyOwnershipAndGetOrganization(ORG_ID, USER_ID);
        verify(s3StorageService).uploadFile(any(MultipartFile.class), eq("organization-logos"));
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void uploadLogo_ShouldDeleteOldLogoIfExists() throws IOException {
        // Arrange
        String oldLogoUrl = "organization-logos/old-logo.jpg";
        testOrganization.setLogoUrl(oldLogoUrl);

        MockMultipartFile logoFile = new MockMultipartFile(
                "logo",
                "logo.jpg",
                "image/jpeg",
                "test logo content".getBytes()
        );

        when(ownershipService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID)).thenReturn(testOrganization);
        when(s3StorageService.uploadFile(any(MultipartFile.class), eq("organization-logos"))).thenReturn(LOGO_URL);
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);
        when(s3StorageService.generatePresignedUrl(LOGO_URL, 60)).thenReturn(PRESIGNED_URL);

        // Act
        organizationService.uploadLogo(ORG_ID, logoFile, USER_ID);

        // Assert
        verify(ownershipService).verifyOwnershipAndGetOrganization(ORG_ID, USER_ID);
        verify(s3StorageService).deleteFile(oldLogoUrl);
    }

    @Test
    void uploadLogo_ShouldThrowException_WhenFileTypeIsInvalid() throws IOException {
        // Arrange
        MockMultipartFile logoFile = new MockMultipartFile(
                "logo",
                "logo.txt",
                "text/plain",
                "not an image".getBytes()
        );

        // Act & Assert
        assertThrows(BadRequestException.class, () ->
                organizationService.uploadLogo(ORG_ID, logoFile, USER_ID)
        );

        verify(s3StorageService, never()).uploadFile(any(), anyString());
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void uploadLogo_ShouldThrowException_WhenFileSizeExceedsLimit() throws IOException {
        // Arrange
        MockMultipartFile logoFile = new MockMultipartFile(
                "logo",
                "logo.jpg",
                "image/jpeg",
                new byte[3 * 1024 * 1024] // 3MB
        );

        // Act & Assert
        assertThrows(BadRequestException.class, () ->
                organizationService.uploadLogo(ORG_ID, logoFile, USER_ID)
        );

        verify(s3StorageService, never()).uploadFile(any(), anyString());
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void deleteOrganization_ShouldDeleteOrganization() {
        // Arrange
        when(ownershipService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID)).thenReturn(testOrganization);
        doNothing().when(organizationRepository).delete(any(Organization.class));

        // Act
        organizationService.deleteOrganization(ORG_ID, USER_ID);

        // Assert
        verify(ownershipService).verifyOwnershipAndGetOrganization(ORG_ID, USER_ID);
        verify(s3StorageService).deleteFile(LOGO_URL);
        verify(organizationRepository).delete(testOrganization);
    }

    @Test
    void deleteOrganization_ShouldNotDeleteLogoIfNotExists() {
        // Arrange
        Organization orgWithoutLogo = testOrganization.toBuilder().logoUrl(null).build();
        when(ownershipService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID)).thenReturn(orgWithoutLogo);
        doNothing().when(organizationRepository).delete(any(Organization.class));

        // Act
        organizationService.deleteOrganization(ORG_ID, USER_ID);

        // Assert
        verify(ownershipService).verifyOwnershipAndGetOrganization(ORG_ID, USER_ID);
        verify(s3StorageService, never()).deleteFile(anyString());
        verify(organizationRepository).delete(orgWithoutLogo);
    }

    @Test
    void removeLogo_ShouldDeleteLogoAndUpdateOrganization() {
        // Arrange
        when(ownershipService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID)).thenReturn(testOrganization);
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

        // Act
        organizationService.removeLogo(ORG_ID, USER_ID);

        // Assert
        verify(ownershipService).verifyOwnershipAndGetOrganization(ORG_ID, USER_ID);
        verify(s3StorageService).deleteFile(LOGO_URL);

        ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(organizationCaptor.capture());

        Organization capturedOrg = organizationCaptor.getValue();
        assertNull(capturedOrg.getLogoUrl());
    }

    @Test
    void removeLogo_ShouldNotCallS3Delete_WhenLogoDoesNotExist() {
        // Arrange
        Organization orgWithoutLogo = testOrganization.toBuilder().logoUrl(null).build();
        when(ownershipService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID)).thenReturn(orgWithoutLogo);

        // Act
        organizationService.removeLogo(ORG_ID, USER_ID);

        // Assert
        verify(ownershipService).verifyOwnershipAndGetOrganization(ORG_ID, USER_ID);
        verify(s3StorageService, never()).deleteFile(anyString());
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void removeLogo_ShouldPropagateException_WhenUserIsNotOwner() {
        // Arrange
        when(ownershipService.verifyOwnershipAndGetOrganization(ORG_ID, USER_ID))
                .thenThrow(new AuthorizationDeniedException("User does not have access to this organization"));

        // Act & Assert
        assertThrows(AuthorizationDeniedException.class, () ->
                organizationService.removeLogo(ORG_ID, USER_ID)
        );

        verify(ownershipService).verifyOwnershipAndGetOrganization(ORG_ID, USER_ID);
        verify(s3StorageService, never()).deleteFile(anyString());
        verify(organizationRepository, never()).save(any());
    }
}
