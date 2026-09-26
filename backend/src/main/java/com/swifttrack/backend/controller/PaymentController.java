package com.swifttrack.backend.controller;

import com.swifttrack.backend.dto.PaymentDto;
import com.swifttrack.backend.security.UserPrincipal;
import com.swifttrack.backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/session")
    public ResponseEntity<PaymentDto.PaymentSessionResponse> createSession(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody PaymentDto.CreatePaymentSessionRequest request) {
        return ResponseEntity.ok(paymentService.createPaymentSession(currentUser.getId(), request));
    }

    @PostMapping("/paypal/capture")
    public ResponseEntity<PaymentDto.PaymentStatusResponse> capturePayPal(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody PaymentDto.PayPalCaptureRequest request) {
        return ResponseEntity.ok(paymentService.capturePayPalPayment(currentUser.getId(), request));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<PaymentDto.PaymentStatusResponse> getStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(currentUser.getId(), id));
    }

    @PostMapping("/refund")
    public ResponseEntity<PaymentDto.RefundResponse> requestRefund(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody PaymentDto.CreateRefundRequest request) {
        return ResponseEntity.ok(paymentService.refundPayment(currentUser.getId(), request));
    }
}
