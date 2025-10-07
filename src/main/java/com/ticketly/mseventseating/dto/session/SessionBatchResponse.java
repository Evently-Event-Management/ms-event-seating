package com.ticketly.mseventseating.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SessionBatchResponse {
    private UUID eventId;
    private int totalCreated;
    private List<SessionResponse> sessions;
}