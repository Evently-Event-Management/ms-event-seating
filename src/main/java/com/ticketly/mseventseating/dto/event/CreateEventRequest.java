package com.ticketly.mseventseating.dto.event;

import com.ticketly.mseventseating.dto.session.SessionRequest;
import com.ticketly.mseventseating.dto.tier.TierRequest;
import com.ticketly.mseventseating.model.SalesStartRuleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class CreateEventRequest {
    @NotBlank
    private String title;
    private String description;
    private String overview;
    private List<String> coverPhotos;

    @NotNull
    private UUID organizationId;
    private UUID venueId; // Can be null for online events

    @NotEmpty
    private Set<UUID> categoryIds;

    private boolean isOnline;
    private String onlineLink;
    private String locationDescription;

    @Valid
    @NotEmpty
    private List<TierRequest> tiers;

    @Valid
    @NotEmpty
    private List<SessionRequest> sessions;

    @NotNull
    private UUID seatingLayoutTemplateId;
}