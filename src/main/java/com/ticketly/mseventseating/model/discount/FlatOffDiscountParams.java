package com.ticketly.mseventseating.model.discount;

import model.DiscountType;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Parameters for a flat amount discount.
 *
 * @param type     The type of discount, must be FLAT_OFF.
 * @param amount   The fixed amount to deduct.
 * @param currency The ISO 4217 currency code (e.g., "USD", "LKR").
 * @param minSpend Optional minimum spend required for the discount to apply.
 *
 */
public record FlatOffDiscountParams(
        DiscountType type,
        BigDecimal amount,
        String currency,
        BigDecimal minSpend
) implements DiscountParameters {

    // Compact constructor for validation and normalization
    public FlatOffDiscountParams {
        if (type != DiscountType.FLAT_OFF) {
            throw new IllegalArgumentException("Type must be FLAT_OFF for FlatOffDiscountParams");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount amount must be a positive number.");
        }

        Objects.requireNonNull(currency, "Currency cannot be null.");
        // Normalization: Ensure currency is always stored in a clean, consistent format.
        currency = currency.trim().toUpperCase();
        if (currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code.");
        }
        if (minSpend != null && minSpend.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum spend must be a non-negative number.");
        }
    }
}