package com.swifttrack.app.core.util;

import com.google.firebase.firestore.FirebaseFirestoreException;

public enum OperationErrorType {
    AUTH_EXPIRED("Your session has expired. Please sign in again."),
    NETWORK_UNAVAILABLE("No internet connection. Please check your connection and try again."),
    PERMISSION_DENIED("You don't have permission to perform this request."),
    SERVICE_UNAVAILABLE("SwiftTrack service is currently busy. Please try again shortly."),
    TIMEOUT("The request took too long. Please check your connection."),
    VALIDATION_ERROR("Please complete all required fields correctly."),
    UNKNOWN("We couldn't process your request right now. Please try again.");

    private final String defaultUserMessage;

    OperationErrorType(String defaultUserMessage) {
        this.defaultUserMessage = defaultUserMessage;
    }

    public String getDefaultUserMessage() {
        return defaultUserMessage;
    }

    public static OperationErrorType fromException(Exception e) {
        if (e == null) return UNKNOWN;
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
            if (fe.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return NETWORK_UNAVAILABLE;
            } else if (fe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return PERMISSION_DENIED;
            } else if (fe.getCode() == FirebaseFirestoreException.Code.UNAUTHENTICATED) {
                return AUTH_EXPIRED;
            } else if (fe.getCode() == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                return TIMEOUT;
            } else if (fe.getCode() == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                return SERVICE_UNAVAILABLE;
            }
        }
        String msg = e.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("network") || lower.contains("connection") || lower.contains("offline") || lower.contains("unable to resolve host")) {
                return NETWORK_UNAVAILABLE;
            }
            if (lower.contains("permission") || lower.contains("denied")) {
                return PERMISSION_DENIED;
            }
            if (lower.contains("unauthenticated") || lower.contains("auth")) {
                return AUTH_EXPIRED;
            }
        }
        return UNKNOWN;
    }
}
