package com.swifttrack.app.core.payment;

import java.io.Serializable;

public class PaymentRequest implements Serializable {

    private String paymentAttemptId;
    private String bookingId;
    private String quoteId;
    private String userId;
    private long amountMinor;
    private CurrencyConfig currencyConfig;
    private String firstName;
    private String lastName;
    private String cardNumberMasked;
    private String paymentToken; // PCI-Compliant token (e.g. pm_1N...)
    private boolean isSandbox;
    private boolean saveCard;

    public PaymentRequest(String paymentAttemptId, String bookingId, String quoteId, String userId, long amountMinor, CurrencyConfig currencyConfig, String firstName, String lastName, String cardNumberMasked, String paymentToken, boolean isSandbox, boolean saveCard) {
        this.paymentAttemptId = paymentAttemptId;
        this.bookingId = bookingId;
        this.quoteId = quoteId;
        this.userId = userId;
        this.amountMinor = amountMinor;
        this.currencyConfig = currencyConfig;
        this.firstName = firstName;
        this.lastName = lastName;
        this.cardNumberMasked = cardNumberMasked;
        this.paymentToken = paymentToken;
        this.isSandbox = isSandbox;
        this.saveCard = saveCard;
    }

    public String getPaymentAttemptId() { return paymentAttemptId; }
    public String getBookingId() { return bookingId; }
    public String getQuoteId() { return quoteId; }
    public String getUserId() { return userId; }
    public long getAmountMinor() { return amountMinor; }
    public CurrencyConfig getCurrencyConfig() { return currencyConfig; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getCardNumberMasked() { return cardNumberMasked; }
    public String getPaymentToken() { return paymentToken; }
    public boolean isSandbox() { return isSandbox; }
    public boolean isSaveCard() { return saveCard; }
}
