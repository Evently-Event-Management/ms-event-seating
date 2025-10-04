package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.Discount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiscountRepository extends JpaRepository<Discount, UUID> {
    Page<Discount> findAllByEventId(UUID eventId, Pageable pageable);
    Page<Discount> findAllByEventIdAndIsPublic(UUID eventId, boolean isPublic, Pageable pageable);
}
