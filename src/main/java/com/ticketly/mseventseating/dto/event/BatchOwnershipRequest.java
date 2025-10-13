package com.ticketly.mseventseating.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for batch ownership verification requests
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchOwnershipRequest {
    private List<UUID> eventIds;
    private String userId;
}

