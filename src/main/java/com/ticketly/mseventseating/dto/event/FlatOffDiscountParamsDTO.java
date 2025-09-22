package com.ticketly.mseventseating.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.DiscountType;

import java.math.BigDecimal;

/**
 * DTO for flat amount discount parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlatOffDiscountParamsDTO implements DiscountParametersDTO {
    private DiscountType type;
    private BigDecimal amount;
    private String currency;
}
