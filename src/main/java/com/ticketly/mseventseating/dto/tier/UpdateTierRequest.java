package com.ticketly.mseventseating.dto.tier;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateTierRequest(
    String name,
    
    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Color must be a valid hex color code (e.g., #FF5733)")
    String color,
    
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    BigDecimal price
) {
}