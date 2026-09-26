package com.swifttrack.app.data.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.core.util.OperationErrorType;
import com.swifttrack.app.data.model.ProblemReport;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ProblemReportRepository {

    private static ProblemReportRepository instance;
    private final FirebaseFirestore firestore;

    private ProblemReportRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized ProblemReportRepository getInstance() {
        if (instance == null) {
            instance = new ProblemReportRepository();
        }
        return instance;
    }

    public interface ProblemReportCallback {
        void onSuccess(String reportId);
        void onError(OperationErrorType errorType, String userFriendlyMessage);
    }

    public void submitProblemReport(Context context, ProblemReport report, ProblemReportCallback callback) {
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

        String reportId = generateReportReference();
        report.setReportId(reportId);
        report.setUserId(uid);
        report.setUserEmail(email);
        report.setUserName(name);

        String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
        String androidVer = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";

        Map<String, Object> map = new HashMap<>();
        map.put("reportId", reportId);
        map.put("userId", uid);
        map.put("userName", name != null ? name : "");
        map.put("userEmail", email != null ? email : "");
        map.put("category", report.getCategory() != null ? report.getCategory() : "App Crash / Freeze");
        map.put("subject", report.getSubject() != null ? report.getSubject() : "");
        map.put("description", report.getDescription() != null ? report.getDescription() : "");
        map.put("screenName", report.getScreenName() != null ? report.getScreenName() : "Account / More Hub");
        map.put("bookingId", report.getBookingId() != null ? report.getBookingId() : "");
        map.put("appVersion", report.getAppVersion() != null ? report.getAppVersion() : "1.0");
        map.put("androidVersion", androidVer);
        map.put("deviceModel", deviceModel);
        map.put("createdAt", FieldValue.serverTimestamp());
        map.put("createdAtTimestamp", System.currentTimeMillis());
        map.put("status", "NEW");
        map.put("emailNotificationStatus", "PENDING");

        firestore.collection("problemReports")
                .document(reportId)
                .set(map)
                .addOnSuccessListener(aVoid -> callback.onSuccess(reportId))
                .addOnFailureListener(e -> {
                    OperationErrorType errType = OperationErrorType.fromException(e);
                    callback.onError(errType, errType.getDefaultUserMessage());
                });
    }

    private String generateReportReference() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("PR-");
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
