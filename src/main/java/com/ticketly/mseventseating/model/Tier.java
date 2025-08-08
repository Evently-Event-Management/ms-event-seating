package com.ticketly.mseventseating.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tiers")  // Added table name explicitly
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tier {

    @Id
    private UUID id;

    private String name;
    private String color;
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonBackReference("event-tiers")
    private Event event;
}