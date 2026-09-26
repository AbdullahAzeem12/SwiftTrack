package com.swifttrack.backend.service.payment;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.swifttrack.backend.exception.ApiException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StripePaymentGateway {

    @Value("${payment.stripe.secret-key}")
    private String secretKey;

    @Getter
    @Value("${payment.stripe.publishable-key}")
    private String publishableKey;

    @Value("${payment.stripe.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isBlank() && !secretKey.contains("placeholder")) {
            Stripe.apiKey = secretKey;
        }
    }

    public PaymentIntent createPaymentIntent(long amountMinor, String currency, String bookingRef, String idempotencyKey, Map<String, String> customMetadata) {
        try {
            Map<String, String> metadata = new HashMap<>(customMetadata != null ? customMetadata : Map.of());
            metadata.put("bookingReference", bookingRef);
            metadata.put("system", "SwiftTrack");

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountMinor)
                    .setCurrency(currency.toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .setDescription("SwiftTrack Rail Ticket: " + bookingRef);

            metadata.forEach(paramsBuilder::putMetadata);

            RequestOptions options = RequestOptions.builder()
                    .setApiKey(secretKey)
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            return PaymentIntent.create(paramsBuilder.build(), options);
        } catch (StripeException e) {
            log.error("Stripe createPaymentIntent failed for booking {}: {}", bookingRef, e.getMessage(), e);
            throw new ApiException("STRIPE_ERROR", "Payment initialization failed: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    public Refund refundPayment(String paymentIntentId, Integer amountMinor, String reason) {
        try {
            RefundCreateParams.Builder params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId);

            if (amountMinor != null && amountMinor > 0) {
                params.setAmount(Long.valueOf(amountMinor));
            }
            if (reason != null && !reason.isBlank()) {
                params.putMetadata("reason", reason);
            }

            RequestOptions options = RequestOptions.builder()
                    .setApiKey(secretKey)
                    .build();

            return Refund.create(params.build(), options);
        } catch (StripeException e) {
            log.error("Stripe refund failed for payment intent {}: {}", paymentIntentId, e.getMessage(), e);
            throw new ApiException("STRIPE_REFUND_ERROR", "Refund processing failed: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    public Event verifyAndConstructWebhookEvent(String payload, String signatureHeader) {
        try {
            if (webhookSecret == null || signatureHeader == null) {
                throw new ApiException("WEBHOOK_VERIFICATION_FAILED", "Missing signature or webhook secret", HttpStatus.FORBIDDEN);
            }
            return Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook cryptographic signature check failed: {}", e.getMessage());
            throw new ApiException("INVALID_WEBHOOK_SIGNATURE", "Stripe webhook signature verification failed", HttpStatus.FORBIDDEN);
        }
    }
}
