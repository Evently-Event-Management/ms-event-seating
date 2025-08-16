package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.config.AppLimitsConfig;
import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.event.EventResponseDTO;
import com.ticketly.mseventseating.dto.event.SessionRequest;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.factory.EventFactory;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.LimitService;
import com.ticketly.mseventseating.service.OrganizationService;
import com.ticketly.mseventseating.service.S3StorageService;
import model.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCreationServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private LimitService limitService;

    @Mock
    private EventFactory eventFactory;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private AppLimitsConfig.EventConfig eventConfig;


    @InjectMocks
    private EventCreationService eventCreationService;

    private UUID organizationId;
    private String userId;
    private Organization organization;
    private Event event;
    private CreateEventRequest createEventRequest;
    private Jwt jwt;
    private MockMultipartFile validImageFile;
    private MockMultipartFile invalidImageFile;
    private MockMultipartFile largeImageFile;

    @BeforeEach
    void setUp() {
        // Initialize common test data
        organizationId = UUID.randomUUID();
        userId = "user123";

        // Setup organization
        organization = Organization.builder()
                .id(organizationId)
                .name("Test Organization")
                .build();

        // Setup event
        event = Event.builder()
                .id(UUID.randomUUID())
                .title("Test Event")
                .description("Test Description")
                .overview("Test Overview")
                .status(EventStatus.PENDING)
                .organization(organization)
                .createdAt(OffsetDateTime.now())
                .build();

        // Add empty coverPhotos list to properly initialize for tests
        event.setCoverPhotos(new ArrayList<>());

        // Setup sessions for the request
        List<SessionRequest> sessions = Collections.singletonList(
                SessionRequest.builder()
                        .startTime(OffsetDateTime.now().plusDays(10))
                        .endTime(OffsetDateTime.now().plusDays(10).plusHours(3))
                        .salesStartRuleType(SalesStartRuleType.FIXED)
                        .salesStartFixedDatetime(OffsetDateTime.now().plusDays(1))
                        .build()
        );

        // Setup create event request with empty tiers list to prevent NPE
        createEventRequest = CreateEventRequest.builder()
                .title("Test Event")
                .description("Test Description")
                .overview("Test Overview")
                .organizationId(organizationId)
                .sessions(sessions)
                .tiers(new ArrayList<>()) // Initialize with empty list to prevent NullPointerException
                .build();

        // Setup JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userId);

        jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claims(c -> c.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // Setup test files
        validImageFile = new MockMultipartFile(
                "coverImages",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        invalidImageFile = new MockMultipartFile(
                "coverImages",
                "test-document.pdf",
                "application/pdf",
                "test document content".getBytes()
        );

        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
        Arrays.fill(largeContent, (byte) 1);
        largeImageFile = new MockMultipartFile(
                "coverImages",
                "large-image.jpg",
                "image/jpeg",
                largeContent
        );

        // Setup app config mocks
//        when(limitService.getAppConfiguration()).thenReturn(appConfig);
//        when(appConfig.getEventLimits()).thenReturn(eventConfig);
//        when(eventConfig.getMaxCoverPhotos()).thenReturn(3);
    }

    @Test
    void createEvent_Success() throws IOException {
        // Arrange
        MultipartFile[] coverImages = {validImageFile};
        List<String> uploadedKeys = Collections.singletonList("s3-key-1.jpg");

        // Prepare expected EventCoverPhoto entities
        List<EventCoverPhoto> expectedCoverPhotos = uploadedKeys.stream()
            .map(key -> EventCoverPhoto.builder()
                .photoUrl(key)
                .event(event)
                .build())
            .collect(Collectors.toList());

        event.setCoverPhotos(expectedCoverPhotos);

        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(10);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt)).thenReturn(5);
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);
        when(s3StorageService.uploadFile(any(MultipartFile.class), eq("event-cover-photos"))).thenReturn("s3-key-1.jpg");
        when(eventFactory.createFromRequest(eq(createEventRequest), eq(organization), eq(uploadedKeys))).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);
        when(limitService.getEventConfig()).thenReturn(eventConfig);
        when(eventConfig.getMaxCoverPhotos()).thenReturn(3);
        when(eventConfig.getMaxCoverPhotoSize()).thenReturn(5L * 1024 * 1024); // 5MB

        // Act
        EventResponseDTO response = eventCreationService.createEvent(createEventRequest, coverImages, userId, jwt);

        // Assert
        assertNotNull(response);
        assertEquals(event.getId(), response.getId());
        assertEquals(event.getTitle(), response.getTitle());
        assertEquals(event.getStatus().name(), response.getStatus());
        assertEquals(organization.getId(), response.getOrganizationId());

        // Verify
        verify(organizationService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(limitService).getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);
        verify(limitService).getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);
        verify(eventRepository).countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED);
        verify(s3StorageService).uploadFile(any(MultipartFile.class), eq("event-cover-photos"));
        verify(eventFactory).createFromRequest(createEventRequest, organization, uploadedKeys);
        verify(eventRepository).save(event);
    }

    @Test
    void createEvent_WithNullCoverImages_Success() {
        // Arrange
        List<String> emptyKeys = Collections.emptyList();

        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(10);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt)).thenReturn(5);
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);
        when(eventFactory.createFromRequest(eq(createEventRequest), eq(organization), eq(emptyKeys))).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);

        // Act
        EventResponseDTO response = eventCreationService.createEvent(createEventRequest, null, userId, jwt);

        // Assert
        assertNotNull(response);
        assertEquals(event.getId(), response.getId());

        // Verify
        verify(organizationService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(eventFactory).createFromRequest(createEventRequest, organization, emptyKeys);
        verify(eventRepository).save(event);
        verifyNoInteractions(s3StorageService);
    }

    @Test
    void createEvent_WithEmptyCoverImagesArray_Success() {
        // Arrange
        MultipartFile[] emptyCoverImages = new MultipartFile[0];
        List<String> emptyKeys = Collections.emptyList();

        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(10);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt)).thenReturn(5);
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);
        when(eventFactory.createFromRequest(eq(createEventRequest), eq(organization), eq(emptyKeys))).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);

        // Act
        EventResponseDTO response = eventCreationService.createEvent(createEventRequest, emptyCoverImages, userId, jwt);

        // Assert
        assertNotNull(response);
        assertEquals(event.getId(), response.getId());

        // Verify
        verify(organizationService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(eventFactory).createFromRequest(createEventRequest, organization, emptyKeys);
        verify(eventRepository).save(event);
        verifyNoInteractions(s3StorageService);
    }

    @Test
    void createEvent_MultipleCoverImages_Success() throws IOException {
        // Arrange
        MockMultipartFile image1 = new MockMultipartFile("coverImages", "image1.jpg", "image/jpeg", "image1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("coverImages", "image2.jpg", "image/jpeg", "image2".getBytes());
        MultipartFile[] coverImages = {image1, image2};
        List<String> uploadedKeys = Arrays.asList("s3-key-1.jpg", "s3-key-2.jpg");

        // Prepare expected EventCoverPhoto entities
        List<EventCoverPhoto> expectedCoverPhotos = uploadedKeys.stream()
            .map(key -> EventCoverPhoto.builder()
                .photoUrl(key)
                .event(event)
                .build())
            .collect(Collectors.toList());

        event.setCoverPhotos(expectedCoverPhotos);

        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(10);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt)).thenReturn(5);
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);
        when(s3StorageService.uploadFile(image1, "event-cover-photos")).thenReturn("s3-key-1.jpg");
        when(s3StorageService.uploadFile(image2, "event-cover-photos")).thenReturn("s3-key-2.jpg");
        when(eventFactory.createFromRequest(eq(createEventRequest), eq(organization), eq(uploadedKeys))).thenReturn(event);
        when(eventRepository.save(event)).thenReturn(event);
        when(limitService.getEventConfig()).thenReturn(eventConfig);
        when(eventConfig.getMaxCoverPhotos()).thenReturn(3);
        when(eventConfig.getMaxCoverPhotoSize()).thenReturn(5L * 1024 * 1024); // 5MB

        // Act
        EventResponseDTO response = eventCreationService.createEvent(createEventRequest, coverImages, userId, jwt);

        // Assert
        assertNotNull(response);
        assertEquals(event.getId(), response.getId());
        assertEquals(2, event.getCoverPhotos().size());
        assertEquals("s3-key-1.jpg", event.getCoverPhotos().get(0).getPhotoUrl());
        assertEquals("s3-key-2.jpg", event.getCoverPhotos().get(1).getPhotoUrl());

        // Verify
        verify(s3StorageService).uploadFile(image1, "event-cover-photos");
        verify(s3StorageService).uploadFile(image2, "event-cover-photos");
        verify(eventFactory).createFromRequest(createEventRequest, organization, uploadedKeys);
    }

    @Test
    void createEvent_ExceedsActiveEventLimit_ThrowsBadRequestException() {
        // Arrange
        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(5);
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                eventCreationService.createEvent(createEventRequest, null, userId, jwt)
        );

        assertTrue(exception.getMessage().contains("You have reached the limit of 5 active events"));

        // Verify
        verify(organizationService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(limitService).getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);
        verify(eventRepository).countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED);
        verifyNoInteractions(eventFactory);
        verifyNoInteractions(s3StorageService);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_ExceedsSessionsPerEventLimit_ThrowsBadRequestException() {
        // Arrange
        // Create a request with too many sessions
        List<SessionRequest> manySessions = Arrays.asList(
                SessionRequest.builder().startTime(OffsetDateTime.now().plusDays(1)).endTime(OffsetDateTime.now().plusDays(1).plusHours(2)).build(),
                SessionRequest.builder().startTime(OffsetDateTime.now().plusDays(2)).endTime(OffsetDateTime.now().plusDays(2).plusHours(2)).build(),
                SessionRequest.builder().startTime(OffsetDateTime.now().plusDays(3)).endTime(OffsetDateTime.now().plusDays(3).plusHours(2)).build()
        );

        CreateEventRequest requestWithManySessions = CreateEventRequest.builder()
                .title("Test Event")
                .description("Test Description")
                .organizationId(organizationId)
                .sessions(manySessions)
                .tiers(new ArrayList<>()) // Initialize empty tiers list to avoid NullPointerException
                .build();

        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(10);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt)).thenReturn(2); // Limit is 2, but we're trying to create 3
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                eventCreationService.createEvent(requestWithManySessions, null, userId, jwt)
        );

        assertTrue(exception.getMessage().contains("You cannot create more than 2 sessions per event"));

        // Verify
        verify(organizationService).verifyOwnershipAndGetOrganization(organizationId, userId);
        verify(limitService).getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt);
        verify(limitService).getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt);
        verify(eventRepository).countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED);
        verifyNoInteractions(eventFactory);
        verifyNoInteractions(s3StorageService);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_ExceedsMaxCoverPhotos_ThrowsBadRequestException() {
        // Arrange
        MockMultipartFile image1 = new MockMultipartFile("coverImages", "image1.jpg", "image/jpeg", "content1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("coverImages", "image2.jpg", "image/jpeg", "content2".getBytes());
        MockMultipartFile image3 = new MockMultipartFile("coverImages", "image3.jpg", "image/jpeg", "content3".getBytes());
        MockMultipartFile image4 = new MockMultipartFile("coverImages", "image4.jpg", "image/jpeg", "content4".getBytes());
        MultipartFile[] tooManyCoverImages = {image1, image2, image3, image4}; // 4 images, but limit is 3

        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(10);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt)).thenReturn(5);
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);
        when(limitService.getEventConfig()).thenReturn(eventConfig);
        when(eventConfig.getMaxCoverPhotos()).thenReturn(3);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                eventCreationService.createEvent(createEventRequest, tooManyCoverImages, userId, jwt)
        );

        assertTrue(exception.getMessage().contains("You can upload a maximum of 3 cover photos"));

        // Verify
        verifyNoInteractions(s3StorageService);
        verifyNoInteractions(eventFactory);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_InvalidFileType_ThrowsBadRequestException() {
        // Arrange
        MultipartFile[] coverImages = {invalidImageFile}; // PDF, not an image

        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(10);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt)).thenReturn(5);
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);
        when(limitService.getEventConfig()).thenReturn(eventConfig);
        when(eventConfig.getMaxCoverPhotos()).thenReturn(3);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                eventCreationService.createEvent(createEventRequest, coverImages, userId, jwt)
        );

        assertTrue(exception.getMessage().contains("Invalid file type detected"));

        // Verify
        verifyNoInteractions(s3StorageService);
        verifyNoInteractions(eventFactory);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_ExceedsFileSizeLimit_ThrowsBadRequestException() {
        // Arrange
        MultipartFile[] coverImages = {largeImageFile}; // 6MB, exceeds 5MB limit

        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(10);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt)).thenReturn(5);
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);
        when(limitService.getEventConfig()).thenReturn(eventConfig);
        when(eventConfig.getMaxCoverPhotos()).thenReturn(3);
        when(eventConfig.getMaxCoverPhotoSize()).thenReturn(5L * 1024 * 1024); // 5MB

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                eventCreationService.createEvent(createEventRequest, coverImages, userId, jwt)
        );

        assertTrue(exception.getMessage().contains("File size exceeds the maximum allowed size"));

        // Verify
        verifyNoInteractions(s3StorageService);
        verifyNoInteractions(eventFactory);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_UploadFails_ThrowsRuntimeException() throws IOException {
        // Arrange
        MultipartFile[] coverImages = {validImageFile};

        when(organizationService.verifyOwnershipAndGetOrganization(organizationId, userId)).thenReturn(organization);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_ACTIVE_EVENTS, jwt)).thenReturn(10);
        when(limitService.getTierLimit(SubscriptionLimitType.MAX_SESSIONS_PER_EVENT, jwt)).thenReturn(5);
        when(eventRepository.countByOrganizationIdAndStatus(organizationId, EventStatus.APPROVED)).thenReturn(5L);
        when(s3StorageService.uploadFile(any(MultipartFile.class), eq("event-cover-photos")))
                .thenThrow(new IOException("Upload failed"));
        when(limitService.getEventConfig()).thenReturn(eventConfig);
        when(eventConfig.getMaxCoverPhotos()).thenReturn(3);
        when(eventConfig.getMaxCoverPhotoSize()).thenReturn(5L * 1024 * 1024); // 5MB

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                eventCreationService.createEvent(createEventRequest, coverImages, userId, jwt)
        );

        assertTrue(exception.getMessage().contains("Failed to upload cover image"));

        // Verify
        verify(s3StorageService).uploadFile(any(MultipartFile.class), eq("event-cover-photos"));
        verifyNoInteractions(eventFactory);
        verify(eventRepository, never()).save(any());
    }
}
