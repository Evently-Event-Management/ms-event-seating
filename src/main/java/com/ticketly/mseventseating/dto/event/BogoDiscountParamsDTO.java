package com.ticketly.mseventseating.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for "Buy N, Get N Free" discount parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BogoDiscountParamsDTO implements DiscountParametersDTO {
    private int buyQuantity;
    private int getQuantity;
}
