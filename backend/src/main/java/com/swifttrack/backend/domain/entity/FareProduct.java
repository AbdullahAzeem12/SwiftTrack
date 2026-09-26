package com.swifttrack.backend.domain.entity;

import com.swifttrack.backend.domain.enums.TravelClass;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "fare_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "travel_class", nullable = false)
    private TravelClass travelClass;

    @Column(name = "validity_type", nullable = false)
    private String validityType;

    @Column(name = "base_price_minor", nullable = false)
    private Integer basePriceMinor;

    @Column(nullable = false, length = 3)
    private String currency = "GBP";

    @Column(name = "is_refundable", nullable = false)
    private boolean isRefundable = true;

    @Column(name = "is_changeable", nullable = false)
    private boolean isChangeable = true;

    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
}
