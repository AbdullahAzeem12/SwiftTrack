package com.swifttrack.app.data.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.core.util.OperationErrorType;
import com.swifttrack.app.data.model.FeedbackItem;

import java.util.HashMap;
import java.util.Map;

public class FeedbackRepository {

    private static FeedbackRepository instance;
    private final FirebaseFirestore firestore;

    private FeedbackRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized FeedbackRepository getInstance() {
        if (instance == null) {
            instance = new FeedbackRepository();
        }
        return instance;
    }

    public interface FeedbackCallback {
        void onSuccess(String feedbackId);
        void onError(OperationErrorType errorType, String userFriendlyMessage);
    }

    public void submitFeedback(Context context, FeedbackItem item, FeedbackCallback callback) {
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

        item.setUserId(uid);
        item.setUserEmail(email);
        item.setUserName(name);

        String feedbackId = "FB-" + System.currentTimeMillis();
        item.setFeedbackId(feedbackId);

        Map<String, Object> map = new HashMap<>();
        map.put("feedbackId", item.getFeedbackId());
        map.put("userId", item.getUserId());
        map.put("userEmail", item.getUserEmail() != null ? item.getUserEmail() : "");
        map.put("userName", item.getUserName() != null ? item.getUserName() : "");
        map.put("rating", item.getRating());
        map.put("category", item.getCategory() != null ? item.getCategory() : "General Feedback");
        map.put("message", item.getMessage() != null ? item.getMessage() : "");
        map.put("recommend", item.getRecommend() != null ? item.getRecommend() : "YES");
        map.put("appVersion", item.getAppVersion() != null ? item.getAppVersion() : "1.0");
        map.put("platform", "Android");
        map.put("createdAt", FieldValue.serverTimestamp());
        map.put("createdAtTimestamp", System.currentTimeMillis());
        map.put("status", "NEW");
        map.put("emailNotificationStatus", "PENDING");

        firestore.collection("appFeedback")
                .document(feedbackId)
                .set(map)
                .addOnSuccessListener(aVoid -> callback.onSuccess(feedbackId))
                .addOnFailureListener(e -> {
                    OperationErrorType errType = OperationErrorType.fromException(e);
                    callback.onError(errType, errType.getDefaultUserMessage());
                });
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
