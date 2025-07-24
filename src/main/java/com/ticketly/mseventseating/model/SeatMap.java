package com.ticketly.mseventseating.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "seat_map")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatMap {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String seatName; // e.g., A1, A2

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne
    @JoinColumn(name = "tier_id")
    private Tier tier;

    @Enumerated(EnumType.STRING)
    private SeatStatus status; // AVAILABLE, RESERVED, BOOKED
}