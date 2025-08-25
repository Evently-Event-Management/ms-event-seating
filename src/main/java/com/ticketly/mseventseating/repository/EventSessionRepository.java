package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventSessionRepository extends JpaRepository<EventSession, UUID> {
}
