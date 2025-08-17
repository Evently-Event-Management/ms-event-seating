package com.ticketly.mseventseating.service.event;

import com.ticketly.mseventseating.dto.event.EventDetailDTO;
import com.ticketly.mseventseating.dto.event.EventSummaryDTO;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.EventRepository;
import com.ticketly.mseventseating.service.OrganizationOwnershipService;
import com.ticketly.mseventseating.service.S3StorageService;
import model.EventStatus;
import model.SessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventOwnershipService eventOwnershipService;

    @Mock
    private OrganizationOwnershipService ownershipService;

    @Mock
    private S3StorageService s3StorageService;


    @InjectMocks
    private EventQueryService eventQueryService;

    private UUID eventId;
    private UUID organizationId;
    private String userId;
    private Event event;
    private Pageable pageable;
    private List<Event> eventList;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        userId = "test-user-id";
        pageable = PageRequest.of(0, 10);

        // Setup Organization
        Organization organization = new Organization();
        organization.setId(organizationId);
        organization.setUserId(userId);
        organization.setName("Test Organization");

        // Setup Category
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Test Category");

        // Setup Event
        event = new Event();
        event.setId(eventId);
        event.setTitle("Test Event");
        event.setDescription("Test Description");
        event.setOverview("Test Overview");
        event.setOrganization(organization);
        event.setCategory(category);
        event.setStatus(EventStatus.PENDING);

        // Setup cover photos using EventCoverPhoto entities
        List<EventCoverPhoto> coverPhotos = new ArrayList<>();
        EventCoverPhoto photo1 = new EventCoverPhoto();
        photo1.setId(UUID.randomUUID());
        photo1.setPhotoUrl("photo1.jpg");
        photo1.setEvent(event);

        EventCoverPhoto photo2 = new EventCoverPhoto();
        photo2.setId(UUID.randomUUID());
        photo2.setPhotoUrl("photo2.jpg");
        photo2.setEvent(event);

        coverPhotos.add(photo1);
        coverPhotos.add(photo2);
        event.setCoverPhotos(coverPhotos);

        event.setCreatedAt(OffsetDateTime.now().minusDays(1));
        event.setUpdatedAt(OffsetDateTime.now());

        // Setup Tiers
        Tier tier = new Tier();
        tier.setId(UUID.randomUUID());
        tier.setName("VIP");
        tier.setColor("#FF0000");
        tier.setPrice(BigDecimal.valueOf(100.0));
        tier.setEvent(event);

        // Setup Sessions
        EventSession session = new EventSession();
        session.setId(UUID.randomUUID());
        session.setStartTime(OffsetDateTime.now().plusDays(1));
        session.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(2));
        session.setEvent(event);
        session.setStatus(SessionStatus.PENDING);

        event.setTiers(Collections.singletonList(tier));
        event.setSessions(Collections.singletonList(session));

        // Setup event list for pagination tests
        eventList = new ArrayList<>();
        eventList.add(event);

        // Add another event
        Event event2 = new Event();
        event2.setId(UUID.randomUUID());
        event2.setTitle("Second Test Event");
        event2.setDescription("Second Test Description");
        event2.setOrganization(organization);
        event2.setCategory(category);
        event2.setStatus(EventStatus.APPROVED);

        // Setup cover photo for second event
        List<EventCoverPhoto> coverPhotos2 = new ArrayList<>();
        EventCoverPhoto photo3 = new EventCoverPhoto();
        photo3.setId(UUID.randomUUID());
        photo3.setPhotoUrl("photo3.jpg");
        photo3.setEvent(event2);
        coverPhotos2.add(photo3);
        event2.setCoverPhotos(coverPhotos2);

        event2.setTiers(Collections.emptyList());
        event2.setSessions(Collections.emptyList());
        eventList.add(event2);
    }

    @Test
    @DisplayName("Should return all events when status is null")
    void findAllEvents_whenStatusIsNull_shouldReturnAllEvents() {
        // Arrange
        Page<Event> eventPage = new PageImpl<>(eventList, pageable, eventList.size());
        when(eventRepository.findAll(pageable)).thenReturn(eventPage);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findAllEvents(null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals("Test Event", result.getContent().get(0).getTitle());
        assertEquals("Second Test Event", result.getContent().get(1).getTitle());
        verify(eventRepository).findAll(pageable);
        verify(eventRepository, never()).findAllByStatus(any(), any());
        verify(eventRepository, never()).findBySearchTermAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Should return filtered events when status is provided")
    void findAllEvents_whenStatusIsProvided_shouldReturnFilteredEvents() {
        // Arrange
        List<Event> filteredList = Collections.singletonList(eventList.getFirst()); // Only the PENDING event
        Page<Event> eventPage = new PageImpl<>(filteredList, pageable, filteredList.size());
        when(eventRepository.findAllByStatus(EventStatus.PENDING, pageable)).thenReturn(eventPage);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findAllEvents(EventStatus.PENDING, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test Event", result.getContent().getFirst().getTitle());
        assertEquals(EventStatus.PENDING, result.getContent().getFirst().getStatus());
        verify(eventRepository).findAllByStatus(EventStatus.PENDING, pageable);
        verify(eventRepository, never()).findAll(same(pageable));
        verify(eventRepository, never()).findBySearchTermAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Should return searched events when search term is provided")
    void findAllEvents_whenSearchTermIsProvided_shouldReturnSearchedEvents() {
        // Arrange
        String searchTerm = "Test";
        List<Event> filteredList = Collections.singletonList(eventList.getFirst());
        Page<Event> eventPage = new PageImpl<>(filteredList, pageable, filteredList.size());
        when(eventRepository.findBySearchTermAndStatus(eq(searchTerm), isNull(), eq(pageable))).thenReturn(eventPage);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findAllEvents(null, searchTerm, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(eventRepository).findBySearchTermAndStatus(eq(searchTerm), isNull(), eq(pageable));
        verify(eventRepository, never()).findAll((Example<Event>) any());
        verify(eventRepository, never()).findAllByStatus(any(), any());
    }

    @Test
    @DisplayName("Should return event details by ID when user is owner")
    void findEventByIdOwner_whenUserIsOwner_shouldReturnEventDetails() {
        // Arrange
        when(eventOwnershipService.isOwner(eventId, userId)).thenReturn(true);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        EventDetailDTO result = eventQueryService.findEventByIdOwner(eventId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("Test Event", result.getTitle());
        assertEquals(organizationId, result.getOrganizationId());
        assertEquals("Test Organization", result.getOrganizationName());
        verify(eventOwnershipService).isOwner(eventId, userId);
        verify(eventRepository).findById(eventId);
    }

    @Test
    @DisplayName("Should throw AuthorizationDeniedException when user is not event owner")
    void findEventByIdOwner_whenUserIsNotOwner_shouldThrowAuthorizationDeniedException() {
        // Arrange
        when(eventOwnershipService.isOwner(eventId, userId)).thenReturn(false);

        // Act & Assert
        AuthorizationDeniedException exception = assertThrows(AuthorizationDeniedException.class, () ->
                eventQueryService.findEventByIdOwner(eventId, userId));

        assertEquals("You don't have permission to access this event", exception.getMessage());
        verify(eventOwnershipService).isOwner(eventId, userId);
        verify(eventRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should return event details by ID without authorization check")
    void findEventById_withoutAuthorizationCheck_shouldReturnEventDetails() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Act
        EventDetailDTO result = eventQueryService.findEventById(eventId);

        // Assert
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        assertEquals("Test Event", result.getTitle());
        verify(eventRepository).findById(eventId);
        verifyNoInteractions(eventOwnershipService); // No ownership check
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when event not found without authorization check")
    void findEventById_whenEventNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
                eventQueryService.findEventById(eventId));

        assertEquals("Event not found with ID: " + eventId, exception.getMessage());
        verify(eventRepository).findById(eventId);
        verifyNoInteractions(eventOwnershipService);
    }

    @Test
    @DisplayName("Should truncate long descriptions in event summary")
    void mapToEventSummary_withLongDescription_shouldTruncateDescription() {
        // Arrange
        String longDescription = "This is a very long description that exceeds 150 characters. " +
                "It should be truncated in the summary view to make the UI cleaner and more consistent. " +
                "The truncated text should end with three dots to indicate there's more content.";
        event.setDescription(longDescription);

        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(event), pageable, 1);
        when(eventRepository.findAll(pageable)).thenReturn(eventPage);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findAllEvents(null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        String truncatedDescription = result.getContent().getFirst().getDescription();
        assertEquals(150, truncatedDescription.length());
        assertTrue(truncatedDescription.endsWith("..."));
    }

    @Test
    @DisplayName("Should handle events without sessions when finding earliest session date")
    void mapToEventSummary_withNoSessions_shouldReturnNullEarliestDate() {
        // Arrange
        event.setSessions(Collections.emptyList());
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(event), pageable, 1);
        when(eventRepository.findAll(pageable)).thenReturn(eventPage);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findAllEvents(null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().getFirst().getEarliestSessionDate());
    }

    @Test
    @DisplayName("Should handle events with EventCoverPhoto entities for cover photos")
    void mapToEventSummary_withCoverPhotos_shouldSetFirstCoverPhotoUrl() {
        // Arrange
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(event), pageable, 1);
        when(eventRepository.findAll(pageable)).thenReturn(eventPage);
        when(s3StorageService.generatePresignedUrl("photo1.jpg", 60)).thenReturn("https://s3.example.com/photo1.jpg");

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findAllEvents(null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("https://s3.example.com/photo1.jpg", result.getContent().getFirst().getCoverPhoto());
        verify(s3StorageService).generatePresignedUrl("photo1.jpg", 60);
    }

    @Test
    @DisplayName("Should handle events without cover photos")
    void mapToEventSummary_withNoCoverPhotos_shouldReturnNullCoverPhoto() {
        // Arrange
        event.setCoverPhotos(Collections.emptyList());
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(event), pageable, 1);
        when(eventRepository.findAll(pageable)).thenReturn(eventPage);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findAllEvents(null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertNull(result.getContent().getFirst().getCoverPhoto());
        verify(s3StorageService, never()).generatePresignedUrl(any(), anyInt());
    }

    @Test
    @DisplayName("Should map all cover photo URLs to EventDetailDTO")
    void mapToEventDetail_withMultipleCoverPhotos_shouldMapAllUrls() {
        // Arrange
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(s3StorageService.generatePresignedUrl("photo1.jpg", 60)).thenReturn("https://s3.example.com/photo1.jpg");
        when(s3StorageService.generatePresignedUrl("photo2.jpg", 60)).thenReturn("https://s3.example.com/photo2.jpg");

        // Act
        EventDetailDTO result = eventQueryService.findEventById(eventId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getCoverPhotos().size());
        assertEquals("https://s3.example.com/photo1.jpg", result.getCoverPhotos().get(0));
        assertEquals("https://s3.example.com/photo2.jpg", result.getCoverPhotos().get(1));
        verify(s3StorageService, times(2)).generatePresignedUrl(anyString(), eq(60));
    }

    @Test
    @DisplayName("Should find events by organization for organization owner")
    void findEventsByOrganizationOwner_withValidOwner_shouldReturnEvents() {
        // Arrange
        String searchTerm = "Test";
        EventStatus status = EventStatus.PENDING;

        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(event), pageable, 1);
        when(eventRepository.findByOrganizationIdAndSearchTermAndStatus(
                organizationId, searchTerm, status, pageable)).thenReturn(eventPage);
        when(ownershipService.isOwner(organizationId, userId)).thenReturn(true);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findEventsByOrganizationOwner(
                organizationId, userId, status, searchTerm, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test Event", result.getContent().getFirst().getTitle());
        verify(eventRepository).findByOrganizationIdAndSearchTermAndStatus(
                organizationId, searchTerm, status, pageable);
        verify(ownershipService).isOwner(organizationId, userId);
    }

    @Test
    @DisplayName("Should find events by organization for admin without ownership check")
    void findEventsByOrganization_asAdmin_shouldReturnEvents() {
        // Arrange
        String searchTerm = null;
        EventStatus status = null;

        Page<Event> eventPage = new PageImpl<>(eventList, pageable, eventList.size());
        when(eventRepository.findByOrganizationIdAndSearchTermAndStatus(
                organizationId, null, null, pageable)).thenReturn(eventPage);

        // Act
        Page<EventSummaryDTO> result = eventQueryService.findEventsByOrganization(
                organizationId, status, searchTerm, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(eventRepository).findByOrganizationIdAndSearchTermAndStatus(
                organizationId, null, null, pageable);
        verifyNoInteractions(ownershipService); // No ownership check for admin
    }

    @Test
    @DisplayName("Should throw AuthorizationDeniedException when user is not organization owner")
    void findEventsByOrganizationOwner_whenUserIsNotOwner_shouldThrowAuthorizationDeniedException() {
        // Arrange
        when(ownershipService.isOwner(organizationId, userId)).thenReturn(false);

        // Act & Assert
        AuthorizationDeniedException exception = assertThrows(AuthorizationDeniedException.class, () ->
                eventQueryService.findEventsByOrganizationOwner(organizationId, userId, null, null, pageable));

        assertEquals("User does not have access to this organization", exception.getMessage());
        verify(ownershipService).isOwner(organizationId, userId);
        // Verify that repository is never called when access is denied
        verify(eventRepository, never()).findByOrganizationIdAndSearchTermAndStatus(any(), any(), any(), any());
    }
}
