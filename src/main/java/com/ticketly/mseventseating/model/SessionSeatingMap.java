package com.ticketly.mseventseating.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "session_seating_maps")  // Changed from "session_seating_map" to "session_seating_maps"
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionSeatingMap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Changed from OneToOne with Event to OneToOne with EventSession
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_session_id", nullable = false, unique = true)
    private EventSession eventSession;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_data", columnDefinition = "jsonb")
    private String layoutData; // Store the raw JSON string for the event-specific snapshot
}