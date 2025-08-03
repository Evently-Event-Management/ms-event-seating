package com.ticketly.mseventseating.model;

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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String color;
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;
}