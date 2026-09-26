package com.swifttrack.app.data.model;

import java.io.Serializable;

public class RewardTransaction implements Serializable {

    public enum TransactionType {
        EARN,
        REDEEM,
        REFUND
    }

    private String rewardTransactionId;
    private String userId;
    private String bookingId;
    private TransactionType type;
    private int points;
    private double monetaryValue;
    private long timestamp;
    private String status; // "COMPLETED", "PENDING", "CANCELLED"

    public RewardTransaction() {}

    public RewardTransaction(String rewardTransactionId, String userId, String bookingId, TransactionType type, int points, double monetaryValue, long timestamp, String status) {
        this.rewardTransactionId = rewardTransactionId;
        this.userId = userId;
        this.bookingId = bookingId;
        this.type = type;
        this.points = points;
        this.monetaryValue = monetaryValue;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getRewardTransactionId() { return rewardTransactionId; }
    public void setRewardTransactionId(String rewardTransactionId) { this.rewardTransactionId = rewardTransactionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public double getMonetaryValue() { return monetaryValue; }
    public void setMonetaryValue(double monetaryValue) { this.monetaryValue = monetaryValue; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
