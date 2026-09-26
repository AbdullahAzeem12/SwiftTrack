package com.swifttrack.app.data.remote.dto;

import java.util.UUID;

public class PaymentDto {

    public static class CreatePaymentSessionRequest {
        private UUID bookingId;
        private String idempotencyKey;
        private String provider = "STRIPE";
        private String currency = "GBP";

        public CreatePaymentSessionRequest() {}

        public CreatePaymentSessionRequest(UUID bookingId, String idempotencyKey, String provider, String currency) {
            this.bookingId = bookingId;
            this.idempotencyKey = idempotencyKey;
            this.provider = provider;
            this.currency = currency;
        }

        public UUID getBookingId() { return bookingId; }
        public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

    public static class PayPalCaptureRequest {
        private UUID bookingId;
        private String paypalOrderId;

        public PayPalCaptureRequest() {}

        public PayPalCaptureRequest(UUID bookingId, String paypalOrderId) {
            this.bookingId = bookingId;
            this.paypalOrderId = paypalOrderId;
        }

        public UUID getBookingId() { return bookingId; }
        public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

        public String getPaypalOrderId() { return paypalOrderId; }
        public void setPaypalOrderId(String paypalOrderId) { this.paypalOrderId = paypalOrderId; }
    }

    public static class PaymentSessionResponse {
        private UUID paymentId;
        private UUID bookingId;
        private String provider;
        private String providerPaymentId;
        private String providerOrderId;
        private String clientSecret;
        private String publishableKey;
        private String paypalApproveUrl;
        private String status;
        private Integer amountMinor;
        private String currency;

        public PaymentSessionResponse() {}

        public UUID getPaymentId() { return paymentId; }
        public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

        public UUID getBookingId() { return bookingId; }
        public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getProviderPaymentId() { return providerPaymentId; }
        public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }

        public String getProviderOrderId() { return providerOrderId; }
        public void setProviderOrderId(String providerOrderId) { this.providerOrderId = providerOrderId; }

        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

        public String getPublishableKey() { return publishableKey; }
        public void setPublishableKey(String publishableKey) { this.publishableKey = publishableKey; }

        public String getPaypalApproveUrl() { return paypalApproveUrl; }
        public void setPaypalApproveUrl(String paypalApproveUrl) { this.paypalApproveUrl = paypalApproveUrl; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getAmountMinor() { return amountMinor; }
        public void setAmountMinor(Integer amountMinor) { this.amountMinor = amountMinor; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }

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
        private String updatedAt;

        public PaymentStatusResponse() {}

        public UUID getPaymentId() { return paymentId; }
        public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

        public UUID getBookingId() { return bookingId; }
        public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getProviderPaymentId() { return providerPaymentId; }
        public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }

        public String getProviderOrderId() { return providerOrderId; }
        public void setProviderOrderId(String providerOrderId) { this.providerOrderId = providerOrderId; }

        public String getBookingStatus() { return bookingStatus; }
        public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }

        public String getTicketCode() { return ticketCode; }
        public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }

        public Integer getAmountMinor() { return amountMinor; }
        public void setAmountMinor(Integer amountMinor) { this.amountMinor = amountMinor; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getFailureMessage() { return failureMessage; }
        public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }
}
