package com.swifttrack.app.data.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.core.util.OperationErrorType;
import com.swifttrack.app.data.model.SupportTicket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SupportRepository {

    private static SupportRepository instance;
    private final FirebaseFirestore firestore;

    private SupportRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized SupportRepository getInstance() {
        if (instance == null) {
            instance = new SupportRepository();
        }
        return instance;
    }

    public interface SupportTicketCallback {
        void onSuccess(String ticketReference);
        void onError(OperationErrorType errorType, String userFriendlyMessage);
    }

    public interface TicketsListener {
        void onTicketsUpdated(List<SupportTicket> tickets);
        void onError(OperationErrorType errorType, String userFriendlyMessage);
    }

    public void createSupportTicket(Context context, SupportTicket ticket, SupportTicketCallback callback) {
        if (context != null && !isNetworkAvailable(context)) {
            callback.onError(OperationErrorType.NETWORK_UNAVAILABLE, OperationErrorType.NETWORK_UNAVAILABLE.getDefaultUserMessage());
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        KeystoreManager keystoreManager = new KeystoreManager(context != null ? context.getApplicationContext() : null);

        if (currentUser == null || currentUser.getUid() == null || currentUser.getUid().trim().isEmpty()) {
            callback.onError(OperationErrorType.AUTH_EXPIRED, OperationErrorType.AUTH_EXPIRED.getDefaultUserMessage());
            return;
        }

        String uid = currentUser.getUid();
        String email = currentUser.getEmail() != null ? currentUser.getEmail() : keystoreManager.getUserEmail();
        String name = keystoreManager.getUserName();

        String refId = generateSupportReference();
        ticket.setTicketId(refId);
        ticket.setUserId(uid);
        ticket.setUserEmail(email);
        ticket.setUserName(name);

        long now = System.currentTimeMillis();

        Map<String, Object> map = new HashMap<>();
        map.put("ticketId", refId);
        map.put("userId", uid);
        map.put("userName", name != null ? name : "");
        map.put("userEmail", email != null ? email : "");
        map.put("category", ticket.getCategory() != null ? ticket.getCategory() : "Booking Issue");
        map.put("subject", ticket.getSubject() != null ? ticket.getSubject() : "");
        map.put("description", ticket.getDescription() != null ? ticket.getDescription() : "");
        map.put("bookingId", ticket.getBookingId() != null ? ticket.getBookingId() : "");
        map.put("status", "OPEN");
        map.put("priority", "NORMAL");
        map.put("createdAt", FieldValue.serverTimestamp());
        map.put("createdAtTimestamp", now);
        map.put("updatedAtTimestamp", now);
        map.put("appVersion", ticket.getAppVersion() != null ? ticket.getAppVersion() : "1.0");
        map.put("platform", "Android");
        map.put("emailNotificationStatus", "PENDING");

        firestore.collection("supportTickets")
                .document(refId)
                .set(map)
                .addOnSuccessListener(aVoid -> callback.onSuccess(refId))
                .addOnFailureListener(e -> {
                    OperationErrorType errType = OperationErrorType.fromException(e);
                    callback.onError(errType, errType.getDefaultUserMessage());
                });
    }

    public ListenerRegistration observeUserTickets(String userId, TicketsListener listener) {
        if (userId == null || userId.trim().isEmpty()) {
            listener.onError(OperationErrorType.AUTH_EXPIRED, OperationErrorType.AUTH_EXPIRED.getDefaultUserMessage());
            return null;
        }

        return firestore.collection("supportTickets")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        OperationErrorType errType = OperationErrorType.fromException(e);
                        listener.onError(errType, errType.getDefaultUserMessage());
                        return;
                    }

                    List<SupportTicket> list = new ArrayList<>();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            SupportTicket ticket = doc.toObject(SupportTicket.class);
                            if (ticket != null) {
                                if (ticket.getTicketId() == null) ticket.setTicketId(doc.getId());
                                list.add(ticket);
                            }
                        }
                    }

                    // Sort descending by createdAtTimestamp
                    list.sort((a, b) -> Long.compare(b.getCreatedAtTimestamp(), a.getCreatedAtTimestamp()));
                    listener.onTicketsUpdated(list);
                });
    }

    private String generateSupportReference() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("ST-");
        Random r = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private boolean isNetworkAvailable(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception ignored) {}
        return true;
    }
}
