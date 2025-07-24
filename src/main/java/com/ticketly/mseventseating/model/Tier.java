package com.ticketly.mseventseating.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
public class Tier {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;
    private String color;
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    // getters and setters
}
