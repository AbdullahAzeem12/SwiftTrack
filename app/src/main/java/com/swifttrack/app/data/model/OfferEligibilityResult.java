package com.swifttrack.app.data.model;

public class OfferEligibilityResult {

    private boolean isEligible;
    private String statusBadgeText;
    private String reason;
    private int pointsRequired;
    private int pointsAvailable;
    private int pointsShortfall;
    private double calculatedDiscountAmount;

    public OfferEligibilityResult(boolean isEligible, String statusBadgeText, String reason,
                                  int pointsRequired, int pointsAvailable, int pointsShortfall,
                                  double calculatedDiscountAmount) {
        this.isEligible = isEligible;
        this.statusBadgeText = statusBadgeText;
        this.reason = reason;
        this.pointsRequired = pointsRequired;
        this.pointsAvailable = pointsAvailable;
        this.pointsShortfall = Math.max(0, pointsShortfall);
        this.calculatedDiscountAmount = calculatedDiscountAmount;
    }

    public boolean isEligible() { return isEligible; }
    public String getStatusBadgeText() { return statusBadgeText; }
    public String getReason() { return reason; }
    public int getPointsRequired() { return pointsRequired; }
    public int getPointsAvailable() { return pointsAvailable; }
    public int getPointsShortfall() { return pointsShortfall; }
    public double getCalculatedDiscountAmount() { return calculatedDiscountAmount; }
}
