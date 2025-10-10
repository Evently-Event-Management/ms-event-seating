package com.ticketly.mseventseating.dto.tier;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateTierRequest(
    @NotBlank(message = "Tier name is required")
    String name,
    
    @NotBlank(message = "Color is required")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Color must be a valid hex color code (e.g., #FF5733)")
    String color,
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    BigDecimal price
) {
}