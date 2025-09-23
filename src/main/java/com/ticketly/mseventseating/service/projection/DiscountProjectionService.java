package com.ticketly.mseventseating.service.projection;

import com.ticketly.mseventseating.dto.event.*;
import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Discount;
import com.ticketly.mseventseating.model.EventSession;
import com.ticketly.mseventseating.model.Tier;
import com.ticketly.mseventseating.model.discount.BogoDiscountParams;
import com.ticketly.mseventseating.model.discount.DiscountParameters;
import com.ticketly.mseventseating.model.discount.FlatOffDiscountParams;
import com.ticketly.mseventseating.model.discount.PercentageDiscountParams;
import com.ticketly.mseventseating.repository.DiscountRepository;
import dto.projection.DiscountProjectionDTO;
import dto.projection.discount.BogoDiscountParamsProjectionDTO;
import dto.projection.discount.DiscountParametersProjectionDTO;
import dto.projection.discount.FlatOffDiscountParamsProjectionDTO;
import dto.projection.discount.PercentageDiscountParamsProjectionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.DiscountType;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountProjectionService {
    private final DiscountRepository discountRepository;

    public DiscountProjectionDTO projectDiscount(UUID discountId) {
        Discount discount = discountRepository.findById(discountId).orElse(null);
        if (discount == null) {
            throw new ResourceNotFoundException("discount not found");
        }
        return mapToDiscountDetailsDTO(discount);
    }


    protected DiscountProjectionDTO mapToDiscountDetailsDTO(Discount discount) {
        return DiscountProjectionDTO.builder()
                .id(discount.getId())
                .code(discount.getCode())
                .parameters(mapDiscountParameters(discount.getParameters()))
                .maxUsage(discount.getMaxUsage())
                .currentUsage(discount.getCurrentUsage())
                .isActive(discount.isActive())
                .isPublic(discount.isPublic())
                .activeFrom(discount.getActiveFrom())
                .expiresAt(discount.getExpiresAt())
                .applicableTierIds(discount.getApplicableTiers() != null
                        ? discount.getApplicableTiers().stream()
                        .map(Tier::getId)
                        .collect(Collectors.toList())
                        : null)
                .applicableSessionIds(discount.getApplicableSessions() != null
                        ? discount.getApplicableSessions().stream()
                        .map(EventSession::getId)
                        .collect(Collectors.toList())
                        : null)
                .build();
    }

    /**
     * Maps domain DiscountParameters to DTO DiscountParametersDTO based on their type
     */
    protected DiscountParametersProjectionDTO mapDiscountParameters(DiscountParameters parameters) {
        if (parameters == null) {
            return null;
        }

        return switch (parameters) {
            case PercentageDiscountParams p ->
                    new PercentageDiscountParamsProjectionDTO(DiscountType.PERCENTAGE, p.percentage());
            case FlatOffDiscountParams f ->
                    new FlatOffDiscountParamsProjectionDTO(DiscountType.FLAT_OFF, f.amount(), f.currency());
            case BogoDiscountParams b ->
                    new BogoDiscountParamsProjectionDTO(DiscountType.BUY_N_GET_N_FREE, b.buyQuantity(), b.getQuantity());
        };
    }
}
