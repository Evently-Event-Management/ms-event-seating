package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {
    List<Venue> findByOrganizationId(UUID organizationId);
    List<Venue> findByOrganizationUserId(String userId);
}
