package com.ticketly.mseventseating.service.projection;

import com.ticketly.mseventseating.exception.ResourceNotFoundException;
import com.ticketly.mseventseating.model.Discount;
import com.ticketly.mseventseating.model.Tier;
import com.ticketly.mseventseating.repository.DiscountRepository;
import com.ticketly.mseventseating.repository.TierRepository;
import dto.projection.DiscountProjectionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.EventStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscountProjectionService {
    private final DiscountRepository discountRepository;
    private final TierRepository tierRepository;
    private final EventMapper eventMapper;

    public DiscountProjectionDTO projectDiscount(UUID discountId) {
        Discount discount = discountRepository.findById(discountId).orElse(null);
        if (discount == null) {
            throw new ResourceNotFoundException("discount not found");
        }
        if (discount.getEvent().getStatus() != EventStatus.APPROVED &&
                discount.getEvent().getStatus() != EventStatus.COMPLETED) {
            throw new ResourceNotFoundException("Event is not approved for projection: " + discount.getEvent().getId());
        }

        List<Tier> tiers = tierRepository.findByEventId(discount.getEvent().getId());
        return eventMapper.mapToDiscountDetailsDTO(discount, tiers);
    }
}
