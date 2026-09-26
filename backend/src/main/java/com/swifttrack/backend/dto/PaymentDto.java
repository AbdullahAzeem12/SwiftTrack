package com.swifttrack.backend.dto;

import com.swifttrack.backend.domain.enums.PaymentProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

public class PaymentDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePaymentSessionRequest {
        @NotNull
        private UUID bookingId;
        @NotBlank
        private String idempotencyKey;
        private PaymentProvider provider = PaymentProvider.STRIPE;
        private String currency = "GBP"; // GBP or USD
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSessionResponse {
        private UUID paymentId;
        private UUID bookingId;
        private PaymentProvider provider;
        private String providerPaymentId; // Stripe PaymentIntent ID
        private String providerOrderId;   // PayPal Order ID
        private String clientSecret;       // Stripe PaymentIntent client_secret
        private String publishableKey;     // Stripe Publishable Key
        private String paypalApproveUrl;   // PayPal approval link
        private String status;
        private Integer amountMinor;
        private String currency;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayPalCaptureRequest {
        @NotNull
        private UUID bookingId;
        @NotBlank
        private String paypalOrderId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentStatusResponse {
        private UUID paymentId;
        private UUID bookingId;
        private String provider;
        private String status;
        private String providerPaymentId;
        private String providerOrderId;
        private String bookingStatus;
        private String ticketCode;
        private Integer amountMinor;
        private String currency;
        private String failureMessage;
        private ZonedDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRefundRequest {
        @NotNull
        private UUID bookingId;
        private Integer amountMinor; // Nullable for full refund
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundResponse {
        private UUID refundId;
        private UUID bookingId;
        private String providerRefundId;
        private Integer refundAmountMinor;
        private String currency;
        private String status;
        private String reason;
    }
}
