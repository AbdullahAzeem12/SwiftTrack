package com.swifttrack.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @Column(name = "max_discount_minor")
    private Integer maxDiscountMinor;

    @Column(name = "valid_from", nullable = false)
    private ZonedDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private ZonedDateTime validTo;

    @Column(name = "usage_limit")
    private Integer usageLimit = 1000;

    @Column(name = "times_used", nullable = false)
    private Integer timesUsed = 0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
