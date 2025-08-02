package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.SeatingLayoutTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SeatingLayoutTemplateRepository extends JpaRepository<SeatingLayoutTemplate, UUID> {
    Page<SeatingLayoutTemplate> findByOrganizationId(UUID organizationId, Pageable pageable);
    
    // Add this new method to count templates by organization
    long countByOrganizationId(UUID organizationId);
}
