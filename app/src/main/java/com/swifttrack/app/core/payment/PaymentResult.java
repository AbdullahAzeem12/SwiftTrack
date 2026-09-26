package com.swifttrack.app.core.payment;

import java.io.Serializable;

public class PaymentResult implements Serializable {

    public enum Status {
        SUCCEEDED,
        FAILED,
        DECLINED,
        EXPIRED_QUOTE,
        REQUIRES_3DS_ACTION,
        CANCELLED
    }

    private Status status;
    private String providerTransactionId;
    private String errorMessage;
    private String errorCode;

    public PaymentResult(Status status, String providerTransactionId, String errorMessage, String errorCode) {
        this.status = status;
        this.providerTransactionId = providerTransactionId;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public static PaymentResult success(String txId) {
        return new PaymentResult(Status.SUCCEEDED, txId, null, null);
    }

    public static PaymentResult failure(String msg, String code) {
        return new PaymentResult(Status.FAILED, null, msg, code);
    }

    public Status getStatus() { return status; }
    public String getProviderTransactionId() { return providerTransactionId; }
    public String getErrorMessage() { return errorMessage; }
    public String getErrorCode() { return errorCode; }
}
