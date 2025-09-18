package com.ticketly.mseventseating.model.discount;
import java.math.BigDecimal;

/**
 * Parameters for a simple percentage-based discount.
 * @param percentage The discount percentage (e.g., 15.5 for 15.5%).
 */
public record PercentageDiscountParams(
        BigDecimal percentage
) implements DiscountParameters {}