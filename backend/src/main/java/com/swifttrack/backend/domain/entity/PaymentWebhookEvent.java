package com.swifttrack.backend.domain.entity;

import com.swifttrack.backend.domain.enums.PaymentProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_webhook_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentProvider provider = PaymentProvider.STRIPE;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "signature_verified", nullable = false)
    @Builder.Default
    private Boolean signatureVerified = true;

    @Column(name = "payload_hash", length = 128)
    private String payloadHash;

    @Column(name = "processing_status", nullable = false, length = 50)
    @Builder.Default
    private String processingStatus = "PROCESSED";

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private ZonedDateTime processedAt;
}
