package com.swifttrack.backend.repository;

import com.swifttrack.backend.domain.entity.PaymentWebhookEvent;
import com.swifttrack.backend.domain.enums.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, UUID> {
    Optional<PaymentWebhookEvent> findByEventId(String eventId);
    Optional<PaymentWebhookEvent> findByProviderAndEventId(PaymentProvider provider, String eventId);
    boolean existsByProviderAndEventId(PaymentProvider provider, String eventId);
}
