package com.ticketly.mseventseating.model.discount;

import model.DiscountType;

import java.math.BigDecimal;

/**
 * Parameters for a simple percentage-based discount.
 *
 * @param percentage  The discount percentage (e.g., 15.5 for 15.5%).
 * @param minSpend    Optional minimum spend required for the discount to apply.
 * @param maxDiscount Optional cap on the amount of discount that can be given.
 */
public record PercentageDiscountParams(
        DiscountType type,
        BigDecimal percentage,
        BigDecimal minSpend,
        BigDecimal maxDiscount
) implements DiscountParameters {
    public PercentageDiscountParams {
        if (type != DiscountType.PERCENTAGE) {
            throw new IllegalArgumentException("Type must be PERCENTAGE for PercentageDiscountParams");
        }
        if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
        if (minSpend != null && minSpend.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum spend must be a non-negative number.");
        }
        if (maxDiscount != null && maxDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Maximum discount must be a positive number.");
        }
    }
}