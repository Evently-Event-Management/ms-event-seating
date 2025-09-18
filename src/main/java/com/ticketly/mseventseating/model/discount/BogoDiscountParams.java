package com.ticketly.mseventseating.model.discount;

/**
 * Parameters for "Buy N, Get N Free" discounts.
 * @param buyQuantity The number of items a customer must buy.
 * @param getQuantity The number of items they get for free.
 * @param policy Defines how the discount is applied (e.g., to the cheapest items).
 */
public record BogoDiscountParams(
        int buyQuantity,
        int getQuantity,
        String policy // e.g., "APPLY_TO_CHEAPEST"
) implements DiscountParameters {}