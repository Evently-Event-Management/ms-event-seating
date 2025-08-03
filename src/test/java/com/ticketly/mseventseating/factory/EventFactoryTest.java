package com.ticketly.mseventseating.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.*;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.CategoryRepository;
import com.ticketly.mseventseating.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventFactoryTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventFactory eventFactory;

    private Venue venue;
    private Set<Category> categories;
    private Organization organization;
    private CreateEventRequest createEventRequest;
    private String sessionLayoutData;
    private LayoutDataDTO mockLayoutData;
    private UUID venueId;

    @BeforeEach
    void setUp() {
        // Initialize test data
        venueId = UUID.randomUUID();
        venue = Venue.builder()
                .id(venueId)
                .name("Test Venue")
                .build();

        UUID categoryId1 = UUID.randomUUID();
        UUID categoryId2 = UUID.randomUUID();
        Category category1 = Category.builder()
                .id(categoryId1)
                .name("Category 1")
                .build();
        Category category2 = Category.builder()
                .id(categoryId2)
                .name("Category 2")
                .build();
        categories = new HashSet<>(Arrays.asList(category1, category2));

        organization = Organization.builder()
                .id(UUID.randomUUID())
                .name("Test Organization")
                .build();

        sessionLayoutData = "{\"layout\":{\"blocks\":[{\"type\":\"seated_grid\",\"name\":\"Block A\",\"rows\":[{\"label\":\"1\",\"seats\":[{\"label\":\"1\"},{\"label\":\"2\"}]}]}]}}";

        // Mock valid layout data
        mockLayoutData = new LayoutDataDTO();
        LayoutDataDTO.Layout layout = getLayout();
        mockLayoutData.setLayout(layout);

        // Configure request with current date time
        OffsetDateTime now = OffsetDateTime.now();

        createEventRequest = CreateEventRequest.builder()
                .title("Test Event")
                .description("Test Description")
                .overview("Test Overview")
                .venueId(venueId)
                .categoryIds(Set.of(categoryId1, categoryId2))
                .isOnline(false)
                .coverPhotos(List.of("photo1.jpg", "photo2.jpg"))
                .tiers(Arrays.asList(
                    TierRequest.builder()
                        .name("VIP")
                        .price(new BigDecimal("100.0"))
                        .color("#FFFF00")
                        .build(),
                    TierRequest.builder()
                        .name("Standard")
                        .price(new BigDecimal("50.0"))
                        .color("#FF0000")
                        .build()
                ))
                .sessions(Collections.singletonList(
                        SessionRequest.builder()
                                .startTime(now.plusDays(10))
                                .endTime(now.plusDays(10).plusHours(3))
                                .salesStartRuleType(SalesStartRuleType.FIXED)
                                .salesStartFixedDatetime(now.plusDays(1))
                                .build()
                ))
                .sessionLayoutData(sessionLayoutData)
                .build();
    }

    private static LayoutDataDTO.Layout getLayout() {
        LayoutDataDTO.Layout layout = new LayoutDataDTO.Layout();
        List<LayoutDataDTO.Block> blocks = new ArrayList<>();

        LayoutDataDTO.Block block = new LayoutDataDTO.Block();
        block.setType("seated_grid");
        block.setName("Block A");

        List<LayoutDataDTO.Row> rows = new ArrayList<>();
        LayoutDataDTO.Row row = new LayoutDataDTO.Row();
        row.setLabel("1");

        List<LayoutDataDTO.Seat> seats = new ArrayList<>();
        LayoutDataDTO.Seat seat1 = new LayoutDataDTO.Seat();
        seat1.setLabel("1");
        LayoutDataDTO.Seat seat2 = new LayoutDataDTO.Seat();
        seat2.setLabel("2");

        seats.add(seat1);
        seats.add(seat2);
        row.setSeats(seats);
        rows.add(row);
        block.setRows(rows);
        blocks.add(block);
        layout.setBlocks(blocks);
        return layout;
    }

    @Test
    void createFromRequest_ShouldCreateEventWithAllProperties() throws JsonProcessingException {
        // Setup mocks for this test
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(categoryRepository.findAllById(any())).thenReturn(new ArrayList<>(categories));
        when(objectMapper.readValue(eq(sessionLayoutData), eq(LayoutDataDTO.class))).thenReturn(mockLayoutData);
        when(objectMapper.writeValueAsString(any(LayoutDataDTO.class))).thenReturn(sessionLayoutData);

        // When
        Event event = eventFactory.createFromRequest(createEventRequest, organization);

        // Then
        assertNotNull(event);
        assertEquals("Test Event", event.getTitle());
        assertEquals("Test Description", event.getDescription());
        assertEquals("Test Overview", event.getOverview());
        assertEquals(organization, event.getOrganization());
        assertEquals(venue, event.getVenue());
        assertEquals(categories, event.getCategories());
        assertEquals(List.of("photo1.jpg", "photo2.jpg"), event.getCoverPhotos());
        assertFalse(event.isOnline());

        // Verify tiers
        assertEquals(2, event.getTiers().size());
        assertTrue(event.getTiers().stream().anyMatch(t ->
            t.getName().equals("VIP") && t.getPrice().compareTo(new BigDecimal("100.0")) == 0));
        assertTrue(event.getTiers().stream().anyMatch(t ->
            t.getName().equals("Standard") && t.getPrice().compareTo(new BigDecimal("50.0")) == 0));

        // Verify sessions
        assertEquals(1, event.getSessions().size());
        EventSession session = event.getSessions().getFirst();
        assertEquals(SalesStartRuleType.FIXED, session.getSalesStartRuleType());
        assertNotNull(session.getSessionSeatingMap());
        assertNotNull(session.getSessionSeatingMap().getLayoutData());

        // Verify interactions
        verify(venueRepository).findById(createEventRequest.getVenueId());
        verify(categoryRepository).findAllById(createEventRequest.getCategoryIds());
        verify(objectMapper).readValue(eq(sessionLayoutData), eq(LayoutDataDTO.class));
        verify(objectMapper).writeValueAsString(any(LayoutDataDTO.class));
    }

    @Test
    void createFromRequest_WhenVenueNotFound_ShouldThrowException() {
        // Given
        UUID nonExistentVenueId = UUID.randomUUID();
        createEventRequest.setVenueId(nonExistentVenueId);
        when(venueRepository.findById(nonExistentVenueId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () ->
            eventFactory.createFromRequest(createEventRequest, organization)
        );

        verify(venueRepository).findById(nonExistentVenueId);
        // Ensure no other calls were made after the venue was not found
        verifyNoMoreInteractions(venueRepository, categoryRepository, objectMapper);
    }

    @Test
    void createFromRequest_WithNullVenueId_ShouldCreateEventWithoutVenue() throws Exception {
        // Given
        createEventRequest.setVenueId(null);
        when(categoryRepository.findAllById(any())).thenReturn(new ArrayList<>(categories));
        when(objectMapper.readValue(anyString(), eq(LayoutDataDTO.class))).thenReturn(mockLayoutData);
        when(objectMapper.writeValueAsString(any(LayoutDataDTO.class))).thenReturn(sessionLayoutData);

        // When
        Event event = eventFactory.createFromRequest(createEventRequest, organization);

        // Then
        assertNotNull(event);
        assertNull(event.getVenue());
        verify(venueRepository, never()).findById(any());
        verify(categoryRepository).findAllById(createEventRequest.getCategoryIds());
        verify(objectMapper).readValue(eq(sessionLayoutData), eq(LayoutDataDTO.class));
        verify(objectMapper).writeValueAsString(any(LayoutDataDTO.class));
    }

    @Test
    void validateAndPrepareSessionLayout_ShouldAssignUniqueIds() throws Exception {
        // Prepare a test layout with blocks, rows, and seats
        String testLayout = "{\"layout\":{\"blocks\":[{\"type\":\"seated_grid\",\"name\":\"Block A\",\"rows\":[{\"label\":\"1\",\"seats\":[{\"label\":\"1\",\"status\":null},{\"label\":\"2\"}]}]},{\"type\":\"standing_capacity\",\"name\":\"Standing Area\",\"capacity\":100}]}}";

        // Setup required mocks for the test to run successfully
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(categoryRepository.findAllById(any())).thenReturn(new ArrayList<>(categories));

        // Create DTO objects for the mocking chain
        LayoutDataDTO testLayoutData = getLayoutDataDTO();

        // Set up mocking
        when(objectMapper.readValue(eq(testLayout), eq(LayoutDataDTO.class))).thenReturn(testLayoutData);

        // Capture the modified object that will be serialized back
        when(objectMapper.writeValueAsString(any(LayoutDataDTO.class))).thenAnswer(invocation -> {
            LayoutDataDTO modified = invocation.getArgument(0);

            // Assert all blocks have IDs
            for (LayoutDataDTO.Block block : modified.getLayout().getBlocks()) {
                assertNotNull(block.getId());

                if ("seated_grid".equals(block.getType())) {
                    for (LayoutDataDTO.Row r : block.getRows()) {
                        assertNotNull(r.getId());

                        for (LayoutDataDTO.Seat s : r.getSeats()) {
                            assertNotNull(s.getId());
                            assertNotNull(s.getStatus());
                        }
                    }
                } else if ("standing_capacity".equals(block.getType())) {
                    assertNotNull(block.getSoldCount());
                    assertEquals(0, block.getSoldCount());
                }
            }

            return testLayout;
        });

        // Create a test request with our layout
        createEventRequest.setSessionLayoutData(testLayout);

        // Call the method that uses validateAndPrepareSessionLayout internally
        Event event = eventFactory.createFromRequest(createEventRequest, organization);

        // Verify the session layout data was processed
        assertNotNull(event);
        assertEquals(1, event.getSessions().size());
        assertNotNull(event.getSessions().getFirst().getSessionSeatingMap());

        // Verify the validation method was called
        verify(objectMapper).readValue(eq(testLayout), eq(LayoutDataDTO.class));
        verify(objectMapper).writeValueAsString(any(LayoutDataDTO.class));
    }

    private static LayoutDataDTO getLayoutDataDTO() {
        LayoutDataDTO testLayoutData = new LayoutDataDTO();
        LayoutDataDTO.Layout layout = new LayoutDataDTO.Layout();
        List<LayoutDataDTO.Block> blocks = new ArrayList<>();

        // Seated block
        LayoutDataDTO.Block seatedBlock = new LayoutDataDTO.Block();
        seatedBlock.setType("seated_grid");
        seatedBlock.setName("Block A");

        List<LayoutDataDTO.Row> rows = new ArrayList<>();
        LayoutDataDTO.Row row = new LayoutDataDTO.Row();
        row.setLabel("1");

        List<LayoutDataDTO.Seat> seats = new ArrayList<>();
        LayoutDataDTO.Seat seat1 = new LayoutDataDTO.Seat();
        seat1.setLabel("1");
        LayoutDataDTO.Seat seat2 = new LayoutDataDTO.Seat();
        seat2.setLabel("2");
        seat2.setStatus("AVAILABLE");

        seats.add(seat1);
        seats.add(seat2);
        row.setSeats(seats);
        rows.add(row);
        seatedBlock.setRows(rows);

        // Standing block
        LayoutDataDTO.Block standingBlock = new LayoutDataDTO.Block();
        standingBlock.setType("standing_capacity");
        standingBlock.setName("Standing Area");
        standingBlock.setCapacity(100);

        blocks.add(seatedBlock);
        blocks.add(standingBlock);
        layout.setBlocks(blocks);
        testLayoutData.setLayout(layout);
        return testLayoutData;
    }

    @Test
    void validateAndPrepareSessionLayout_WithInvalidJson_ShouldThrowException() throws Exception {
        // Given
        String invalidJson = "{invalid json}";

        // Setup venue repository to prevent ResourceNotFoundException
        when(venueRepository.findById(venueId)).thenReturn(Optional.of(venue));
        when(categoryRepository.findAllById(any())).thenReturn(new ArrayList<>(categories));

        when(objectMapper.readValue(eq(invalidJson), eq(LayoutDataDTO.class)))
            .thenThrow(new com.fasterxml.jackson.core.JsonParseException(null, "Invalid JSON"));

        createEventRequest.setSessionLayoutData(invalidJson);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
            eventFactory.createFromRequest(createEventRequest, organization)
        );

        // Verify method calls
        verify(venueRepository).findById(venueId);
        verify(categoryRepository).findAllById(any());
        verify(objectMapper).readValue(eq(invalidJson), eq(LayoutDataDTO.class));
    }
}
