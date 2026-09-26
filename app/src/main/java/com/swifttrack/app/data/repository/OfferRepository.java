package com.swifttrack.app.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.swifttrack.app.core.offers.EligibilityEngine;
import com.swifttrack.app.data.model.Offer;
import com.swifttrack.app.data.model.OfferEligibilityResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfferRepository {

    private static final String TAG = "OfferRepository";
    private static OfferRepository instance;

    private final FirebaseFirestore firestore;
    private final RewardsRepository rewardsRepository;

    private final MutableLiveData<List<Offer>> offersLiveData = new MutableLiveData<>();
    private final Map<String, Offer> offerMap = new HashMap<>();

    private OfferRepository(Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        this.rewardsRepository = RewardsRepository.getInstance(context.getApplicationContext());

        initDefaultOffers();
    }

    public static synchronized OfferRepository getInstance(Context context) {
        if (instance == null) {
            instance = new OfferRepository(context);
        }
        return instance;
    }

    private void initDefaultOffers() {
        List<Offer> list = new ArrayList<>();

        Offer o1 = new Offer(
                "offer_swifttrack20",
                "🏷️ SWIFTTRACK20",
                "Get 20% discount on all Peak & Off-Peak Express fares.",
                "🏷️",
                "SWIFTTRACK20",
                Offer.OfferType.PROMO_CODE,
                Offer.DiscountType.PERCENTAGE,
                20.0,
                0,
                0,
                null,
                false,
                0,
                0,
                true
        );

        Offer o2 = new Offer(
                "offer_airport_return",
                "✈️ Airport Return Special",
                "Save 30% when booking a Return ticket 14 days in advance.",
                "✈️",
                null,
                Offer.OfferType.ADVANCE_BOOKING_DISCOUNT,
                Offer.DiscountType.PERCENTAGE,
                30.0,
                0,
                14,
                "RETURN",
                false,
                0,
                0,
                true
        );

        Offer o3 = new Offer(
                "offer_first_class_upgrade",
                "⭐ First Class Upgrade",
                "Redeem 500 Reward Points for a complimentary First Class Upgrade.",
                "⭐",
                null,
                Offer.OfferType.REWARD_POINTS_REDEMPTION,
                Offer.DiscountType.CLASS_UPGRADE,
                0.0,
                500,
                0,
                null,
                false,
                1,
                0,
                true
        );

        list.add(o1);
        list.add(o2);
        list.add(o3);

        for (Offer o : list) {
            offerMap.put(o.getOfferId(), o);
        }

        offersLiveData.setValue(list);
    }

    public LiveData<List<Offer>> getOffersLiveData() {
        return offersLiveData;
    }

    public Offer getOfferById(String offerId) {
        return offerMap.get(offerId);
    }

    public OfferEligibilityResult evaluateOfferEligibility(
            Offer offer,
            String journeyType,
            long travelDateMillis,
            double eligibleFare,
            int userAvailablePoints
    ) {
        if (offer == null) {
            return new OfferEligibilityResult(false, "INVALID", "Offer not found.", 0, userAvailablePoints, 0, 0.0);
        }
        return EligibilityEngine.evaluate(offer, journeyType, travelDateMillis, eligibleFare, userAvailablePoints, offer.getCurrentUsageCount());
    }

    public interface OfferRedemptionCallback {
        void onSuccess(String redemptionId);
        void onError(String errorMessage);
    }

    /**
     * Atomically redeems 500 Reward Points for First Class Upgrade in Firestore
     */
    public void confirmFirstClassUpgradeRedemption(String userId, String bookingId, OfferRedemptionCallback callback) {
        Offer offer = offerMap.get("offer_first_class_upgrade");
        if (offer == null) {
            callback.onError("Offer not found.");
            return;
        }

        int currentPoints = rewardsRepository.getAvailablePoints(userId);
        if (currentPoints < 500) {
            callback.onError("Insufficient Reward Points. Required: 500, Available: " + currentPoints);
            return;
        }

        // Deduct 500 points via RewardsRepository atomic method
        boolean success = rewardsRepository.confirmRedeemPoints(userId, 500, 0.0, bookingId != null ? bookingId : "upgrade_ref");
        if (success) {
            offer.setCurrentUsageCount(offer.getCurrentUsageCount() + 1);

            String redemptionId = "rdm_fc_" + System.currentTimeMillis();
            Map<String, Object> record = new HashMap<>();
            record.put("redemptionId", redemptionId);
            record.put("userId", userId);
            record.put("offerId", offer.getOfferId());
            record.put("offerTitle", offer.getTitle());
            record.put("type", "OFFER_REDEMPTION");
            record.put("pointsUsed", 500);
            record.put("bookingId", bookingId != null ? bookingId : "");
            record.put("status", "SUCCESS");
            record.put("createdAt", FieldValue.serverTimestamp());

            firestore.collection("Users").document(userId)
                    .collection("OfferRedemptions")
                    .document(redemptionId)
                    .set(record, SetOptions.merge());

            callback.onSuccess(redemptionId);
        } else {
            callback.onError("Failed to process point redemption. Please try again.");
        }
    }
}
