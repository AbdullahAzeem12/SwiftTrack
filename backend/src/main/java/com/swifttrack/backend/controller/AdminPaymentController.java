package com.swifttrack.backend.controller;

import com.swifttrack.backend.domain.entity.Payment;
import com.swifttrack.backend.domain.entity.PaymentRefund;
import com.swifttrack.backend.domain.entity.PayoutRecord;
import com.swifttrack.backend.domain.enums.PaymentProvider;
import com.swifttrack.backend.repository.PaymentRefundRepository;
import com.swifttrack.backend.repository.PaymentRepository;
import com.swifttrack.backend.repository.PayoutRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OPERATIONS_MANAGER', 'FARE_MANAGER')")
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final PayoutRecordRepository payoutRecordRepository;

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentRepository.findAll());
    }

    @GetMapping("/refunds")
    public ResponseEntity<List<PaymentRefund>> getAllRefunds() {
        return ResponseEntity.ok(paymentRefundRepository.findAll());
    }

    @GetMapping("/payouts")
    public ResponseEntity<List<PayoutRecord>> getAllPayouts(@RequestParam(required = false) PaymentProvider provider) {
        if (provider != null) {
            return ResponseEntity.ok(payoutRecordRepository.findByProvider(provider));
        }
        return ResponseEntity.ok(payoutRecordRepository.findAll());
    }
}
