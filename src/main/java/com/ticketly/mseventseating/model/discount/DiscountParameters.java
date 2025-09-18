package com.ticketly.mseventseating.model.discount;

/**
 * A sealed interface to represent all possible discount parameter structures.
 * This ensures that only permitted parameter types can be created.
 */
public sealed interface DiscountParameters
        permits PercentageDiscountParams, FlatOffDiscountParams, BogoDiscountParams {
}