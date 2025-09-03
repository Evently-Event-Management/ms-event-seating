package com.ticketly.mseventseating.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GenerateOverviewRequest(
        @NotNull @NotBlank String prompt,
        String title,
        String organization,
        String description,
        String category) {
}