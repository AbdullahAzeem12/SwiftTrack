package com.swifttrack.app.core.payment;

public interface PaymentGateway {
    interface PaymentCallback {
        void onSuccess(PaymentResult result);
        void onError(Exception e);
    }

    void processPayment(PaymentRequest request, PaymentCallback callback);
}
