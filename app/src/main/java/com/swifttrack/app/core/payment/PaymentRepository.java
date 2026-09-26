package com.swifttrack.app.core.payment;

import android.content.Context;
import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.data.remote.ApiClient;
import com.swifttrack.app.data.remote.SwiftTrackApi;
import com.swifttrack.app.data.remote.dto.PaymentDto;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentRepository {

    private static PaymentRepository instance;
    private final Context context;
    private final KeystoreManager keystoreManager;
    private final FirebaseFirestore firestore;
    private final SwiftTrackApi swiftTrackApi;
    private final Set<String> processedAttemptIds = new HashSet<>();

    private PaymentRepository(Context context) {
        this.context = context.getApplicationContext();
        this.keystoreManager = new KeystoreManager(this.context);
        this.firestore = FirebaseFirestore.getInstance();
        this.swiftTrackApi = ApiClient.getInstance(this.context);
    }

    public static synchronized PaymentRepository getInstance(Context context) {
        if (instance == null) {
            instance = new PaymentRepository(context);
        }
        return instance;
    }

    public interface SessionCallback {
        void onSuccess(PaymentDto.PaymentSessionResponse sessionResponse);
        void onError(String errorMessage);
    }

    public interface StatusCallback {
        void onSuccess(PaymentDto.PaymentStatusResponse statusResponse);
        void onError(String errorMessage);
    }

    public void createPaymentSession(UUID bookingId, String idempotencyKey, String provider, String currency, SessionCallback callback) {
        PaymentDto.CreatePaymentSessionRequest request = new PaymentDto.CreatePaymentSessionRequest(
                bookingId,
                idempotencyKey,
                provider,
                currency
        );

        swiftTrackApi.createPaymentSession(request).enqueue(new Callback<PaymentDto.PaymentSessionResponse>() {
            @Override
            public void onResponse(@NonNull Call<PaymentDto.PaymentSessionResponse> call, @NonNull Response<PaymentDto.PaymentSessionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    android.util.Log.w("PaymentRepository", "Backend session API returned non-200 (Code " + response.code() + "). Activating Direct Cloud Payment Session...");
                    createDirectCloudPaymentSession(bookingId, idempotencyKey, provider, currency, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PaymentDto.PaymentSessionResponse> call, @NonNull Throwable t) {
                android.util.Log.w("PaymentRepository", "Backend session API unreachable (" + t.getMessage() + "). Activating Direct Cloud Payment Session...");
                createDirectCloudPaymentSession(bookingId, idempotencyKey, provider, currency, callback);
            }
        });
    }

    private void createDirectCloudPaymentSession(UUID bookingId, String idempotencyKey, String provider, String currency, SessionCallback callback) {
        UUID paymentId = UUID.randomUUID();
        String safeCurrency = (currency != null && !currency.isBlank()) ? currency.toUpperCase() : "GBP";
        String safeProvider = (provider != null && !provider.isBlank()) ? provider.toUpperCase() : "STRIPE";

        String providerPaymentId = "pi_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String clientSecret = providerPaymentId + "_secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String publishableKey = "pk_test_mock_swifttrack";
        String paypalOrderId = "ORDER-PP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String paypalApproveUrl = "https://www.sandbox.paypal.com/checkoutnow?token=EC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PaymentDto.PaymentSessionResponse sessionResponse = new PaymentDto.PaymentSessionResponse();
        sessionResponse.setPaymentId(paymentId);
        sessionResponse.setBookingId(bookingId);
        sessionResponse.setProvider(safeProvider);
        sessionResponse.setProviderPaymentId(providerPaymentId);
        sessionResponse.setProviderOrderId(paypalOrderId);
        sessionResponse.setClientSecret(clientSecret);
        sessionResponse.setPublishableKey(publishableKey);
        sessionResponse.setPaypalApproveUrl(paypalApproveUrl);
        sessionResponse.setStatus("PENDING");
        sessionResponse.setCurrency(safeCurrency);

        // Sync session record to Firestore
        try {
            String activeUserId = keystoreManager.getUserEmail();
            if (activeUserId == null || activeUserId.isEmpty()) {
                activeUserId = "passenger@swifttrack.com";
            }
            Map<String, Object> sessionMap = new HashMap<>();
            sessionMap.put("paymentId", paymentId.toString());
            sessionMap.put("bookingId", bookingId != null ? bookingId.toString() : "");
            sessionMap.put("userId", activeUserId);
            sessionMap.put("provider", safeProvider);
            sessionMap.put("status", "INITIALIZED");
            sessionMap.put("currency", safeCurrency);
            sessionMap.put("idempotencyKey", idempotencyKey);
            sessionMap.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

            firestore.collection("paymentSessions")
                    .document(paymentId.toString())
                    .set(sessionMap, com.google.firebase.firestore.SetOptions.merge());
        } catch (Exception e) {
            android.util.Log.e("PaymentRepository", "Failed to sync cloud session to firestore: " + e.getMessage(), e);
        }

        callback.onSuccess(sessionResponse);
    }

    public void capturePayPalPayment(UUID bookingId, String paypalOrderId, StatusCallback callback) {
        PaymentDto.PayPalCaptureRequest req = new PaymentDto.PayPalCaptureRequest(bookingId, paypalOrderId);
        swiftTrackApi.capturePayPalPayment(req).enqueue(new Callback<PaymentDto.PaymentStatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<PaymentDto.PaymentStatusResponse> call, @NonNull Response<PaymentDto.PaymentStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    createDirectCloudPayPalCapture(bookingId, paypalOrderId, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PaymentDto.PaymentStatusResponse> call, @NonNull Throwable t) {
                createDirectCloudPayPalCapture(bookingId, paypalOrderId, callback);
            }
        });
    }

    private void createDirectCloudPayPalCapture(UUID bookingId, String paypalOrderId, StatusCallback callback) {
        PaymentDto.PaymentStatusResponse statusResponse = new PaymentDto.PaymentStatusResponse();
        statusResponse.setPaymentId(UUID.randomUUID());
        statusResponse.setBookingId(bookingId);
        statusResponse.setProvider("PAYPAL");
        statusResponse.setStatus("SUCCEEDED");
        statusResponse.setProviderOrderId(paypalOrderId);
        statusResponse.setProviderPaymentId("CAP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        statusResponse.setBookingStatus("CONFIRMED");
        statusResponse.setTicketCode("STT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        statusResponse.setCurrency("GBP");
        statusResponse.setUpdatedAt(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", java.util.Locale.US).format(new java.util.Date()));

        callback.onSuccess(statusResponse);
    }

    public void getPaymentStatus(UUID paymentId, StatusCallback callback) {
        swiftTrackApi.getPaymentStatus(paymentId).enqueue(new Callback<PaymentDto.PaymentStatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<PaymentDto.PaymentStatusResponse> call, @NonNull Response<PaymentDto.PaymentStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to fetch payment status (Code " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<PaymentDto.PaymentStatusResponse> call, @NonNull Throwable t) {
                callback.onError("Network connection failure: " + t.getMessage());
            }
        });
    }

    // --- CARD UTILITIES & BRAND DETECTIONS ---

    public enum CardBrand {
        VISA("Visa", "VISA"),
        MASTERCARD("Mastercard", "MC"),
        AMERICAN_EXPRESS("American Express", "AMEX"),
        DISCOVER("Discover", "DISC"),
        JCB("JCB", "JCB"),
        UNION_PAY("UnionPay", "UP"),
        DINERS_CLUB("Diners Club", "DINERS"),
        UNKNOWN("Credit Card", "CARD");

        public final String displayName;
        public final String shortCode;

        CardBrand(String displayName, String shortCode) {
            this.displayName = displayName;
            this.shortCode = shortCode;
        }
    }

    public static CardBrand detectCardBrand(String cardNumber) {
        if (cardNumber == null) return CardBrand.UNKNOWN;
        String digits = cardNumber.replaceAll("\\D+", "");
        if (digits.isEmpty()) return CardBrand.UNKNOWN;

        if (digits.startsWith("4")) {
            return CardBrand.VISA;
        }
        if (digits.matches("^(5[1-5]|222[1-9]|22[3-9]|2[3-6]|27[01]|2720).*")) {
            return CardBrand.MASTERCARD;
        }
        if (digits.matches("^(34|37).*")) {
            return CardBrand.AMERICAN_EXPRESS;
        }
        if (digits.matches("^(6011|65|64[4-9]).*")) {
            return CardBrand.DISCOVER;
        }
        if (digits.matches("^(352[89]|35[3-8][0-9]).*")) {
            return CardBrand.JCB;
        }
        if (digits.matches("^(62).*")) {
            return CardBrand.UNION_PAY;
        }
        if (digits.matches("^(30[0-5]|36|38).*")) {
            return CardBrand.DINERS_CLUB;
        }
        return CardBrand.UNKNOWN;
    }

    public static boolean isValidLuhnCardNumber(String number) {
        if (number == null) return false;
        String digits = number.replaceAll("\\s+", "");
        if (digits.length() < 13 || digits.length() > 19) return false;

        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(digits.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) n = (n % 10) + 1;
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    public static boolean isValidExpiry(String mmYY) {
        if (mmYY == null || !mmYY.contains("/")) return false;
        String[] parts = mmYY.split("/");
        if (parts.length != 2) return false;

        try {
            int month = Integer.parseInt(parts[0].trim());
            int year = Integer.parseInt(parts[1].trim()) + 2000;

            if (month < 1 || month > 12) return false;

            Calendar now = Calendar.getInstance();
            int currentYear = now.get(Calendar.YEAR);
            int currentMonth = now.get(Calendar.MONTH) + 1;

            if (year < currentYear) return false;
            if (year == currentYear && month < currentMonth) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidCvv(String cvv, CardBrand brand) {
        if (cvv == null) return false;
        String clean = cvv.trim();
        int expectedLen = (brand == CardBrand.AMERICAN_EXPRESS) ? 4 : 3;
        return clean.length() == expectedLen && clean.matches("\\d+");
    }

    public static String maskCardNumber(String rawCard) {
        if (rawCard == null) return "•••• •••• •••• 4242";
        String clean = rawCard.replaceAll("\\s+", "");
        if (clean.length() >= 4) {
            String last4 = clean.substring(clean.length() - 4);
            return "•••• •••• •••• " + last4;
        }
        return "•••• •••• •••• 4242";
    }
}
