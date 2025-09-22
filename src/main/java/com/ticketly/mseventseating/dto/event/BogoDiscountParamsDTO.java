package com.ticketly.mseventseating.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.DiscountType;

/**
 * DTO for "Buy N, Get N Free" discount parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BogoDiscountParamsDTO implements DiscountParametersDTO {
    private DiscountType type;
    private int buyQuantity;
    private int getQuantity;
}
