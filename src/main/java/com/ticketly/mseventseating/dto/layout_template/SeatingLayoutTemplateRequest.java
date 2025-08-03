package com.ticketly.mseventseating.dto.layout_template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatingLayoutTemplateRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Organization ID is required")
    private UUID organizationId;

    @NotNull(message = "Layout data is required")
    private LayoutDataDTO layoutData;
}
