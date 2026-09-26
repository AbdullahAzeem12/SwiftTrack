package com.swifttrack.app.presentation.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.swifttrack.app.data.local.StationEntity;
import com.swifttrack.app.data.model.FareQuote;
import com.swifttrack.app.data.repository.LiveTransitRepository;
import com.swifttrack.app.data.repository.RewardsRepository;
import com.swifttrack.app.data.repository.StationRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeBookViewModel extends AndroidViewModel {

    private static String generateTodayFormattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM ''yy", Locale.UK);
        Calendar cal = Calendar.getInstance();
        return dateFormat.format(cal.getTime()) + " (Today)";
    }

    public static String generateDefaultTomorrowFormattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM ''yy", Locale.UK);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        return dateFormat.format(cal.getTime()) + " (Tomorrow)";
    }

    private static String generateReturnFormattedDate() {
        return generateDefaultTomorrowFormattedDate();
    }

    private final StationRepository stationRepository;
    private final LiveTransitRepository liveTransitRepository;
    private final RewardsRepository rewardsRepository;
    private final com.swifttrack.app.data.repository.BookingFirestoreRepository bookingFirestoreRepository;

    private final MutableLiveData<StationEntity> originStation = new MutableLiveData<>();
    private final MutableLiveData<StationEntity> destinationStation = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isReturnJourney = new MutableLiveData<>(false);

    // Detailed Passenger Composition
    private final MutableLiveData<Integer> adultCount = new MutableLiveData<>(1);
    private final MutableLiveData<Integer> childCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> infantCount = new MutableLiveData<>(0);
    private final MutableLiveData<String> passengerSummary = new MutableLiveData<>("1 Adult");

    private final MutableLiveData<String> selectedClass = new MutableLiveData<>("STANDARD");
    private final MutableLiveData<String> promoCode = new MutableLiveData<>("");
    private final MutableLiveData<String> travelDate = new MutableLiveData<>(generateTodayFormattedDate());
    private final MutableLiveData<String> returnDate = new MutableLiveData<>(generateReturnFormattedDate());
    private final MutableLiveData<String> returnDepartureTime = new MutableLiveData<>("18:10 PM");
    private final MutableLiveData<String> returnArrivalTime = new MutableLiveData<>("18:25 PM");

    // Live Fare Engine Fares
    private final MutableLiveData<Double> standardBaseFare = new MutableLiveData<>(25.00);
    private final MutableLiveData<Double> standardDiscountedFare = new MutableLiveData<>(22.50);

    private final MutableLiveData<Double> premierBaseFare = new MutableLiveData<>(32.00);
    private final MutableLiveData<Double> premierDiscountedFare = new MutableLiveData<>(28.80);

    // Dynamic Checkout Promo & Rewards State Engine
    private final MutableLiveData<String> activePromoCode = new MutableLiveData<>("");
    private final MutableLiveData<Double> promoDiscountAmount = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> rewardPointsToUse = new MutableLiveData<>(0);
    private final MutableLiveData<Double> rewardDiscountAmount = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> finalPayableTotal = new MutableLiveData<>(25.00);

    // Real-Time Fare Quote Model Engine
    private final MutableLiveData<FareQuote> currentFareQuote = new MutableLiveData<>();

    public HomeBookViewModel(@NonNull Application application) {
        super(application);
        this.stationRepository = new StationRepository(application);
        this.liveTransitRepository = LiveTransitRepository.getInstance(application);
        this.rewardsRepository = RewardsRepository.getInstance(application);
        this.bookingFirestoreRepository = com.swifttrack.app.data.repository.BookingFirestoreRepository.getInstance(application);

        StationEntity pad = new StationEntity("station_pad", "PAD", "London Paddington", "London", false, "", 51.5167, -0.1756);
        StationEntity lhr = new StationEntity("station_lhr", "LHR", "Heathrow Airport", "London", true, "HWV", 51.4700, -0.4543);
        originStation.setValue(pad);
        destinationStation.setValue(lhr);

        this.stationRepository.refreshStations();
        recalculateFares();
        refreshLiveRailData(false);
    }

    public LiveData<List<StationEntity>> getStationsLiveData() { return stationRepository.getLocalStationsLiveData(); }
    public LiveData<StationEntity> getOriginStation() { return originStation; }
    public LiveData<StationEntity> getDestinationStation() { return destinationStation; }
    public LiveData<Boolean> getIsReturnJourney() { return isReturnJourney; }
    public LiveData<Integer> getAdultCount() { return adultCount; }
    public LiveData<Integer> getChildCount() { return childCount; }
    public LiveData<Integer> getInfantCount() { return infantCount; }
    public LiveData<String> getPassengerSummary() { return passengerSummary; }
    public LiveData<String> getSelectedClass() { return selectedClass; }
    public LiveData<String> getPromoCode() { return promoCode; }
    public LiveData<String> getTravelDate() { return travelDate; }
    public LiveData<String> getReturnDate() { return returnDate; }
    public LiveData<String> getReturnDepartureTime() { return returnDepartureTime; }
    public LiveData<String> getReturnArrivalTime() { return returnArrivalTime; }

    // Shared TransportAPI / Darwin Live State Getters
    public LiveData<LiveTransitRepository.LiveDepartureInfo> getOutboundLiveDeparture() {
        return liveTransitRepository.getOutboundLiveDeparture();
    }

    public LiveData<LiveTransitRepository.LiveDepartureInfo> getReturnLiveDeparture() {
        return liveTransitRepository.getReturnLiveDeparture();
    }

    public LiveData<LiveTransitRepository.RailLineStatus> getRailLineStatus() {
        return liveTransitRepository.getRailLineStatus();
    }

    public LiveData<String> getDynamicFrequencySummary() {
        return liveTransitRepository.getDynamicFrequencySummary();
    }

    public LiveData<LiveTransitRepository.LiveFreshnessInfo> getFreshnessInfo() {
        return liveTransitRepository.getFreshnessInfo();
    }

    public void reconcileDepartures() {
        liveTransitRepository.reconcileUpcomingDepartures();
    }

    public void refreshLiveRailData(boolean forceRefresh) {
        String origCode = originStation.getValue() != null ? originStation.getValue().code : "PAD";
        String destCode = destinationStation.getValue() != null ? destinationStation.getValue().code : "LHR";
        liveTransitRepository.refreshSharedLiveRailData(origCode, destCode, null, forceRefresh, null);
    }

    public LiveData<Double> getStandardBaseFare() { return standardBaseFare; }
    public LiveData<Double> getStandardDiscountedFare() { return standardDiscountedFare; }
    public LiveData<Double> getPremierBaseFare() { return premierBaseFare; }
    public LiveData<Double> getPremierDiscountedFare() { return premierDiscountedFare; }

    public LiveData<String> getActivePromoCode() { return activePromoCode; }
    public LiveData<Double> getPromoDiscountAmount() { return promoDiscountAmount; }
    public LiveData<Integer> getRewardPointsToUse() { return rewardPointsToUse; }
    public LiveData<Double> getRewardDiscountAmount() { return rewardDiscountAmount; }
    public LiveData<Double> getFinalPayableTotal() { return finalPayableTotal; }
    public LiveData<FareQuote> getCurrentFareQuote() { return currentFareQuote; }

    public void setOriginStation(StationEntity station) {
        originStation.setValue(station);
        recalculateFares();
    }

    public void setDestinationStation(StationEntity station) {
        destinationStation.setValue(station);
        recalculateFares();
    }

    public void swapStations() {
        StationEntity orig = originStation.getValue();
        StationEntity dest = destinationStation.getValue();
        originStation.setValue(dest);
        destinationStation.setValue(orig);
        recalculateFares();
    }

    public void setReturnJourney(boolean isReturn) {
        isReturnJourney.setValue(isReturn);
        if (isReturn && (returnDate.getValue() == null || returnDate.getValue().isEmpty())) {
            returnDate.setValue(generateDefaultTomorrowFormattedDate());
        }
        recalculateFares();
        syncCurrentBookingToFirestore();
    }

    public void setPassengers(int adults, int children, int infants) {
        int safeAdults = Math.max(1, adults);
        int safeChildren = Math.max(0, children);
        int safeInfants = Math.max(0, infants);

        adultCount.setValue(safeAdults);
        childCount.setValue(safeChildren);
        infantCount.setValue(safeInfants);

        StringBuilder sb = new StringBuilder();
        sb.append(safeAdults).append(safeAdults == 1 ? " Adult" : " Adults");
        if (safeChildren > 0) {
            sb.append(", ").append(safeChildren).append(safeChildren == 1 ? " Child" : " Children");
        }
        if (safeInfants > 0) {
            sb.append(", ").append(safeInfants).append(safeInfants == 1 ? " Infant" : " Infants");
        }
        passengerSummary.setValue(sb.toString());
        recalculateFares();
    }

    public void setSelectedClass(String travelClass) {
        selectedClass.setValue(travelClass);
        recalculateCheckoutPricing();
    }

    public void setPromoCode(String code) {
        promoCode.setValue(code);
        if (rewardsRepository.isValidPromoCode(code)) {
            activePromoCode.setValue(code.trim().toUpperCase());
        }
        recalculateFares();
    }

    public boolean applyCheckoutPromo(String code) {
        if (rewardsRepository.isValidPromoCode(code)) {
            activePromoCode.setValue(code.trim().toUpperCase());
            recalculateCheckoutPricing();
            return true;
        }
        return false;
    }

    public void removeCheckoutPromo() {
        activePromoCode.setValue("");
        promoDiscountAmount.setValue(0.0);
        recalculateCheckoutPricing();
    }

    public void applyCheckoutRewards(int points) {
        rewardPointsToUse.setValue(Math.max(0, points));
        recalculateCheckoutPricing();
    }

    public void removeCheckoutRewards() {
        rewardPointsToUse.setValue(0);
        rewardDiscountAmount.setValue(0.0);
        recalculateCheckoutPricing();
    }

    public void setTravelDate(String date) {
        travelDate.setValue(date);
        recalculateCheckoutPricing();
        syncCurrentBookingToFirestore();
    }

    public void setReturnDate(String date) {
        returnDate.setValue(date);
        recalculateCheckoutPricing();
        syncCurrentBookingToFirestore();
    }

    public void setReturnTime(String depTime, String arrTime) {
        if (depTime != null && !depTime.isEmpty()) {
            returnDepartureTime.setValue(depTime);
        }
        if (arrTime != null && !arrTime.isEmpty()) {
            returnArrivalTime.setValue(arrTime);
        }
        syncCurrentBookingToFirestore();
    }

    /**
     * Ensures an active, valid fare quote object with absolute expiration timestamp
     */
    public FareQuote ensureActiveFareQuote(String userId) {
        FareQuote q = currentFareQuote.getValue();
        if (q == null || q.isExpired()) {
            boolean isPremier = "PREMIER".equalsIgnoreCase(selectedClass.getValue());
            Double base = isPremier ? premierBaseFare.getValue() : standardBaseFare.getValue();
            if (base == null) base = isPremier ? 34.00 : 25.00;

            q = new FareQuote(userId, base);
            currentFareQuote.setValue(q);
        }
        return q;
    }

    /**
     * Refreshes the fare quote with a new 15-minute expiration timestamp and revalidates live fares
     */
    public void refreshFareQuote(String userId) {
        recalculateFares();
        boolean isPremier = "PREMIER".equalsIgnoreCase(selectedClass.getValue());
        Double base = isPremier ? premierBaseFare.getValue() : standardBaseFare.getValue();
        if (base == null) base = isPremier ? 34.00 : 25.00;

        FareQuote newQuote = new FareQuote(userId, base);
        currentFareQuote.setValue(newQuote);
        recalculateCheckoutPricing();
    }

    /**
     * State-Driven Checkout Pricing Calculation Engine:
     * Order of operations:
     * Base Subtotal -> Promo Discount (SWIFTTRACK20 20%) -> Remaining -> Rewards Discount -> Final Total (min £0.00)
     */
    public void recalculateCheckoutPricing() {
        boolean isPremier = "PREMIER".equalsIgnoreCase(selectedClass.getValue());
        Double base = isPremier ? premierBaseFare.getValue() : standardBaseFare.getValue();
        if (base == null) base = isPremier ? 34.00 : 25.00;

        String promo = activePromoCode.getValue() != null ? activePromoCode.getValue().trim() : "";
        double promoDisc = 0.0;
        if (rewardsRepository.isValidPromoCode(promo)) {
            promoDisc = rewardsRepository.calculatePromoDiscount(base, promo);
        }
        promoDiscountAmount.setValue(promoDisc);

        double fareAfterPromo = Math.max(0.0, base - promoDisc);

        int pts = rewardPointsToUse.getValue() != null ? rewardPointsToUse.getValue() : 0;
        double rwdDisc = rewardsRepository.calculateRewardDiscount(pts);
        if (rwdDisc > fareAfterPromo) {
            rwdDisc = fareAfterPromo;
            pts = rewardsRepository.calculateMaxPointsForDiscount(fareAfterPromo);
            rewardPointsToUse.setValue(pts);
        }
        rewardDiscountAmount.setValue(rwdDisc);

        double total = Math.max(0.00, fareAfterPromo - rwdDisc);
        finalPayableTotal.setValue(Math.round(total * 100.0) / 100.0);
    }

    /**
     * Explicit User Action Trigger:
     * Saves all user-selected journey options and ticket class to Firestore
     * ONLY after the user selects options and explicitly clicks a ticket option (Standard or Premier).
     */
    public void onTicketOptionSelected(String travelClass) {
        setSelectedClass(travelClass);
        recalculateCheckoutPricing();
        syncCurrentBookingToFirestore();
    }

    /**
     * Synchronizes full current booking selection to Firestore under users/{userId}/bookingSessions/{bookingId}
     */
    public void syncCurrentBookingToFirestore() {
        if (bookingFirestoreRepository == null) return;

        FareQuote quote = currentFareQuote.getValue();
        String userId = bookingFirestoreRepository.resolveActiveUserId();
        if (quote == null) {
            quote = ensureActiveFareQuote(userId);
        }
        String bookingId = quote != null ? quote.getBookingId() : bookingFirestoreRepository.generateUniqueBookingId();

        StationEntity orig = originStation.getValue();
        StationEntity dest = destinationStation.getValue();
        boolean isReturn = Boolean.TRUE.equals(isReturnJourney.getValue());
        String travelD = travelDate.getValue() != null ? travelDate.getValue() : generateTodayFormattedDate();
        String returnD = returnDate.getValue() != null ? returnDate.getValue() : generateDefaultTomorrowFormattedDate();
        String selClass = selectedClass.getValue() != null ? selectedClass.getValue() : "STANDARD";

        int adults = adultCount.getValue() != null ? adultCount.getValue() : 1;
        int children = childCount.getValue() != null ? childCount.getValue() : 0;
        int infants = infantCount.getValue() != null ? infantCount.getValue() : 0;
        int totalPax = adults + children + infants;

        double base = "PREMIER".equalsIgnoreCase(selClass) ?
                (premierBaseFare.getValue() != null ? premierBaseFare.getValue() : 32.0) :
                (standardBaseFare.getValue() != null ? standardBaseFare.getValue() : 25.0);

        double promoDisc = promoDiscountAmount.getValue() != null ? promoDiscountAmount.getValue() : 0.0;
        int ptsUsed = rewardPointsToUse.getValue() != null ? rewardPointsToUse.getValue() : 0;
        double rwdDisc = rewardDiscountAmount.getValue() != null ? rewardDiscountAmount.getValue() : 0.0;
        double finalAmount = finalPayableTotal.getValue() != null ? finalPayableTotal.getValue() : Math.max(0.0, base - promoDisc - rwdDisc);

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("fromStation", orig != null ? orig.name : "Heathrow Airport");
        data.put("fromStationCode", orig != null ? orig.code : "LHR");
        data.put("toStation", dest != null ? dest.name : "London Paddington");
        data.put("toStationCode", dest != null ? dest.code : "PAD");
        data.put("ticketType", isReturn ? "RETURN" : "SINGLE");
        data.put("isReturn", isReturn);
        data.put("outboundDate", travelD);
        if (isReturn) {
            data.put("returnDate", returnD);
            data.put("returnDepartureTime", returnDepartureTime.getValue() != null ? returnDepartureTime.getValue() : "18:10 PM");
            data.put("returnArrivalTime", returnArrivalTime.getValue() != null ? returnArrivalTime.getValue() : "18:25 PM");
        }
        data.put("adults", adults);
        data.put("children", children);
        data.put("infants", infants);
        data.put("passengerCount", totalPax);
        data.put("passengerSummary", passengerSummary.getValue() != null ? passengerSummary.getValue() : "1 Adult");
        data.put("selectedClass", selClass);
        data.put("basePrice", base);
        data.put("currency", "GBP");
        data.put("promoCode", activePromoCode.getValue() != null ? activePromoCode.getValue() : "");
        data.put("promoDiscount", promoDisc);
        data.put("rewardPointsUsed", ptsUsed);
        data.put("rewardDiscount", rwdDisc);
        data.put("finalAmount", finalAmount);
        data.put("bookingStatus", "DRAFT");
        data.put("paymentStatus", "PENDING");
        data.put("expiresAtTimestamp", quote != null ? quote.getExpiresAt() : (System.currentTimeMillis() + 15 * 60 * 1000L));

        bookingFirestoreRepository.syncBookingSessionToFirestore(userId, bookingId, data);
    }

    /**
     * Real-Time Live API Fare Engine Synchronization
     */
    public void recalculateFares() {
        int adults = adultCount.getValue() != null ? adultCount.getValue() : 1;
        int children = childCount.getValue() != null ? childCount.getValue() : 0;
        boolean isReturn = Boolean.TRUE.equals(isReturnJourney.getValue());
        String code = promoCode.getValue() != null ? promoCode.getValue().trim() : "";

        String originCode = originStation.getValue() != null ? originStation.getValue().code : "PAD";
        String destCode = destinationStation.getValue() != null ? destinationStation.getValue().code : "LHR";

        liveTransitRepository.fetchLiveFaresApiAsync(originCode, destCode, adults, children, isReturn, code, new LiveTransitRepository.LiveApiCallback<LiveTransitRepository.LiveFareInfo>() {
            @Override
            public void onSuccess(LiveTransitRepository.LiveFareInfo fareInfo) {
                if (fareInfo != null) {
                    standardBaseFare.setValue(fareInfo.standardBaseFare);
                    standardDiscountedFare.setValue(fareInfo.standardDiscountedFare);
                    premierBaseFare.setValue(fareInfo.premierBaseFare);
                    premierDiscountedFare.setValue(fareInfo.premierDiscountedFare);
                    recalculateCheckoutPricing();
                }
            }

            @Override
            public void onError(Exception e) {
                recalculateCheckoutPricing();
            }
        });
    }
}
