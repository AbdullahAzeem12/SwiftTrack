package com.swifttrack.app.core.offers;

import com.swifttrack.app.data.model.Offer;
import com.swifttrack.app.data.model.OfferEligibilityResult;

import java.util.concurrent.TimeUnit;

public class EligibilityEngine {

    public static OfferEligibilityResult evaluate(
            Offer offer,
            String journeyType,
            long travelDateMillis,
            double eligibleFare,
            int availablePoints,
            int userUsageCount
    ) {
        if (offer == null || !offer.isEnabled()) {
            return new OfferEligibilityResult(
                    false,
                    "DISABLED",
                    "Offer is currently disabled or unavailable.",
                    0, availablePoints, 0, 0.0
            );
        }

        // 1. Check Usage Limit
        if (offer.getUserUsageLimit() > 0 && userUsageCount >= offer.getUserUsageLimit()) {
            return new OfferEligibilityResult(
                    false,
                    "REDEEMED",
                    "You have already redeemed this offer (Limit: " + offer.getUserUsageLimit() + " per member).",
                    offer.getPointsRequired(), availablePoints, 0, 0.0
            );
        }

        // 2. Check Required Points for Redemption Offers
        if (offer.getPointsRequired() > 0) {
            if (availablePoints < offer.getPointsRequired()) {
                int shortfall = offer.getPointsRequired() - availablePoints;
                String reason = "Not enough Reward Points.\n• Your balance: " + availablePoints + " Points\n• Required: " + offer.getPointsRequired() + " Points\n• Earn " + shortfall + " more points to unlock this offer.";
                return new OfferEligibilityResult(
                        false,
                        "INSUFFICIENT POINTS",
                        reason,
                        offer.getPointsRequired(), availablePoints, shortfall, 0.0
                );
            }
        }

        // 3. Check Required Journey Type (e.g. Return ticket required)
        if (offer.getRequiredJourneyType() != null && !offer.getRequiredJourneyType().isEmpty()) {
            if (journeyType == null || !offer.getRequiredJourneyType().equalsIgnoreCase(journeyType)) {
                String reason = "Offer requires a Return ticket purchase.\n• Your selection: " + (journeyType != null ? journeyType : "Single") + "\n• Required: " + offer.getRequiredJourneyType() + " Journey";
                return new OfferEligibilityResult(
                        false,
                        "NOT ELIGIBLE",
                        reason,
                        offer.getPointsRequired(), availablePoints, 0, 0.0
                );
            }
        }

        // 4. Check Required Advance Booking Days (e.g. 14+ days advance)
        if (offer.getRequiredAdvanceDays() > 0) {
            long now = System.currentTimeMillis();
            long diffMillis = travelDateMillis - now;
            long advanceDays = diffMillis > 0 ? TimeUnit.MILLISECONDS.toDays(diffMillis) : 0;

            if (advanceDays < offer.getRequiredAdvanceDays()) {
                String reason = "Offer requires booking at least " + offer.getRequiredAdvanceDays() + " days in advance.\n• Your booking: " + advanceDays + " days advance\n• Required: " + offer.getRequiredAdvanceDays() + "+ days advance";
                return new OfferEligibilityResult(
                        false,
                        "NOT ELIGIBLE",
                        reason,
                        offer.getPointsRequired(), availablePoints, 0, 0.0
                );
            }
        }

        // 5. Calculate Discount Amount
        double calculatedDiscount = 0.0;
        if (offer.getDiscountType() == Offer.DiscountType.PERCENTAGE) {
            calculatedDiscount = Math.round((eligibleFare * (offer.getDiscountValue() / 100.0)) * 100.0) / 100.0;
        } else if (offer.getDiscountType() == Offer.DiscountType.FIXED_AMOUNT) {
            calculatedDiscount = Math.min(eligibleFare, offer.getDiscountValue());
        }

        return new OfferEligibilityResult(
                true,
                "ELIGIBLE",
                "✓ You qualify for this offer!",
                offer.getPointsRequired(), availablePoints, 0, calculatedDiscount
        );
    }
}
