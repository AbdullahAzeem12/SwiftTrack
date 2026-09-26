package com.swifttrack.backend.repository;

import com.swifttrack.backend.domain.entity.PayoutRecord;
import com.swifttrack.backend.domain.enums.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayoutRecordRepository extends JpaRepository<PayoutRecord, UUID> {
    Optional<PayoutRecord> findByProviderPayoutId(String providerPayoutId);
    List<PayoutRecord> findByProvider(PaymentProvider provider);
    List<PayoutRecord> findByStatus(String status);
}
