package com.swifttrack.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.PaymentIntent;
import com.swifttrack.backend.domain.entity.Booking;
import com.swifttrack.backend.domain.entity.Payment;
import com.swifttrack.backend.domain.entity.PriceQuote;
import com.swifttrack.backend.domain.enums.BookingStatus;
import com.swifttrack.backend.domain.enums.PaymentProvider;
import com.swifttrack.backend.domain.enums.PaymentStatus;
import com.swifttrack.backend.dto.PaymentDto;
import com.swifttrack.backend.exception.ApiException;
import com.swifttrack.backend.repository.*;
import com.swifttrack.backend.service.PaymentService;
import com.swifttrack.backend.service.TicketService;
import com.swifttrack.backend.service.payment.PayPalPaymentGateway;
import com.swifttrack.backend.service.payment.StripePaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentRefundRepository paymentRefundRepository;
    @Mock private PaymentWebhookEventRepository paymentWebhookEventRepository;
    @Mock private IdempotencyRepository idempotencyRepository;
    @Mock private TicketService ticketService;
    @Mock private StripePaymentGateway stripePaymentGateway;
    @Mock private PayPalPaymentGateway payPalPaymentGateway;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PaymentService paymentService;

    private UUID userId;
    private UUID bookingId;
    private Booking sampleBooking;
    private PriceQuote validQuote;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                bookingRepository,
                paymentRepository,
                paymentRefundRepository,
                paymentWebhookEventRepository,
                idempotencyRepository,
                ticketService,
                stripePaymentGateway,
                payPalPaymentGateway,
                objectMapper
        );

        userId = UUID.randomUUID();
        bookingId = UUID.randomUUID();

        validQuote = PriceQuote.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .finalAmountMinor(2500)
                .currency("GBP")
                .expiresAt(ZonedDateTime.now().plusMinutes(10))
                .build();

        sampleBooking = Booking.builder()
                .id(bookingId)
                .bookingReference("ST-TEST-1234")
                .userId(userId)
                .quote(validQuote)
                .status(BookingStatus.QUOTED)
                .totalAmountMinor(2500)
                .currency("GBP")
                .contactEmail("passenger@example.com")
                .build();
    }

    @Test
    void testCreatePaymentSession_Stripe_Success() {
        when(idempotencyRepository.findByIdempotencyKey("idem_key_123")).thenReturn(Optional.empty());
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleBooking));
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_stripe_test_123");
        when(mockIntent.getClientSecret()).thenReturn("secret_stripe_test_123");
        when(stripePaymentGateway.createPaymentIntent(anyLong(), anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(mockIntent);
        when(stripePaymentGateway.getPublishableKey()).thenReturn("pk_test_12345");

        PaymentDto.CreatePaymentSessionRequest req = new PaymentDto.CreatePaymentSessionRequest(
                bookingId,
                "idem_key_123",
                PaymentProvider.STRIPE,
                "GBP"
        );

        PaymentDto.PaymentSessionResponse res = paymentService.createPaymentSession(userId, req);

        assertNotNull(res);
        assertEquals("pi_stripe_test_123", res.getProviderPaymentId());
        assertEquals("secret_stripe_test_123", res.getClientSecret());
        assertEquals("pk_test_12345", res.getPublishableKey());
        assertEquals("GBP", res.getCurrency());
        assertEquals(2500, res.getAmountMinor());
    }

    @Test
    void testCreatePaymentSession_QuoteExpired_ThrowsException() {
        validQuote.setExpiresAt(ZonedDateTime.now().minusMinutes(5));
        when(idempotencyRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleBooking));

        PaymentDto.CreatePaymentSessionRequest req = new PaymentDto.CreatePaymentSessionRequest(
                bookingId,
                "idem_key_expired",
                PaymentProvider.STRIPE,
                "GBP"
        );

        ApiException ex = assertThrows(ApiException.class, () -> paymentService.createPaymentSession(userId, req));
        assertEquals("QUOTE_EXPIRED", ex.getErrorCode());
    }

    @Test
    void testCreatePaymentSession_PayPal_Success() {
        when(idempotencyRepository.findByIdempotencyKey("idem_key_paypal")).thenReturn(Optional.empty());
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(sampleBooking));
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        when(payPalPaymentGateway.createOrder(anyLong(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Map.of(
                        "orderId", "PAYPAL_ORDER_789",
                        "approveUrl", "https://www.sandbox.paypal.com/checkoutnow?token=PAYPAL_ORDER_789",
                        "status", "CREATED"
                ));

        PaymentDto.CreatePaymentSessionRequest req = new PaymentDto.CreatePaymentSessionRequest(
                bookingId,
                "idem_key_paypal",
                PaymentProvider.PAYPAL,
                "USD"
        );

        PaymentDto.PaymentSessionResponse res = paymentService.createPaymentSession(userId, req);

        assertNotNull(res);
        assertEquals(PaymentProvider.PAYPAL, res.getProvider());
        assertEquals("PAYPAL_ORDER_789", res.getProviderOrderId());
        assertEquals("https://www.sandbox.paypal.com/checkoutnow?token=PAYPAL_ORDER_789", res.getPaypalApproveUrl());
    }
}
