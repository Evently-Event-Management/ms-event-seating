package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.Discount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiscountRepository extends JpaRepository<Discount, UUID> {
}
