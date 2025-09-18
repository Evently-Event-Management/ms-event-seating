package com.ticketly.mseventseating.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ticketly.mseventseating.model.discount.DiscountParameters;
import com.ticketly.mseventseating.model.discount.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonBackReference("event-discounts") // Add this
    private Event event;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    /**
     * The discriminator column. This tells our code how to interpret the 'parameters' field.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private DiscountType type;

    /**
     * The JSONB column.
     * The @Type annotation from hypersistence-utils tells Hibernate to map this column
     * to our DiscountParameters interface using Jackson for serialization.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private DiscountParameters parameters;

    @Column(name = "max_usage")
    private Integer maxUsage;

    @Column(name = "current_usage", nullable = false)
    private int currentUsage = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "active_from")
    private OffsetDateTime activeFrom;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @ManyToMany
    @JoinTable(
        name = "discount_tiers",
        joinColumns = @JoinColumn(name = "discount_id"),
        inverseJoinColumns = @JoinColumn(name = "tier_id")
    )
    private List<Tier> applicableTiers;


    @ManyToMany
    @JoinTable(
            name = "discount_sessions",
            joinColumns = @JoinColumn(name = "discount_id"),
            inverseJoinColumns = @JoinColumn(name = "session_id")
    )
    private List<EventSession> applicableSessions;
}