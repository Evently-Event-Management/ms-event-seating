package com.ticketly.mseventseating.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.CreateEventRequest;
import com.ticketly.mseventseating.dto.session.SessionRequest;
import com.ticketly.mseventseating.dto.session_layout.SessionSeatingMapRequest;
import com.ticketly.mseventseating.dto.tier.TierRequest;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.*;
import com.ticketly.mseventseating.repository.CategoryRepository;
import com.ticketly.mseventseating.repository.VenueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventFactoryTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Spy // Use a real ObjectMapper instance that we can also verify calls on
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private EventFactory eventFactory;

    private Venue venue;
    private Set<Category> categories;
    private Organization organization;
    private CreateEventRequest createEventRequest;

    @BeforeEach
    void setUp() {
        // --- Basic Setup ---
        UUID venueId = UUID.randomUUID();
        venue = Venue.builder().id(venueId).name("Test Venue").build();

        UUID categoryId = UUID.randomUUID();
        categories = Set.of(Category.builder().id(categoryId).name("Test Category").build());

        organization = Organization.builder().id(UUID.randomUUID()).name("Test Organization").build();

        // --- Tier Setup with Temporary IDs ---
        TierRequest vipTierRequest = TierRequest.builder()
                .id("temp_vip_id") // Temporary client-side ID
                .name("VIP")
                .price(new BigDecimal("100.00"))
                .build();

        TierRequest standardTierRequest = TierRequest.builder()
                .id("temp_std_id") // Temporary client-side ID
                .name("Standard")
                .price(new BigDecimal("50.00"))
                .build();

        // --- Seating Map Setup using Temporary Tier IDs ---
        SessionSeatingMapRequest.Seat vipSeat = new SessionSeatingMapRequest.Seat();
        vipSeat.setLabel("A1");
        vipSeat.setTierId("temp_vip_id"); // Mapped to the temporary VIP tier

        SessionSeatingMapRequest.Seat stdSeat = new SessionSeatingMapRequest.Seat();
        stdSeat.setLabel("B1");
        stdSeat.setTierId("temp_std_id"); // Mapped to the temporary Standard tier

        SessionSeatingMapRequest.Row rowA = new SessionSeatingMapRequest.Row();
        rowA.setLabel("A");
        // ✅ Changed from immutable to mutable ArrayList for seats
        rowA.setSeats(new ArrayList<>(List.of(vipSeat)));

        SessionSeatingMapRequest.Row rowB = new SessionSeatingMapRequest.Row();
        rowB.setLabel("B");
        // ✅ Changed from immutable to mutable ArrayList for seats
        rowB.setSeats(new ArrayList<>(List.of(stdSeat)));

        SessionSeatingMapRequest.Block block = new SessionSeatingMapRequest.Block();
        block.setType("seated_grid");
        // ✅ Changed from immutable to mutable ArrayList for rows
        block.setRows(new ArrayList<>(List.of(rowA, rowB)));

        SessionSeatingMapRequest layoutData = new SessionSeatingMapRequest();
        SessionSeatingMapRequest.Layout layout = new SessionSeatingMapRequest.Layout();
        // ✅ Changed from immutable to mutable ArrayList for blocks
        layout.setBlocks(new ArrayList<>(List.of(block)));
        layoutData.setLayout(layout);

        // --- Final Request Object ---
        createEventRequest = CreateEventRequest.builder()
                .title("Test Event")
                .organizationId(organization.getId())
                .venueId(venueId)
                .categoryIds(Set.of(categoryId))
                .tiers(List.of(vipTierRequest, standardTierRequest))
                .sessions(List.of(
                        SessionRequest.builder()
                                .startTime(OffsetDateTime.now().plusDays(10))
                                .endTime(OffsetDateTime.now().plusDays(10).plusHours(2))
                                .salesStartRuleType(SalesStartRuleType.IMMEDIATE)
                                .sessionSeatingMapRequest(layoutData)
                                .build()
                ))
                .build();
    }

    @Test
    void createFromRequest_ShouldCorrectlyMapTempTierIdsToPermanentUuids() {
        // Arrange
        when(venueRepository.findById(any(UUID.class))).thenReturn(Optional.of(venue));
        when(categoryRepository.findAllById(any())).thenReturn(new ArrayList<>(categories));

        // Act
        Event resultEvent = eventFactory.createFromRequest(createEventRequest, organization);

        // Assert
        assertNotNull(resultEvent);
        assertEquals(2, resultEvent.getTiers().size());
        assertEquals(1, resultEvent.getSessions().size());

        // Extract the permanent UUIDs generated for the tiers
        UUID vipTierUuid = resultEvent.getTiers().stream()
                .filter(t -> "VIP".equals(t.getName())).findFirst().get().getId();
        UUID stdTierUuid = resultEvent.getTiers().stream()
                .filter(t -> "Standard".equals(t.getName())).findFirst().get().getId();

        assertNotNull(vipTierUuid);
        assertNotNull(stdTierUuid);

        // Verify the layout data in the session map was correctly transformed
        String finalLayoutJson = resultEvent.getSessions().getFirst().getSessionSeatingMap().getLayoutData();

        try {
            SessionSeatingMapRequest finalLayout = objectMapper.readValue(finalLayoutJson, SessionSeatingMapRequest.class);

            // Check that the temporary tier IDs have been replaced with the permanent UUIDs
            String seatA1TierId = finalLayout.getLayout().getBlocks().getFirst().getRows().get(0).getSeats().getFirst().getTierId();
            String seatB1TierId = finalLayout.getLayout().getBlocks().getFirst().getRows().get(1).getSeats().getFirst().getTierId();

            assertEquals(vipTierUuid.toString(), seatA1TierId);
            assertEquals(stdTierUuid.toString(), seatB1TierId);

        } catch (JsonProcessingException e) {
            fail("Failed to parse final layout JSON", e);
        }
    }

    @Test
    void createFromRequest_WithInvalidTierIdInLayout_ShouldThrowBadRequestException() {
        // Arrange
        // Add a seat with a tierId that does not exist in the tiers list
        SessionSeatingMapRequest.Seat invalidSeat = new SessionSeatingMapRequest.Seat();
        invalidSeat.setLabel("C1");
        invalidSeat.setTierId("invalid_temp_id");

        // Now this will work because we're using mutable ArrayLists
        createEventRequest.getSessions().getFirst().getSessionSeatingMapRequest()
                .getLayout().getBlocks().getFirst().getRows().getFirst().getSeats().add(invalidSeat);

        when(venueRepository.findById(any(UUID.class))).thenReturn(Optional.of(venue));
        when(categoryRepository.findAllById(any())).thenReturn(new ArrayList<>(categories));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> eventFactory.createFromRequest(createEventRequest, organization));

        assertTrue(exception.getMessage().contains("Seat is assigned to an invalid Tier ID"));
    }

    @Test
    void createFromRequest_WhenVenueNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        when(venueRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> eventFactory.createFromRequest(createEventRequest, organization));
    }
}
