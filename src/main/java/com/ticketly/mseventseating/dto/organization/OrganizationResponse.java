package com.ticketly.mseventseating.dto.organization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {
    private UUID id;
    private String name;
    private String logoUrl;
    private String website;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
