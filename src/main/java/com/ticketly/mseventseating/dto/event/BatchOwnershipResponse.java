package com.ticketly.mseventseating.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for batch ownership verification responses
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchOwnershipResponse {
    private List<UUID> ownedEvents;
}
