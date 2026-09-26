package com.swifttrack.backend.controller;

import com.swifttrack.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = true) String sigHeader) {
        log.info("Received Stripe webhook event notification");
        paymentService.handleStripeWebhookEvent(payload, sigHeader);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/paypal")
    public ResponseEntity<Void> handlePayPalWebhook(
            @RequestBody String payload,
            @RequestHeader Map<String, String> headers) {
        log.info("Received PayPal webhook event notification");
        paymentService.handlePayPalWebhookEvent(headers, payload);
        return ResponseEntity.ok().build();
    }
}
