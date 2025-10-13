package com.ticketly.mseventseating.service.seat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketly.mseventseating.dto.event.SeatDetailsRequest;
import com.ticketly.mseventseating.dto.event.SeatDetailsResponse;
import com.ticketly.mseventseating.exception.BadRequestException;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.EventSession;
import com.ticketly.mseventseating.model.SessionSeatingMap;
import com.ticketly.mseventseating.model.Tier;
import com.ticketly.mseventseating.repository.EventSessionRepository;
import com.ticketly.mseventseating.repository.TierRepository;
import dto.SessionSeatingMapDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.SeatStatus;
import model.SessionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatValidationService {

    private final EventSessionRepository sessionRepository;
    private final TierRepository tierRepository;
    private final ObjectMapper objectMapper;

    /**
     * Validates and retrieves details for seats in a specific session, ensuring they are all AVAILABLE.
     * If any seat is not available or not found, an exception is thrown.
     *
     * @param sessionId The ID of the session the seats belong to
     * @param request   The request containing seat IDs to validate
     * @return List of SeatDetailsResponse for all valid seats
     */
    @Transactional(readOnly = true)
    public List<SeatDetailsResponse> validateAndGetSeatsDetails(UUID sessionId, SeatDetailsRequest request) {
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw new BadRequestException("Seat IDs list cannot be empty");
        }

        // Get the session
        EventSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        if (!(session.getStatus() == SessionStatus.ON_SALE)) {
            throw new BadRequestException("Session is not on sale: " + sessionId);
        }

        // Get the seating map for this session
        SessionSeatingMap seatingMap = session.getSessionSeatingMap();
        if (seatingMap == null) {
            throw new ResourceNotFoundException("Seating map not found for session: " + sessionId);
        }

        // Get the tiers associated with this session's event
        Map<UUID, Tier> tiersById = tierRepository.findByEventId(session.getEvent().getId())
                .stream()
                .collect(Collectors.toMap(Tier::getId, tier -> tier));

        // Parse the seating map JSON
        SessionSeatingMapDTO mapDTO;
        try {
            mapDTO = objectMapper.readValue(seatingMap.getLayoutData(), SessionSeatingMapDTO.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing seating map data for session ID: {}", sessionId, e);
            throw new BadRequestException("Invalid seating map data format");
        }

        List<SeatDetailsResponse> results = new ArrayList<>();
        Set<UUID> remainingSeatIds = new HashSet<>(request.getSeatIds());

        // Process each block and validate seats
        for (SessionSeatingMapDTO.Block block : mapDTO.getLayout().getBlocks()) {
            processSeatsByBlock(block, remainingSeatIds, tiersById, results);
        }

        // Check if all requested seats were found
        if (!remainingSeatIds.isEmpty()) {
            throw new ResourceNotFoundException("Some seats were not found: " + remainingSeatIds);
        }

        return results;
    }

    private void processSeatsByBlock(SessionSeatingMapDTO.Block block, Set<UUID> seatIds,
                                     Map<UUID, Tier> tiersById, List<SeatDetailsResponse> results) {
        // Process individual seats in the block
        if (block.getSeats() != null) {
            processSeatsList(block.getSeats(), seatIds, tiersById, results);
        }

        // Process seats in rows
        if (block.getRows() != null) {
            for (SessionSeatingMapDTO.Row row : block.getRows()) {
                if (row.getSeats() != null) {
                    processSeatsList(row.getSeats(), seatIds, tiersById, results);
                }
            }
        }
    }

    private void processSeatsList(List<SessionSeatingMapDTO.Seat> seats, Set<UUID> seatIds,
                                  Map<UUID, Tier> tiersById, List<SeatDetailsResponse> results) {
        for (SessionSeatingMapDTO.Seat seat : seats) {
            try {
                if (seatIds.contains(seat.getId())) {
                    // Verify seat is available
                    if (seat.getStatus() != SeatStatus.AVAILABLE) {
                        throw new BadRequestException("Seat " + seat.getId() + " is not available. Current status: " + seat.getStatus());
                    }

                    // Get tier information
                    Tier tier = tiersById.get(seat.getTierId());
                    if (tier == null) {
                        throw new ResourceNotFoundException("Tier not found for seat: " + seat.getId());
                    }

                    // Create response
                    SeatDetailsResponse response = SeatDetailsResponse.builder()
                            .seatId(seat.getId())
                            .label(seat.getLabel())
                            .tier(SeatDetailsResponse.TierInfo.builder()
                                    .id(tier.getId())
                                    .name(tier.getName())
                                    .price(tier.getPrice())
                                    .color(tier.getColor())
                                    .build())
                            .build();

                    results.add(response);
                    seatIds.remove(seat.getId());
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid seat ID format: {}", seat.getId());
                throw new BadRequestException("Invalid seat ID format: " + seat.getId());
            }
        }
    }
}
