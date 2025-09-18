package com.ticketly.mseventseating.dto.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateEventRequest {
    @NotBlank
    private String title;
    private String description;
    private String overview;
    private List<String> coverPhotos;

    @NotNull
    private UUID organizationId;
    private UUID venueId;

    @NotNull
    private UUID categoryId;

    @Valid
    @NotEmpty
    private List<TierRequest> tiers;

    @Valid
    @NotEmpty
    private List<SessionRequest> sessions;
}
