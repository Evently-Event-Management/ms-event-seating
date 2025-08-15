package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.SessionSeatingMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SessionSeatingMapRepository extends JpaRepository<SessionSeatingMap, UUID> {
    // Additional query methods can be defined here if needed
}
