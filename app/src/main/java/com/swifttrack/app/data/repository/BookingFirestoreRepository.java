package com.swifttrack.app.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.swifttrack.app.data.local.TicketEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BookingFirestoreRepository {

    private static final String TAG = "BookingFirestoreRepo";
    private static BookingFirestoreRepository instance;

    private final FirebaseFirestore firestore;
    private final RewardsRepository rewardsRepository;

    private BookingFirestoreRepository(@NonNull Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        this.rewardsRepository = RewardsRepository.getInstance(context);
    }

    public static synchronized BookingFirestoreRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new BookingFirestoreRepository(context.getApplicationContext());
        }
        return instance;
    }

    public String generateUniqueBookingId() {
        return "ST-BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String generateUniqueTicketId() {
        return "ST-TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public String generateUniqueBookingRef() {
        return "ST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public String resolveActiveUserId() {
        return rewardsRepository.resolveActiveUserId();
    }

    /**
     * Synchronizes temporary booking session selections to Firestore under both:
     * 1. users/{userId}/bookingSessions/{bookingId}
     * 2. bookingSessions/{bookingId} (Top-level collection)
     */
    public void syncBookingSessionToFirestore(
            @NonNull String userId,
            @NonNull String bookingId,
            @NonNull Map<String, Object> bookingData
    ) {
        if (userId.isEmpty() || bookingId.isEmpty()) return;

        Map<String, Object> sessionPayload = new HashMap<>(bookingData);
        sessionPayload.put("bookingId", bookingId);
        sessionPayload.put("userId", userId);
        sessionPayload.put("updatedAt", FieldValue.serverTimestamp());

        if (!sessionPayload.containsKey("createdAt")) {
            sessionPayload.put("createdAt", FieldValue.serverTimestamp());
        }

        if (!sessionPayload.containsKey("expiresAtTimestamp")) {
            long expiresAt = System.currentTimeMillis() + (15 * 60 * 1000L);
            sessionPayload.put("expiresAtTimestamp", expiresAt);
        }

        // 1. User Subcollection: users/{userId}/bookingSessions/{bookingId}
        firestore.collection("users")
                .document(userId)
                .collection("bookingSessions")
                .document(bookingId)
                .set(sessionPayload, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✓ Firestore user booking session synced: " + bookingId))
                .addOnFailureListener(e -> Log.e(TAG, "✕ Failed to sync user booking session: " + e.getMessage(), e));

        // 2. Top-Level Collection: bookingSessions/{bookingId}
        firestore.collection("bookingSessions")
                .document(bookingId)
                .set(sessionPayload, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✓ Firestore top-level booking session synced: " + bookingId))
                .addOnFailureListener(e -> Log.e(TAG, "✕ Failed to sync top-level booking session: " + e.getMessage(), e));

        // 3. User document summary
        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("userId", userId);
        userUpdate.put("lastActiveBookingId", bookingId);
        userUpdate.put("updatedAt", FieldValue.serverTimestamp());
        firestore.collection("users").document(userId).set(userUpdate, SetOptions.merge());
    }

    /**
     * Updates specific booking status (e.g. CHECKOUT, PAYMENT_PENDING, PAID, EXPIRED)
     */
    public void updateBookingStatus(
            @NonNull String userId,
            @NonNull String bookingId,
            @NonNull String bookingStatus,
            @NonNull String paymentStatus
    ) {
        if (userId.isEmpty() || bookingId.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("bookingStatus", bookingStatus);
        updates.put("paymentStatus", paymentStatus);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(userId)
                .collection("bookingSessions")
                .document(bookingId)
                .set(updates, SetOptions.merge());

        firestore.collection("bookingSessions")
                .document(bookingId)
                .set(updates, SetOptions.merge());
    }

    /**
     * Creates permanent confirmed ticket record in Firestore under both:
     * 1. users/{userId}/tickets/{ticketCode}
     * 2. tickets/{ticketCode} (Top-level collection)
     */
    public void createPermanentTicketInFirestore(
            @NonNull String userId,
            @NonNull TicketEntity ticket
    ) {
        createPermanentTicketInFirestore(userId, ticket, null);
    }

    public void createPermanentTicketInFirestore(
            @NonNull String userId,
            @NonNull TicketEntity ticket,
            @Nullable String deliveryEmail
    ) {
        if (userId.isEmpty() || ticket == null) return;

        Map<String, Object> ticketPayload = new HashMap<>();
        ticketPayload.put("ticketId", ticket.ticketId);
        ticketPayload.put("ticketCode", ticket.ticketCode);
        ticketPayload.put("bookingReference", ticket.bookingReference);
        ticketPayload.put("originStationName", ticket.originStationName);
        ticketPayload.put("destStationName", ticket.destStationName);
        ticketPayload.put("departureTime", ticket.departureTime);
        ticketPayload.put("arrivalTime", ticket.arrivalTime);
        ticketPayload.put("travelClass", ticket.travelClass);
        ticketPayload.put("fareProductName", ticket.fareProductName);
        ticketPayload.put("passengerCategory", ticket.passengerCategory);
        ticketPayload.put("signedQrPayload", ticket.signedQrPayload);
        ticketPayload.put("status", "ACTIVE");
        ticketPayload.put("tripType", ticket.tripType);
        ticketPayload.put("returnDate", ticket.returnDate);
        ticketPayload.put("returnDepartureTime", ticket.returnDepartureTime);
        ticketPayload.put("returnArrivalTime", ticket.returnArrivalTime);
        ticketPayload.put("pricePaid", ticket.pricePaid);
        if (deliveryEmail != null && !deliveryEmail.trim().isEmpty()) {
            ticketPayload.put("deliveryEmail", deliveryEmail.trim());
            ticketPayload.put("emailDeliveryStatus", "PENDING");
        }
        ticketPayload.put("createdAt", FieldValue.serverTimestamp());
        ticketPayload.put("issuedTimestamp", System.currentTimeMillis());

        firestore.collection("users")
                .document(userId)
                .collection("tickets")
                .document(ticket.ticketCode)
                .set(ticketPayload, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✓ Permanent ticket saved to user subcollection: " + ticket.ticketCode))
                .addOnFailureListener(e -> Log.e(TAG, "✕ Failed to save user ticket: " + e.getMessage(), e));

        firestore.collection("tickets")
                .document(ticket.ticketCode)
                .set(ticketPayload, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✓ Permanent ticket saved to top-level collection: " + ticket.ticketCode))
                .addOnFailureListener(e -> Log.e(TAG, "✕ Failed to save top-level ticket: " + e.getMessage(), e));
    }

    /**
     * Updates status of permanent ticket in Firestore (e.g., CANCELLED)
     */
    public void updateTicketStatusInFirestore(@NonNull String userId, @NonNull String ticketCode, @NonNull String status) {
        if (userId.isEmpty() || ticketCode.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        firestore.collection("users")
                .document(userId)
                .collection("tickets")
                .document(ticketCode)
                .set(updates, SetOptions.merge());

        firestore.collection("tickets")
                .document(ticketCode)
                .set(updates, SetOptions.merge());
    }

    /**
     * Real-Time Snapshot Listener for active Firestore Booking Session
     */
    public ListenerRegistration listenToBookingSession(
            @NonNull String userId,
            @NonNull String bookingId,
            @NonNull OnBookingSessionUpdatedListener listener
    ) {
        if (userId.isEmpty() || bookingId.isEmpty()) return null;

        return firestore.collection("users")
                .document(userId)
                .collection("bookingSessions")
                .document(bookingId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Snapshot listener failed: " + e.getMessage());
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        listener.onSessionUpdated(snapshot.getData());
                    }
                });
    }

    public interface OnBookingSessionUpdatedListener {
        void onSessionUpdated(Map<String, Object> data);
    }

    public interface OnLiveServicesUpdatedListener {
        void onServicesUpdated(java.util.List<Map<String, Object>> services);
    }

    public ListenerRegistration listenToLiveServices(@NonNull OnLiveServicesUpdatedListener listener) {
        return firestore.collection("liveServices")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "liveServices listener error: " + e.getMessage());
                        return;
                    }
                    if (snapshots != null && !snapshots.isEmpty()) {
                        java.util.List<Map<String, Object>> list = new java.util.ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            if (doc.exists() && doc.getData() != null) {
                                list.add(doc.getData());
                            }
                        }
                        listener.onServicesUpdated(list);
                    }
                });
    }
}
