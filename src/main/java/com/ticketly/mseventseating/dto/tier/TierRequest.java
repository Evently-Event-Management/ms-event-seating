package com.ticketly.mseventseating.dto.tier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TierRequest {
    @NotBlank
    private String name;
    private String color;
    @NotNull
    private BigDecimal price;
}