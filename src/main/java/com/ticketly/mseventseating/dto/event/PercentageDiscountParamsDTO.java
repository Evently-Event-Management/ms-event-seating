package com.ticketly.mseventseating.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for percentage-based discount parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PercentageDiscountParamsDTO implements DiscountParametersDTO {
    private BigDecimal percentage;
}
