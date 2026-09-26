package com.swifttrack.app.data.model;

import java.io.Serializable;
import java.util.Locale;
import java.util.UUID;

public class FareQuote implements Serializable {

    public enum QuoteStatus {
        CREATED,
        ACTIVE,
        EXPIRING,
        EXPIRED,
        REFRESHING,
        CONFIRMED
    }

    public static final long QUOTE_DURATION_MS = 15 * 60 * 1000L; // 15 Minutes (900,000 ms)

    private String quoteId;
    private String bookingId;
    private String userId;
    private double baseFare;
    private String currency;
    private long createdAt;
    private long expiresAt;
    private QuoteStatus status;

    public FareQuote() {
        this.quoteId = "QT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + QUOTE_DURATION_MS;
        this.currency = "GBP";
        this.status = QuoteStatus.ACTIVE;
    }

    public FareQuote(String userId, double baseFare) {
        this();
        this.userId = userId;
        this.baseFare = baseFare;
        this.bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String getQuoteId() { return quoteId; }
    public void setQuoteId(String quoteId) { this.quoteId = quoteId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getBaseFare() { return baseFare; }
    public void setBaseFare(double baseFare) { this.baseFare = baseFare; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public QuoteStatus getStatus() { return status; }
    public void setStatus(QuoteStatus status) { this.status = status; }

    public long getRemainingMillis() {
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    public long getRemainingSeconds() {
        return getRemainingMillis() / 1000L;
    }

    public boolean isExpired() {
        return getRemainingMillis() <= 0L;
    }

    public String getFormattedRemainingTime() {
        long remainingMs = getRemainingMillis();
        long totalSeconds = remainingMs / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
