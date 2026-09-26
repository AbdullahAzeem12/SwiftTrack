package com.swifttrack.app.data.model;

public class Offer {

    public enum OfferType {
        PROMO_CODE,
        ADVANCE_BOOKING_DISCOUNT,
        REWARD_POINTS_REDEMPTION
    }

    public enum DiscountType {
        PERCENTAGE,
        FIXED_AMOUNT,
        CLASS_UPGRADE
    }

    private String offerId;
    private String title;
    private String description;
    private String icon;
    private String promoCode;
    private OfferType offerType;
    private DiscountType discountType;
    private double discountValue;
    private int pointsRequired;
    private int requiredAdvanceDays;
    private String requiredJourneyType;
    private boolean isStackable;
    private int userUsageLimit;
    private int currentUsageCount;
    private boolean enabled;

    public Offer() {}

    public Offer(String offerId, String title, String description, String icon, String promoCode,
                 OfferType offerType, DiscountType discountType, double discountValue,
                 int pointsRequired, int requiredAdvanceDays, String requiredJourneyType,
                 boolean isStackable, int userUsageLimit, int currentUsageCount, boolean enabled) {
        this.offerId = offerId;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.promoCode = promoCode;
        this.offerType = offerType;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.pointsRequired = pointsRequired;
        this.requiredAdvanceDays = requiredAdvanceDays;
        this.requiredJourneyType = requiredJourneyType;
        this.isStackable = isStackable;
        this.userUsageLimit = userUsageLimit;
        this.currentUsageCount = currentUsageCount;
        this.enabled = enabled;
    }

    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public OfferType getOfferType() { return offerType; }
    public void setOfferType(OfferType offerType) { this.offerType = offerType; }

    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public int getPointsRequired() { return pointsRequired; }
    public void setPointsRequired(int pointsRequired) { this.pointsRequired = pointsRequired; }

    public int getRequiredAdvanceDays() { return requiredAdvanceDays; }
    public void setRequiredAdvanceDays(int requiredAdvanceDays) { this.requiredAdvanceDays = requiredAdvanceDays; }

    public String getRequiredJourneyType() { return requiredJourneyType; }
    public void setRequiredJourneyType(String requiredJourneyType) { this.requiredJourneyType = requiredJourneyType; }

    public boolean isStackable() { return isStackable; }
    public void setStackable(boolean stackable) { isStackable = stackable; }

    public int getUserUsageLimit() { return userUsageLimit; }
    public void setUserUsageLimit(int userUsageLimit) { this.userUsageLimit = userUsageLimit; }

    public int getCurrentUsageCount() { return currentUsageCount; }
    public void setCurrentUsageCount(int currentUsageCount) { this.currentUsageCount = currentUsageCount; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
