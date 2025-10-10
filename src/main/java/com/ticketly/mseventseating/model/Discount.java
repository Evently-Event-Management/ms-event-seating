package com.ticketly.mseventseating.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ticketly.mseventseating.model.discount.DiscountParameters;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "discounts",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"event_id", "code"})
        }
)@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonBackReference("event-discounts")
    private Event event;

    @Column(name = "code", nullable = false)
    private String code;


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
    @Builder.Default
    private int currentUsage = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;

    @Column(name = "active_from")
    private OffsetDateTime activeFrom;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    //Discounted sum price given to all users
    @Column(name = "discounted_total", precision = 10, scale = 2)
    private BigDecimal discountedTotal;

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