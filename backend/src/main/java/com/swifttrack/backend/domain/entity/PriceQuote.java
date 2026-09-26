package com.swifttrack.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "price_quotes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fare_product_id", nullable = false)
    private FareProduct fareProduct;

    @Column(name = "passenger_count", nullable = false)
    private Integer passengerCount;

    @Column(name = "promo_code")
    private String promoCode;

    @Column(name = "base_amount_minor", nullable = false)
    private Integer baseAmountMinor;

    @Column(name = "discount_amount_minor", nullable = false)
    private Integer discountAmountMinor;

    @Column(name = "final_amount_minor", nullable = false)
    private Integer finalAmountMinor;

    @Column(nullable = false, length = 3)
    private String currency = "GBP";

    @Column(nullable = false, length = 512)
    private String signature;

    @Column(name = "expires_at", nullable = false)
    private ZonedDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
}
