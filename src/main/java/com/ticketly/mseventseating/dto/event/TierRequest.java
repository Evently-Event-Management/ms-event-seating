package com.ticketly.mseventseating.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TierRequest {
    @NotBlank
    private String id;

    @NotBlank
    private String name;
    private String color;
    @NotNull
    private BigDecimal price;
}