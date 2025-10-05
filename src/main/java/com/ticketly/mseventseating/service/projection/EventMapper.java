package com.ticketly.mseventseating.service.projection;

import com.ticketly.mseventseating.dto.event.DiscountResponseDTO;
import com.ticketly.mseventseating.model.Discount;
import com.ticketly.mseventseating.model.EventSession;
import com.ticketly.mseventseating.model.Tier;
import com.ticketly.mseventseating.model.discount.BogoDiscountParams;
import com.ticketly.mseventseating.model.discount.DiscountParameters;
import com.ticketly.mseventseating.model.discount.FlatOffDiscountParams;
import com.ticketly.mseventseating.model.discount.PercentageDiscountParams;
import dto.projection.DiscountProjectionDTO;
import dto.projection.TierInfo;
import dto.projection.discount.BogoDiscountParamsDTO;
import dto.projection.discount.DiscountParametersDTO;
import dto.projection.discount.FlatOffDiscountParamsDTO;
import dto.projection.discount.PercentageDiscountParamsDTO;
import lombok.RequiredArgsConstructor;
import model.DiscountType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Maps domain entities to projection DTOs for event-related data
 */
@Component
@RequiredArgsConstructor
public class EventMapper {



    /**
     * Maps a discount entity to a DiscountDetailsDTO
     *
     * @param discount The discount entity to map
     * @return A DiscountDetailsDTO with all the discount details
     */
    public DiscountResponseDTO mapToDiscountDetailsDTO(Discount discount) {
        return DiscountResponseDTO.builder()
                .id(discount.getId())
                .code(discount.getCode())
                .parameters(mapDiscountParameters(discount.getParameters()))
                .maxUsage(discount.getMaxUsage())
                .currentUsage(discount.getCurrentUsage())
                .isActive(discount.isActive())
                .isPublic(discount.isPublic())
                .activeFrom(discount.getActiveFrom())
                .expiresAt(discount.getExpiresAt())
                .applicableTierIds(discount.getApplicableTiers() != null ?
                        discount.getApplicableTiers().stream()
                                .map(Tier::getId)
                                .collect(Collectors.toList()) : null)
                .applicableSessionIds(discount.getApplicableSessions() != null ?
                        discount.getApplicableSessions().stream()
                                .map(EventSession::getId)
                                .collect(Collectors.toList()) : null)
                .build();
    }


    /**
     * Maps a discount entity to a DiscountProjectionDTO
     * 
     * @param discount The discount entity to map
     * @param tiers The list of tiers from the event
     * @return A DiscountProjectionDTO with all the discount details
     */
    public DiscountProjectionDTO mapToDiscountDetailsDTO(Discount discount, List<Tier> tiers) {
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
                .applicableTiers(discount.getApplicableTiers() != null
                        ? discount.getApplicableTiers().stream()
                        .map(tier -> tiers.stream()
                                .filter(t -> t.getId().equals(tier.getId()))
                                .findFirst()
                                .orElse(null))
                        .filter(Objects::nonNull)
                        .map(this::mapToTierInfo)
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
    public DiscountParametersDTO mapDiscountParameters(DiscountParameters parameters) {
        return getDiscountParametersDTO(parameters);
    }

    public static DiscountParametersDTO getDiscountParametersDTO(DiscountParameters parameters) {
        if (parameters == null) {
            return null;
        }

        return switch (parameters) {
            case PercentageDiscountParams p ->
                    new PercentageDiscountParamsDTO(DiscountType.PERCENTAGE, p.percentage(), p.minSpend(), p.maxDiscount());
            case FlatOffDiscountParams f ->
                    new FlatOffDiscountParamsDTO(DiscountType.FLAT_OFF, f.amount(), f.currency(), f.minSpend());
            case BogoDiscountParams b ->
                    new BogoDiscountParamsDTO(DiscountType.BUY_N_GET_N_FREE, b.buyQuantity(), b.getQuantity());
        };
    }

    /**
     * Maps a tier entity to a TierInfo DTO
     */
    public TierInfo mapToTierInfo(Tier tier) {
        return TierInfo.builder()
                .id(tier.getId())
                .name(tier.getName())
                .price(tier.getPrice())
                .color(tier.getColor())
                .build();
    }
}
