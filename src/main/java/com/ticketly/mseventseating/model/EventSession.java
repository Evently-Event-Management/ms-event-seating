package com.ticketly.mseventseating.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import model.SessionStatus;
import model.SessionType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "event_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonBackReference("event-sessions") // Add this
    private Event event;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SessionStatus status = SessionStatus.PENDING;

    // --- Session-Specific Location & Sales Information ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionType sessionType;

    // ✅ ADDED: A JSONB column to store embedded physical venue details.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "venue_details", columnDefinition = "jsonb")
    private String venueDetails; // Stores a JSON string of a VenueDetailsDTO

    // ✅ UPDATED: Simplified to a single sales start time field that the frontend must calculate
    @Column(name = "sales_start_time")
    private OffsetDateTime salesStartTime;

    @OneToOne(mappedBy = "eventSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private SessionSeatingMap sessionSeatingMap;
}