package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.Tier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TierRepository extends JpaRepository<Tier, UUID> {
    List<Tier> findByEventId(UUID eventId);
}
