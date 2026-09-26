package com.swifttrack.backend.repository;

import com.swifttrack.backend.domain.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
    List<PaymentRefund> findByBookingId(UUID bookingId);
    List<PaymentRefund> findByPaymentId(UUID paymentId);
    Optional<PaymentRefund> findByProviderRefundId(String providerRefundId);
}
