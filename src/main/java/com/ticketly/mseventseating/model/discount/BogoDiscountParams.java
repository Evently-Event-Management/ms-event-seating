package com.ticketly.mseventseating.model.discount;


/**
 * Parameters for "Buy N, Get N Free" discounts.
 *
 * @param buyQuantity The number of items a customer must buy.
 * @param getQuantity The number of items they get for free.
 */
public record BogoDiscountParams(
        int buyQuantity,
        int getQuantity
) implements DiscountParameters {
}