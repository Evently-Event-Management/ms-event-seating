package com.ticketly.mseventseating.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.EventStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventDetailDTO {
    private UUID id;
    private String title;
    private String description;
    private String overview;
    private EventStatus status;
    private String rejectionReason;
    private List<String> coverPhotos;
    private UUID organizationId;
    private String organizationName;
    private UUID categoryId;
    private String categoryName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<TierDTO> tiers;
    private List<SessionDetailDTO> sessions;
}
