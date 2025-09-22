package com.ticketly.mseventseating.model.discount;
import model.DiscountType;

import java.math.BigDecimal;

/**
 * Parameters for a simple percentage-based discount.
 * @param percentage The discount percentage (e.g., 15.5 for 15.5%).
 */
public record PercentageDiscountParams(
        DiscountType type,
        BigDecimal percentage
) implements DiscountParameters {
    public PercentageDiscountParams {
        if (type != DiscountType.PERCENTAGE) {
            throw new IllegalArgumentException("Type must be PERCENTAGE for PercentageDiscountParams");
        }
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
    }
}