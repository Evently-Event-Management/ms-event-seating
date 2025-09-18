package com.ticketly.mseventseating.model.discount;

import com.ticketly.mseventseating.model.discount.DiscountParameters;
import java.math.BigDecimal;

/**
 * Parameters for a flat amount discount.
 * @param amount The fixed amount to deduct.
 * @param currency The ISO 4217 currency code (e.g., "USD", "LKR").
 */
public record FlatOffDiscountParams(
        BigDecimal amount,
        String currency
) implements DiscountParameters {}