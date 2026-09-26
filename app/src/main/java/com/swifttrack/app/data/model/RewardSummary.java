package com.swifttrack.app.data.model;

public class RewardSummary {
    private String userId;
    private int pointsBalance;
    private int lifetimeEarned;
    private int lifetimeSpent;
    private int reservedPoints;
    private long lastUpdated;

    public RewardSummary() {}

    public RewardSummary(String userId, int pointsBalance, int lifetimeEarned, int lifetimeSpent, int reservedPoints, long lastUpdated) {
        this.userId = userId;
        this.pointsBalance = Math.max(0, pointsBalance);
        this.lifetimeEarned = Math.max(0, lifetimeEarned);
        this.lifetimeSpent = Math.max(0, lifetimeSpent);
        this.reservedPoints = Math.max(0, reservedPoints);
        this.lastUpdated = lastUpdated;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getPointsBalance() {
        return Math.max(0, pointsBalance);
    }
    public void setPointsBalance(int pointsBalance) {
        this.pointsBalance = Math.max(0, pointsBalance);
    }

    public int getLifetimeEarned() { return lifetimeEarned; }
    public void setLifetimeEarned(int lifetimeEarned) { this.lifetimeEarned = lifetimeEarned; }

    public int getLifetimeSpent() { return lifetimeSpent; }
    public void setLifetimeSpent(int lifetimeSpent) { this.lifetimeSpent = lifetimeSpent; }

    public int getReservedPoints() { return reservedPoints; }
    public void setReservedPoints(int reservedPoints) { this.reservedPoints = reservedPoints; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
}
