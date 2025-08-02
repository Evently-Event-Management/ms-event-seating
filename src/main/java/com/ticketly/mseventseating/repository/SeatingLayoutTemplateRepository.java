package com.ticketly.mseventseating.repository;

import com.ticketly.mseventseating.model.Organization;
import com.ticketly.mseventseating.model.SeatingLayoutTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SeatingLayoutTemplateRepository extends JpaRepository<SeatingLayoutTemplate, UUID> {
    List<SeatingLayoutTemplate> findByOrganization(Organization organization);
    List<SeatingLayoutTemplate> findByOrganizationId(UUID organizationId);
    Page<SeatingLayoutTemplate> findByOrganizationId(UUID organizationId, Pageable pageable);
}
