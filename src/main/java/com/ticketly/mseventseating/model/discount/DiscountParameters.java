package com.ticketly.mseventseating.model.discount;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A sealed interface to represent all possible discount parameter structures.
 * This ensures that only permitted parameter types can be created.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = PercentageDiscountParams.class, name = "PERCENTAGE"),
    @JsonSubTypes.Type(value = FlatOffDiscountParams.class, name = "FLAT_OFF"),
    @JsonSubTypes.Type(value = BogoDiscountParams.class, name = "BUY_N_GET_N_FREE"),
})
public sealed interface DiscountParameters
        permits PercentageDiscountParams, FlatOffDiscountParams, BogoDiscountParams {
}