package com.ticketly.mseventseating.model.discount;
import model.DiscountType;

/**
 * Parameters for "Buy N, Get N Free" discounts.
 * @param type The type of discount, must be BUY_N_GET_N_FREE.
 * @param buyQuantity The number of items a customer must buy.
 * @param getQuantity The number of items they get for free.
 */
public record BogoDiscountParams(
        DiscountType type,
        int buyQuantity,
        int getQuantity
) implements DiscountParameters {

    // Compact constructor for validation
    public BogoDiscountParams {
        if (type != DiscountType.BUY_N_GET_N_FREE) {
            throw new IllegalArgumentException("Type must be BUY_N_GET_N_FREE for BogoDiscountParams");
        }
        if (buyQuantity <= 0 || getQuantity <= 0) {
            throw new IllegalArgumentException("Buy and Get quantities must be positive numbers.");
        }
    }
}