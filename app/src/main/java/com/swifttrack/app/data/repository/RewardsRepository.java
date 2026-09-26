package com.swifttrack.app.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.data.model.RewardSummary;
import com.swifttrack.app.data.model.RewardTransaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RewardsRepository {

    private static final String TAG = "RewardsRepository";
    private static final String PREF_NAME = "swifttrack_rewards_prefs";

    public static final String VALID_PROMO_CODE = "SWIFTTRACK20";
    public static final double PROMO_DISCOUNT_RATE = 0.20; // 20% discount
    public static final double EARN_RATE_PER_POUND = 1.0;  // 1 point per £1 spend
    public static final int POINTS_PER_POUND_DISCOUNT = 100; // 100 points = £1.00 discount

    private static RewardsRepository instance;
    private final Context appContext;
    private final SharedPreferences prefs;
    private final FirebaseFirestore firestore;
    private final KeystoreManager keystoreManager;
    private final Set<String> processedBookingIds = new HashSet<>();

    private final MutableLiveData<RewardSummary> rewardSummaryLiveData = new MutableLiveData<>();
    private ListenerRegistration firestoreListenerRegistration;
    private String activeObservedUserId;

    private RewardsRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.firestore = FirebaseFirestore.getInstance();
        this.keystoreManager = new KeystoreManager(appContext);

        String initialUid = resolveActiveUserId();
        initDefaultSummary(initialUid);
        startObservingUserRewards(initialUid);
    }

    public static synchronized RewardsRepository getInstance(Context context) {
        if (instance == null) {
            instance = new RewardsRepository(context);
        }
        return instance;
    }

    public LiveData<RewardSummary> getRewardSummaryLiveData() {
        String currentUid = resolveActiveUserId();
        if (!currentUid.equals(activeObservedUserId)) {
            startObservingUserRewards(currentUid);
        }
        return rewardSummaryLiveData;
    }

    public String resolveActiveUserId() {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser != null && fUser.getUid() != null && !fUser.getUid().isEmpty()) {
            return fUser.getUid();
        }
        String storedUid = keystoreManager.getUserUid();
        if (storedUid != null && !storedUid.isEmpty()) {
            return storedUid;
        }
        String storedEmail = keystoreManager.getUserEmail();
        if (storedEmail != null && !storedEmail.isEmpty()) {
            return "user_" + Math.abs(storedEmail.hashCode());
        }
        return "demo_user_default";
    }

    private void initDefaultSummary(String userId) {
        int avail = prefs.getInt("points_" + userId, 250);
        int lifetime = prefs.getInt("lifetime_points_" + userId, 500);
        int spent = prefs.getInt("spent_points_" + userId, 250);
        int reserved = prefs.getInt("reserved_points_" + userId, 0);

        RewardSummary summary = new RewardSummary(userId, avail, lifetime, spent, reserved, System.currentTimeMillis());
        rewardSummaryLiveData.setValue(summary);
    }

    public synchronized void startObservingUserRewards(String userId) {
        if (userId == null || userId.isEmpty()) return;

        if (firestoreListenerRegistration != null && userId.equals(activeObservedUserId)) {
            return;
        }

        if (firestoreListenerRegistration != null) {
            firestoreListenerRegistration.remove();
            firestoreListenerRegistration = null;
        }

        activeObservedUserId = userId;
        initDefaultSummary(userId);

        firestoreListenerRegistration = firestore.collection("Users").document(userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Reward snapshot error: " + error.getMessage());
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        parseAndApplyFirestoreSnapshot(userId, snapshot);
                    } else {
                        // Check lowercase 'users' collection fallback
                        firestore.collection("users").document(userId).get().addOnSuccessListener(fallbackSnap -> {
                            if (fallbackSnap != null && fallbackSnap.exists()) {
                                parseAndApplyFirestoreSnapshot(userId, fallbackSnap);
                            }
                        });
                    }
                });
    }

    private void parseAndApplyFirestoreSnapshot(String userId, DocumentSnapshot snapshot) {
        Long pts = snapshot.getLong("rewardPoints");
        Long lifetime = snapshot.getLong("lifetimeRewardPoints");
        Long spent = snapshot.getLong("lifetimeSpentPoints");
        Long reserved = snapshot.getLong("reservedPoints");

        int availVal = pts != null ? pts.intValue() : prefs.getInt("points_" + userId, 250);
        int lifetimeVal = lifetime != null ? lifetime.intValue() : prefs.getInt("lifetime_points_" + userId, 500);
        int spentVal = spent != null ? spent.intValue() : prefs.getInt("spent_points_" + userId, 250);
        int reservedVal = reserved != null ? reserved.intValue() : 0;

        int calculatedAvail = Math.max(0, availVal - reservedVal);

        prefs.edit()
                .putInt("points_" + userId, calculatedAvail)
                .putInt("lifetime_points_" + userId, lifetimeVal)
                .putInt("spent_points_" + userId, spentVal)
                .putInt("reserved_points_" + userId, reservedVal)
                .apply();

        RewardSummary summary = new RewardSummary(userId, calculatedAvail, lifetimeVal, spentVal, reservedVal, System.currentTimeMillis());
        rewardSummaryLiveData.postValue(summary);
    }

    // --- PROMO CODE LOGIC ---

    public boolean isValidPromoCode(String inputCode) {
        if (inputCode == null) return false;
        String trimmed = inputCode.trim();
        return VALID_PROMO_CODE.equalsIgnoreCase(trimmed);
    }

    public double calculatePromoDiscount(double baseSubtotal, String promoCode) {
        if (isValidPromoCode(promoCode) && baseSubtotal > 0) {
            double rawDiscount = baseSubtotal * PROMO_DISCOUNT_RATE;
            return Math.round(rawDiscount * 100.0) / 100.0;
        }
        return 0.0;
    }

    // --- REWARDS POINTS CALCULATIONS & ACTIONS ---

    public int getAvailablePoints(String userId) {
        RewardSummary summary = rewardSummaryLiveData.getValue();
        if (summary != null && summary.getUserId().equals(userId)) {
            return summary.getPointsBalance();
        }
        return prefs.getInt("points_" + userId, 250);
    }

    public int getUserRewardsBalance(String userId) {
        return getAvailablePoints(userId);
    }

    public int getLifetimePoints(String userId) {
        return prefs.getInt("lifetime_points_" + userId, 500);
    }

    public double calculateRewardDiscount(int pointsToUse) {
        if (pointsToUse <= 0) return 0.0;
        double discount = (double) pointsToUse / POINTS_PER_POUND_DISCOUNT;
        return Math.round(discount * 100.0) / 100.0;
    }

    public int calculateMaxPointsForDiscount(double remainingSubtotal) {
        if (remainingSubtotal <= 0) return 0;
        return (int) Math.floor(remainingSubtotal * POINTS_PER_POUND_DISCOUNT);
    }

    /**
     * Reserves points during checkout so they cannot be double-spent.
     */
    public synchronized boolean reservePoints(String userId, int pointsToReserve, String bookingId) {
        if (pointsToReserve <= 0 || bookingId == null) return false;
        int currentAvail = getAvailablePoints(userId);
        if (pointsToReserve > currentAvail) return false;

        int currentReserved = prefs.getInt("reserved_points_" + userId, 0);
        int newReserved = currentReserved + pointsToReserve;
        int newAvail = currentAvail - pointsToReserve;

        prefs.edit()
                .putInt("points_" + userId, newAvail)
                .putInt("reserved_points_" + userId, newReserved)
                .apply();

        RewardSummary summary = new RewardSummary(userId, newAvail, getLifetimePoints(userId), prefs.getInt("spent_points_" + userId, 0), newReserved, System.currentTimeMillis());
        rewardSummaryLiveData.postValue(summary);

        syncRewardStateToFirestore(userId, newAvail, getLifetimePoints(userId), prefs.getInt("spent_points_" + userId, 0), newReserved, null);
        return true;
    }

    /**
     * Releases reserved points back to available balance if checkout is cancelled.
     */
    public synchronized void releaseReservedPoints(String userId, int pointsToRelease, String bookingId) {
        if (pointsToRelease <= 0) return;
        int currentReserved = prefs.getInt("reserved_points_" + userId, 0);
        int newReserved = Math.max(0, currentReserved - pointsToRelease);
        int currentAvail = getAvailablePoints(userId);
        int newAvail = currentAvail + pointsToRelease;

        prefs.edit()
                .putInt("points_" + userId, newAvail)
                .putInt("reserved_points_" + userId, newReserved)
                .apply();

        RewardSummary summary = new RewardSummary(userId, newAvail, getLifetimePoints(userId), prefs.getInt("spent_points_" + userId, 0), newReserved, System.currentTimeMillis());
        rewardSummaryLiveData.postValue(summary);

        syncRewardStateToFirestore(userId, newAvail, getLifetimePoints(userId), prefs.getInt("spent_points_" + userId, 0), newReserved, null);
    }

    /**
     * Confirms and deducts redeemed reward points after payment success. Idempotent.
     */
    public synchronized boolean confirmRedeemPoints(String userId, int pointsToRedeem, double discountAmount, String bookingId) {
        if (pointsToRedeem <= 0 || bookingId == null) return false;
        String idempotencyKey = "redeem_" + bookingId;
        if (processedBookingIds.contains(idempotencyKey)) {
            return false;
        }

        int currentPoints = getAvailablePoints(userId);
        int pointsDeducted = Math.min(pointsToRedeem, currentPoints);

        int currentSpent = prefs.getInt("spent_points_" + userId, 0);
        int newSpent = currentSpent + pointsDeducted;
        int newBalance = currentPoints - pointsDeducted;
        int currentReserved = prefs.getInt("reserved_points_" + userId, 0);
        int newReserved = Math.max(0, currentReserved - pointsDeducted);

        prefs.edit()
                .putInt("points_" + userId, newBalance)
                .putInt("spent_points_" + userId, newSpent)
                .putInt("reserved_points_" + userId, newReserved)
                .apply();

        processedBookingIds.add(idempotencyKey);

        long now = System.currentTimeMillis();
        RewardTransaction tx = new RewardTransaction(
                "tx_rdm_" + now,
                userId,
                bookingId,
                RewardTransaction.TransactionType.REDEEM,
                pointsDeducted,
                discountAmount,
                now,
                "COMPLETED"
        );

        RewardSummary summary = new RewardSummary(userId, newBalance, getLifetimePoints(userId), newSpent, newReserved, now);
        rewardSummaryLiveData.postValue(summary);

        syncRewardStateToFirestore(userId, newBalance, getLifetimePoints(userId), newSpent, newReserved, tx);
        return true;
    }

    /**
     * Awards 1 reward point per £1 spend after successful payment. Idempotent.
     */
    public synchronized int awardPurchaseRewards(String userId, double finalPayableAmount, String bookingId) {
        if (finalPayableAmount <= 0 || bookingId == null) return 0;
        String idempotencyKey = "earn_" + bookingId;
        if (processedBookingIds.contains(idempotencyKey)) {
            return 0;
        }

        int pointsEarned = (int) Math.floor(finalPayableAmount * EARN_RATE_PER_POUND);
        if (pointsEarned <= 0) return 0;

        int currentAvailable = getAvailablePoints(userId);
        int currentLifetime = getLifetimePoints(userId);

        int newAvailable = currentAvailable + pointsEarned;
        int newLifetime = currentLifetime + pointsEarned;

        prefs.edit()
                .putInt("points_" + userId, newAvailable)
                .putInt("lifetime_points_" + userId, newLifetime)
                .apply();

        processedBookingIds.add(idempotencyKey);

        long now = System.currentTimeMillis();
        RewardTransaction tx = new RewardTransaction(
                "tx_earn_" + now,
                userId,
                bookingId,
                RewardTransaction.TransactionType.EARN,
                pointsEarned,
                finalPayableAmount,
                now,
                "COMPLETED"
        );

        RewardSummary summary = new RewardSummary(userId, newAvailable, newLifetime, prefs.getInt("spent_points_" + userId, 0), prefs.getInt("reserved_points_" + userId, 0), now);
        rewardSummaryLiveData.postValue(summary);

        syncRewardStateToFirestore(userId, newAvailable, newLifetime, prefs.getInt("spent_points_" + userId, 0), prefs.getInt("reserved_points_" + userId, 0), tx);
        return pointsEarned;
    }

    public synchronized void refundAsPoints(String userId, int points, String ref) {
        int currentAvailable = getAvailablePoints(userId);
        int currentLifetime = getLifetimePoints(userId);
        int newAvailable = currentAvailable + points;
        int newLifetime = currentLifetime + points;

        prefs.edit()
                .putInt("points_" + userId, newAvailable)
                .putInt("lifetime_points_" + userId, newLifetime)
                .apply();

        long now = System.currentTimeMillis();
        RewardTransaction tx = new RewardTransaction(
                "tx_ref_" + now,
                userId,
                ref,
                RewardTransaction.TransactionType.EARN,
                points,
                0.0,
                now,
                "COMPLETED"
        );

        RewardSummary summary = new RewardSummary(userId, newAvailable, newLifetime, prefs.getInt("spent_points_" + userId, 0), prefs.getInt("reserved_points_" + userId, 0), now);
        rewardSummaryLiveData.postValue(summary);

        syncRewardStateToFirestore(userId, newAvailable, newLifetime, prefs.getInt("spent_points_" + userId, 0), prefs.getInt("reserved_points_" + userId, 0), tx);
    }

    private void syncRewardStateToFirestore(String userId, int availablePoints, int lifetimePoints, int spentPoints, int reservedPoints, RewardTransaction tx) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("rewardPoints", availablePoints);
            data.put("lifetimeRewardPoints", lifetimePoints);
            data.put("lifetimeSpentPoints", spentPoints);
            data.put("reservedPoints", reservedPoints);
            data.put("lastUpdated", FieldValue.serverTimestamp());

            firestore.collection("Users").document(userId)
                    .set(data, SetOptions.merge());

            firestore.collection("users").document(userId)
                    .set(data, SetOptions.merge());

            if (tx != null) {
                firestore.collection("Users").document(userId)
                        .collection("RewardTransactions")
                        .document(tx.getRewardTransactionId())
                        .set(tx);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync rewards to Firestore: " + e.getMessage());
        }
    }
}
