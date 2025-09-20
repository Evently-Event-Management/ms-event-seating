package com.ticketly.mseventseating.dto.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.ticketly.mseventseating.model.discount.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for handling different types of discount parameters
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DiscountParametersDTO.PercentageDTO.class, name = "PERCENTAGE"),
    @JsonSubTypes.Type(value = DiscountParametersDTO.FlatOffDTO.class, name = "FLAT_OFF"),
    @JsonSubTypes.Type(value = DiscountParametersDTO.BogoDTO.class, name = "BUY_N_GET_N_FREE"),
})
public interface DiscountParametersDTO {
    DiscountType getType();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class PercentageDTO implements DiscountParametersDTO {
        private BigDecimal percentage;

        @Override
        public DiscountType getType() {
            return DiscountType.PERCENTAGE;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class FlatOffDTO implements DiscountParametersDTO {
        private BigDecimal amount;
        private String currency;

        @Override
        public DiscountType getType() {
            return DiscountType.FLAT_OFF;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class BogoDTO implements DiscountParametersDTO {
        private int buyQuantity;
        private int getQuantity;

        @Override
        public DiscountType getType() {
            return DiscountType.BUY_N_GET_N_FREE;
        }
    }
}
