package com.swifttrack.backend.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swifttrack.backend.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PayPalPaymentGateway {

    @Value("${payment.paypal.mode:sandbox}")
    private String mode;

    @Value("${payment.paypal.client-id}")
    private String clientId;

    @Value("${payment.paypal.client-secret}")
    private String clientSecret;

    @Value("${payment.paypal.webhook-id:}")
    private String webhookId;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String cachedAccessToken;
    private Instant tokenExpiryTime = Instant.MIN;

    private String getBaseUrl() {
        return "live".equalsIgnoreCase(mode) ? "https://api-m.paypal.com" : "https://api-m.sandbox.paypal.com";
    }

    private synchronized String getAccessToken() {
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiryTime.minusSeconds(60))) {
            return cachedAccessToken;
        }

        try {
            String credentials = clientId + ":" + clientSecret;
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/v1/oauth2/token"))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Failed to obtain PayPal OAuth token. Status: {}, Body: {}", response.statusCode(), response.body());
                throw new ApiException("PAYPAL_AUTH_ERROR", "Failed to authenticate with PayPal", HttpStatus.BAD_GATEWAY);
            }

            JsonNode root = objectMapper.readTree(response.body());
            cachedAccessToken = root.get("access_token").asText();
            int expiresIn = root.has("expires_in") ? root.get("expires_in").asInt() : 3600;
            tokenExpiryTime = Instant.now().plusSeconds(expiresIn);
            return cachedAccessToken;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayPal token retrieval exception: {}", e.getMessage(), e);
            throw new ApiException("PAYPAL_AUTH_ERROR", "PayPal authentication communication error", HttpStatus.BAD_GATEWAY);
        }
    }

    public Map<String, Object> createOrder(long amountMinor, String currency, String bookingRef, String returnUrl, String cancelUrl) {
        try {
            String token = getAccessToken();
            double amountFormatted = amountMinor / 100.0;
            String valueStr = String.format(java.util.Locale.US, "%.2f", amountFormatted);

            Map<String, Object> amountMap = Map.of(
                    "currency_code", currency.toUpperCase(),
                    "value", valueStr
            );

            Map<String, Object> purchaseUnit = Map.of(
                    "reference_id", bookingRef,
                    "description", "SwiftTrack Rail Ticket " + bookingRef,
                    "custom_id", bookingRef,
                    "amount", amountMap
            );

            Map<String, Object> applicationContext = Map.of(
                    "brand_name", "SwiftTrack",
                    "landing_page", "BILLING",
                    "user_action", "PAY_NOW",
                    "return_url", returnUrl != null ? returnUrl : "swifttrack://paypal-return",
                    "cancel_url", cancelUrl != null ? cancelUrl : "swifttrack://paypal-cancel"
            );

            Map<String, Object> payload = Map.of(
                    "intent", "CAPTURE",
                    "purchase_units", List.of(purchaseUnit),
                    "application_context", applicationContext
            );

            String requestJson = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/v2/checkout/orders"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "return=representation")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("PayPal order creation failed. Status: {}, Body: {}", response.statusCode(), response.body());
                throw new ApiException("PAYPAL_ORDER_CREATION_FAILED", "Failed to create PayPal order", HttpStatus.BAD_GATEWAY);
            }

            JsonNode root = objectMapper.readTree(response.body());
            String orderId = root.get("id").asText();
            String approveUrl = "";

            if (root.has("links")) {
                for (JsonNode link : root.get("links")) {
                    if ("approve".equalsIgnoreCase(link.get("rel").asText())) {
                        approveUrl = link.get("href").asText();
                        break;
                    }
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", orderId);
            result.put("approveUrl", approveUrl);
            result.put("status", root.get("status").asText());
            return result;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayPal createOrder exception: {}", e.getMessage(), e);
            throw new ApiException("PAYPAL_ERROR", "PayPal order initiation failed: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    public Map<String, Object> captureOrder(String orderId) {
        try {
            String token = getAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/v2/checkout/orders/" + orderId + "/capture"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 201) {
                log.error("PayPal capture failed for order {}. Status: {}, Body: {}", orderId, response.statusCode(), response.body());
                throw new ApiException("PAYPAL_CAPTURE_FAILED", "Failed to capture PayPal payment", HttpStatus.BAD_GATEWAY);
            }

            JsonNode root = objectMapper.readTree(response.body());
            String status = root.get("status").asText();

            String captureId = "";
            if (root.has("purchase_units") && root.get("purchase_units").size() > 0) {
                JsonNode payments = root.get("purchase_units").get(0).get("payments");
                if (payments != null && payments.has("captures") && payments.get("captures").size() > 0) {
                    captureId = payments.get("captures").get(0).get("id").asText();
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", orderId);
            result.put("status", status);
            result.put("captureId", captureId);
            return result;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayPal captureOrder exception: {}", e.getMessage(), e);
            throw new ApiException("PAYPAL_ERROR", "PayPal capture processing failed: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    public boolean verifyWebhookSignature(Map<String, String> headers, String payload) {
        try {
            if (webhookId == null || webhookId.isBlank() || webhookId.contains("placeholder")) {
                return true; // Pass in mock/local test environment if not configured
            }

            String token = getAccessToken();

            Map<String, Object> verificationPayload = Map.of(
                    "auth_algo", headers.getOrDefault("paypal-auth-algo", ""),
                    "cert_url", headers.getOrDefault("paypal-cert-url", ""),
                    "transmission_id", headers.getOrDefault("paypal-transmission-id", ""),
                    "transmission_sig", headers.getOrDefault("paypal-transmission-sig", ""),
                    "transmission_time", headers.getOrDefault("paypal-transmission-time", ""),
                    "webhook_id", webhookId,
                    "webhook_event", objectMapper.readTree(payload)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/v1/notifications/verify-webhook-signature"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(verificationPayload)))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode resNode = objectMapper.readTree(response.body());
                return "SUCCESS".equalsIgnoreCase(resNode.get("verification_status").asText());
            }
            return false;
        } catch (Exception e) {
            log.warn("PayPal webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
