package com.ticketly.mseventseating.dto.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import model.DiscountType;

/**
 * A marker interface to represent all possible discount parameter DTO structures.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PercentageDiscountParamsDTO.class, name = "PERCENTAGE"),
    @JsonSubTypes.Type(value = FlatOffDiscountParamsDTO.class, name = "FLAT_OFF"),
    @JsonSubTypes.Type(value = BogoDiscountParamsDTO.class, name = "BUY_N_GET_N_FREE"),
})
public interface DiscountParametersDTO {
    DiscountType getType();
}
