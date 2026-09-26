package com.swifttrack.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.swifttrack.backend.domain.entity.*;
import com.swifttrack.backend.domain.enums.BookingStatus;
import com.swifttrack.backend.domain.enums.PaymentProvider;
import com.swifttrack.backend.domain.enums.PaymentStatus;
import com.swifttrack.backend.dto.PaymentDto;
import com.swifttrack.backend.exception.ApiException;
import com.swifttrack.backend.repository.*;
import com.swifttrack.backend.service.payment.PayPalPaymentGateway;
import com.swifttrack.backend.service.payment.StripePaymentGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final TicketService ticketService;
    private final StripePaymentGateway stripePaymentGateway;
    private final PayPalPaymentGateway payPalPaymentGateway;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentDto.PaymentSessionResponse createPaymentSession(UUID userId, PaymentDto.CreatePaymentSessionRequest request) {
        String key = request.getIdempotencyKey();

        // 1. Idempotency Verification
        Optional<IdempotencyRecord> existingKey = idempotencyRepository.findByIdempotencyKey(key);
        if (existingKey.isPresent()) {
            IdempotencyRecord rec = existingKey.get();
            try {
                return objectMapper.readValue(rec.getResponsePayload(), PaymentDto.PaymentSessionResponse.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached idempotency response for key: {}", key);
            }
        }

        // 2. Validate Booking Ownership & State
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ApiException("BOOKING_NOT_FOUND", "Booking not found", HttpStatus.NOT_FOUND));

        if (!booking.getUserId().equals(userId)) {
            throw new ApiException("UNAUTHORIZED_ACCESS", "You do not own this booking", HttpStatus.FORBIDDEN);
        }

        if (booking.getStatus() == BookingStatus.TICKET_ISSUED || booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new ApiException("BOOKING_ALREADY_PAID", "This booking is already paid and confirmed", HttpStatus.CONFLICT);
        }

        // 3. Strict 15-Minute Fare Quote Expiration Verification
        PriceQuote quote = booking.getQuote();
        if (quote != null && quote.getExpiresAt() != null && ZonedDateTime.now().isAfter(quote.getExpiresAt())) {
            throw new ApiException("QUOTE_EXPIRED", "The 15-minute price guarantee for this journey has expired. Please refresh your fare.", HttpStatus.BAD_REQUEST);
        }

        // 4. Currency and Amount Validation
        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank()) 
                ? request.getCurrency().toUpperCase() 
                : booking.getCurrency();

        if (!"GBP".equalsIgnoreCase(currency) && !"USD".equalsIgnoreCase(currency)) {
            throw new ApiException("UNSUPPORTED_CURRENCY", "Supported currencies are GBP and USD only", HttpStatus.BAD_REQUEST);
        }

        int amountMinor = booking.getTotalAmountMinor();
        PaymentProvider provider = request.getProvider() != null ? request.getProvider() : PaymentProvider.STRIPE;

        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseGet(() -> Payment.builder()
                        .booking(booking)
                        .amountMinor(amountMinor)
                        .currency(currency)
                        .provider(provider)
                        .status(PaymentStatus.PENDING)
                        .idempotencyKey(key)
                        .build());

        payment.setProvider(provider);
        payment.setAmountMinor(amountMinor);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(key);

        PaymentDto.PaymentSessionResponse response;

        if (provider == PaymentProvider.STRIPE) {
            Map<String, String> metadata = Map.of(
                    "userId", userId.toString(),
                    "bookingId", booking.getId().toString()
            );

            PaymentIntent intent = stripePaymentGateway.createPaymentIntent(
                    amountMinor,
                    currency,
                    booking.getBookingReference(),
                    key,
                    metadata
            );

            payment.setProviderPaymentId(intent.getId());
            payment.setClientSecretRef(intent.getClientSecret());

            response = PaymentDto.PaymentSessionResponse.builder()
                    .paymentId(payment.getId())
                    .bookingId(booking.getId())
                    .provider(PaymentProvider.STRIPE)
                    .providerPaymentId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .publishableKey(stripePaymentGateway.getPublishableKey())
                    .status(payment.getStatus().name())
                    .amountMinor(amountMinor)
                    .currency(currency)
                    .build();

        } else {
            // PayPal Orders v2 Flow
            Map<String, Object> orderResult = payPalPaymentGateway.createOrder(
                    amountMinor,
                    currency,
                    booking.getBookingReference(),
                    "swifttrack://paypal-return",
                    "swifttrack://paypal-cancel"
            );

            String orderId = (String) orderResult.get("orderId");
            String approveUrl = (String) orderResult.get("approveUrl");

            payment.setProviderOrderId(orderId);

            response = PaymentDto.PaymentSessionResponse.builder()
                    .paymentId(payment.getId())
                    .bookingId(booking.getId())
                    .provider(PaymentProvider.PAYPAL)
                    .providerOrderId(orderId)
                    .paypalApproveUrl(approveUrl)
                    .status(payment.getStatus().name())
                    .amountMinor(amountMinor)
                    .currency(currency)
                    .build();
        }

        payment = paymentRepository.save(payment);
        response.setPaymentId(payment.getId());

        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        bookingRepository.save(booking);

        // Record Idempotency
        try {
            String jsonPayload = objectMapper.writeValueAsString(response);
            IdempotencyRecord rec = IdempotencyRecord.builder()
                    .idempotencyKey(key)
                    .userId(userId)
                    .requestHash(request.getBookingId().toString())
                    .bookingId(booking.getId())
                    .paymentId(payment.getProviderPaymentId() != null ? payment.getProviderPaymentId() : payment.getProviderOrderId())
                    .resultStatus("SUCCESS")
                    .responsePayload(jsonPayload)
                    .expiresAt(ZonedDateTime.now().plusHours(24))
                    .build();
            idempotencyRepository.save(rec);
        } catch (Exception ignored) {}

        return response;
    }

    @Transactional
    public PaymentDto.PaymentStatusResponse capturePayPalPayment(UUID userId, PaymentDto.PayPalCaptureRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ApiException("BOOKING_NOT_FOUND", "Booking not found", HttpStatus.NOT_FOUND));

        if (!booking.getUserId().equals(userId)) {
            throw new ApiException("UNAUTHORIZED_ACCESS", "Unauthorized", HttpStatus.FORBIDDEN);
        }

        Payment payment = paymentRepository.findByProviderOrderId(request.getPaypalOrderId())
                .orElseThrow(() -> new ApiException("PAYMENT_NOT_FOUND", "PayPal payment order not found", HttpStatus.NOT_FOUND));

        Map<String, Object> captureResult = payPalPaymentGateway.captureOrder(request.getPaypalOrderId());
        String status = (String) captureResult.get("status");

        if ("COMPLETED".equalsIgnoreCase(status)) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setCompletedAt(ZonedDateTime.now());
            payment.setProviderPaymentId((String) captureResult.get("captureId"));
            paymentRepository.save(payment);

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            Ticket ticket = ticketService.issueTicketForBooking(booking);

            return PaymentDto.PaymentStatusResponse.builder()
                    .paymentId(payment.getId())
                    .bookingId(booking.getId())
                    .provider(payment.getProvider().name())
                    .status(payment.getStatus().name())
                    .providerPaymentId(payment.getProviderPaymentId())
                    .providerOrderId(payment.getProviderOrderId())
                    .bookingStatus(booking.getStatus().name())
                    .ticketCode(ticket.getTicketCode())
                    .amountMinor(payment.getAmountMinor())
                    .currency(payment.getCurrency())
                    .updatedAt(payment.getUpdatedAt())
                    .build();
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureMessage("PayPal capture returned status: " + status);
            paymentRepository.save(payment);

            throw new ApiException("PAYPAL_CAPTURE_UNSUCCESSFUL", "Payment capture status: " + status, HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public void handleStripeWebhookEvent(String payload, String signatureHeader) {
        Event event = stripePaymentGateway.verifyAndConstructWebhookEvent(payload, signatureHeader);

        // Check for duplicate webhook event (Replay Protection)
        if (paymentWebhookEventRepository.existsByProviderAndEventId(PaymentProvider.STRIPE, event.getId())) {
            log.info("Duplicate Stripe webhook event ignored: {}", event.getId());
            return;
        }

        PaymentWebhookEvent webhookEvent = PaymentWebhookEvent.builder()
                .provider(PaymentProvider.STRIPE)
                .eventId(event.getId())
                .eventType(event.getType())
                .payload(payload)
                .signatureVerified(true)
                .processingStatus("PROCESSED")
                .build();
        paymentWebhookEventRepository.save(webhookEvent);

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (intent != null) {
                processStripePaymentSuccess(intent);
            }
        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (intent != null) {
                processStripePaymentFailure(intent);
            }
        }
    }

    @Transactional
    public void handlePayPalWebhookEvent(Map<String, String> headers, String payload) {
        boolean isValid = payPalPaymentGateway.verifyWebhookSignature(headers, payload);
        if (!isValid) {
            throw new ApiException("INVALID_WEBHOOK_SIGNATURE", "PayPal webhook signature verification failed", HttpStatus.FORBIDDEN);
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventId = root.get("id").asText();
            String eventType = root.get("event_type").asText();

            if (paymentWebhookEventRepository.existsByProviderAndEventId(PaymentProvider.PAYPAL, eventId)) {
                log.info("Duplicate PayPal webhook event ignored: {}", eventId);
                return;
            }

            PaymentWebhookEvent webhookEvent = PaymentWebhookEvent.builder()
                    .provider(PaymentProvider.PAYPAL)
                    .eventId(eventId)
                    .eventType(eventType)
                    .payload(payload)
                    .signatureVerified(true)
                    .processingStatus("PROCESSED")
                    .build();
            paymentWebhookEventRepository.save(webhookEvent);

            if ("PAYMENT.CAPTURE.COMPLETED".equalsIgnoreCase(eventType)) {
                JsonNode resource = root.get("resource");
                String captureId = resource.has("id") ? resource.get("id").asText() : "";
                String customId = resource.has("custom_id") ? resource.get("custom_id").asText() : "";

                Optional<Payment> paymentOpt = paymentRepository.findByProviderPaymentId(captureId);
                if (paymentOpt.isEmpty() && !customId.isBlank()) {
                    paymentOpt = bookingRepository.findByBookingReference(customId)
                            .flatMap(b -> paymentRepository.findByBookingId(b.getId()));
                }

                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
                        payment.setStatus(PaymentStatus.SUCCEEDED);
                        payment.setCompletedAt(ZonedDateTime.now());
                        paymentRepository.save(payment);

                        Booking booking = payment.getBooking();
                        booking.setStatus(BookingStatus.CONFIRMED);
                        bookingRepository.save(booking);

                        ticketService.issueTicketForBooking(booking);
                    }
                }
            }
        } catch (Exception e) {
            log.error("PayPal webhook JSON processing failed: {}", e.getMessage(), e);
        }
    }

    private void processStripePaymentSuccess(PaymentIntent intent) {
        Payment payment = paymentRepository.findByProviderPaymentId(intent.getId())
                .orElse(null);

        if (payment == null) {
            log.warn("Payment record not found for Stripe intent: {}", intent.getId());
            return;
        }

        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setCompletedAt(ZonedDateTime.now());

            if (intent.getCharges() != null && intent.getCharges().getData() != null && !intent.getCharges().getData().isEmpty()) {
                Charge charge = intent.getCharges().getData().get(0);
                if (charge.getPaymentMethodDetails() != null && charge.getPaymentMethodDetails().getCard() != null) {
                    payment.setPaymentMethodBrand(charge.getPaymentMethodDetails().getCard().getBrand());
                    payment.setPaymentMethodLast4(charge.getPaymentMethodDetails().getCard().getLast4());
                }
            }

            paymentRepository.save(payment);

            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            ticketService.issueTicketForBooking(booking);
            log.info("Payment and ticket issuance completed for booking reference: {}", booking.getBookingReference());
        }
    }

    private void processStripePaymentFailure(PaymentIntent intent) {
        Payment payment = paymentRepository.findByProviderPaymentId(intent.getId())
                .orElse(null);

        if (payment != null) {
            payment.setStatus(PaymentStatus.FAILED);
            if (intent.getLastPaymentError() != null) {
                payment.setFailureCode(intent.getLastPaymentError().getCode());
                payment.setFailureMessage(intent.getLastPaymentError().getMessage());
            }
            paymentRepository.save(payment);

            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.DRAFT);
            bookingRepository.save(booking);
        }
    }

    public PaymentDto.PaymentStatusResponse getPaymentStatus(UUID userId, UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException("PAYMENT_NOT_FOUND", "Payment not found", HttpStatus.NOT_FOUND));

        Booking booking = payment.getBooking();
        if (!booking.getUserId().equals(userId)) {
            throw new ApiException("UNAUTHORIZED_ACCESS", "Unauthorized", HttpStatus.FORBIDDEN);
        }

        Ticket ticket = ticketService.getTicketForBooking(booking.getId());

        return PaymentDto.PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .bookingId(booking.getId())
                .provider(payment.getProvider() != null ? payment.getProvider().name() : "STRIPE")
                .status(payment.getStatus().name())
                .providerPaymentId(payment.getProviderPaymentId())
                .providerOrderId(payment.getProviderOrderId())
                .bookingStatus(booking.getStatus().name())
                .ticketCode(ticket != null ? ticket.getTicketCode() : null)
                .amountMinor(payment.getAmountMinor())
                .currency(payment.getCurrency())
                .failureMessage(payment.getFailureMessage())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    @Transactional
    public PaymentDto.RefundResponse refundPayment(UUID userId, PaymentDto.CreateRefundRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ApiException("BOOKING_NOT_FOUND", "Booking not found", HttpStatus.NOT_FOUND));

        Payment payment = paymentRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ApiException("PAYMENT_NOT_FOUND", "Payment not found", HttpStatus.NOT_FOUND));

        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new ApiException("INVALID_REFUND_STATE", "Only succeeded payments can be refunded", HttpStatus.BAD_REQUEST);
        }

        Integer refundAmount = request.getAmountMinor() != null ? request.getAmountMinor() : payment.getAmountMinor();
        String providerRefundId;

        if (payment.getProvider() == PaymentProvider.STRIPE) {
            var refund = stripePaymentGateway.refundPayment(payment.getProviderPaymentId(), refundAmount, request.getReason());
            providerRefundId = refund.getId();
        } else {
            // PayPal refund simulation / call
            providerRefundId = "pyr_" + UUID.randomUUID().toString().substring(0, 12);
        }

        PaymentRefund refundEntity = PaymentRefund.builder()
                .booking(booking)
                .payment(payment)
                .provider(payment.getProvider())
                .providerRefundId(providerRefundId)
                .refundAmountMinor(refundAmount)
                .currency(payment.getCurrency())
                .reason(request.getReason())
                .status("COMPLETED")
                .processedAt(ZonedDateTime.now())
                .build();
        paymentRefundRepository.save(refundEntity);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        booking.setStatus(BookingStatus.REFUNDED);
        bookingRepository.save(booking);

        return PaymentDto.RefundResponse.builder()
                .refundId(refundEntity.getId())
                .bookingId(booking.getId())
                .providerRefundId(providerRefundId)
                .refundAmountMinor(refundAmount)
                .currency(payment.getCurrency())
                .status("COMPLETED")
                .reason(request.getReason())
                .build();
    }
}
