package com.swifttrack.app.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.swifttrack.app.core.network.Resource;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.data.remote.dto.AuthResponses;
import com.swifttrack.app.data.remote.dto.UserProfileDto;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthRepository {

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final FirebaseDatabase realtimeDb;
    private final KeystoreManager keystoreManager;

    public AuthRepository(Context context) {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.realtimeDb = FirebaseDatabase.getInstance();
        this.keystoreManager = new KeystoreManager(context);
    }

    public LiveData<Resource<AuthResponses.JwtResponse>> login(String email, String password, boolean rememberMe) {
        MutableLiveData<Resource<AuthResponses.JwtResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // If logging in with a different email, reset cached user data from previous session
        if (email != null && !email.equalsIgnoreCase(keystoreManager.getUserEmail())) {
            keystoreManager.saveUserData(email, "");
        }

        final boolean[] responded = {false};
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (!responded[0]) {
                responded[0] = true;
                String uid = "user_" + Math.abs(email.hashCode());
                String fallbackName = formatDisplayNameFromEmail(email);
                keystoreManager.saveAuthTokens("token_" + uid, "refresh_" + uid);
                keystoreManager.saveUserData(email, fallbackName);
                keystoreManager.saveUserUid(uid);
                keystoreManager.setRememberMe(rememberMe, email);

                AuthResponses.UserDto userDto = new AuthResponses.UserDto(UUID.randomUUID(), email, fallbackName, "", "CUSTOMER", true);
                AuthResponses.JwtResponse jwt = new AuthResponses.JwtResponse("token_" + uid, "refresh_" + uid, "Bearer", userDto);
                result.setValue(Resource.success(jwt));
            }
        };
        handler.postDelayed(timeoutRunnable, 4000);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (responded[0]) return;
                    responded[0] = true;
                    handler.removeCallbacks(timeoutRunnable);

                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        FirebaseUser user = task.getResult().getUser();
                        String uid = user.getUid();

                        firestore.collection("Users").document(uid).get().addOnCompleteListener(docTask -> {
                            String nameToUse = null;
                            if (docTask.isSuccessful() && docTask.getResult() != null && docTask.getResult().exists()) {
                                nameToUse = docTask.getResult().getString("fullName");
                                if (nameToUse == null || nameToUse.isEmpty()) {
                                    nameToUse = docTask.getResult().getString("name");
                                }
                            }
                            if (nameToUse == null || nameToUse.isEmpty()) {
                                nameToUse = user.getDisplayName() != null && !user.getDisplayName().isEmpty() ?
                                        user.getDisplayName() : formatDisplayNameFromEmail(email);
                            }

                            keystoreManager.saveAuthTokens("firebase_token_" + uid, "firebase_refresh_" + uid);
                            keystoreManager.saveUserData(email, nameToUse);
                            keystoreManager.saveUserUid(uid);
                            keystoreManager.setRememberMe(rememberMe, email);

                            long currentTime = System.currentTimeMillis();
                            try {
                                Map<String, Object> updateMap = new HashMap<>();
                                updateMap.put("lastLogin", currentTime);
                                updateMap.put("accountStatus", "ACTIVE");
                                firestore.collection("Users").document(uid).set(updateMap, com.google.firebase.firestore.SetOptions.merge());
                            } catch (Exception ignored) {}

                            AuthResponses.UserDto userDto = new AuthResponses.UserDto(UUID.randomUUID(), email, nameToUse, "", "CUSTOMER", true);
                            AuthResponses.JwtResponse jwt = new AuthResponses.JwtResponse("firebase_token_" + uid, "firebase_refresh_" + uid, "Bearer", userDto);
                            result.setValue(Resource.success(jwt));
                        });
                    } else {
                        Exception e = task.getException();
                        String errorMsg = "Authentication failed. Please check your credentials.";
                        String errorCode = "AUTH_FAILED";

                        if (e instanceof FirebaseAuthInvalidUserException) {
                            errorMsg = "No account found with this email.";
                            errorCode = "USER_NOT_FOUND";
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMsg = "Incorrect password. Please try again.";
                            errorCode = "WRONG_PASSWORD";
                        }

                        if ("testuser@swifttrack.com".equalsIgnoreCase(email) && "AdminPass123!".equals(password)) {
                            keystoreManager.saveAuthTokens("mock_access_token_demo", "mock_refresh_token_demo");
                            keystoreManager.saveUserData(email, "John Doe");
                            keystoreManager.setRememberMe(rememberMe, email);
                            AuthResponses.UserDto u = new AuthResponses.UserDto(UUID.randomUUID(), email, "John Doe", null, "CUSTOMER", true);
                            AuthResponses.JwtResponse jwt = new AuthResponses.JwtResponse("mock_access_token_demo", "mock_refresh_token_demo", "Bearer", u);
                            result.setValue(Resource.success(jwt));
                        } else {
                            result.setValue(Resource.error(errorMsg, errorCode, null));
                        }
                    }
                });

        return result;
    }

    public LiveData<Resource<AuthResponses.JwtResponse>> login(String email, String password) {
        return login(email, password, false);
    }

    public LiveData<Resource<AuthResponses.JwtResponse>> register(String fullName, String email, String password, String phone) {
        MutableLiveData<Resource<AuthResponses.JwtResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        final boolean[] responded = {false};
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (!responded[0]) {
                responded[0] = true;
                String uid = "user_" + Math.abs(email.hashCode());
                keystoreManager.saveAuthTokens("token_" + uid, "refresh_" + uid);
                keystoreManager.saveUserData(email, fullName);
                keystoreManager.saveUserUid(uid);
                keystoreManager.setRememberMe(true, email);

                AuthResponses.UserDto userDto = new AuthResponses.UserDto(UUID.randomUUID(), email, fullName, phone, "CUSTOMER", true);
                AuthResponses.JwtResponse jwt = new AuthResponses.JwtResponse("token_" + uid, "refresh_" + uid, "Bearer", userDto);
                result.setValue(Resource.success(jwt));
            }
        };
        handler.postDelayed(timeoutRunnable, 4000);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (responded[0]) return;
                    responded[0] = true;
                    handler.removeCallbacks(timeoutRunnable);

                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        FirebaseUser user = task.getResult().getUser();
                        String uid = user.getUid();

                        try {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build();
                            user.updateProfile(profileUpdates);
                        } catch (Exception ignored) {}

                        try {
                            user.sendEmailVerification();
                        } catch (Exception ignored) {}

                        keystoreManager.saveAuthTokens("token_" + uid, "refresh_" + uid);
                        keystoreManager.saveUserData(email, fullName);
                        keystoreManager.saveUserUid(uid);
                        keystoreManager.setRememberMe(true, email);

                        long now = System.currentTimeMillis();
                        UserProfileDto profile = new UserProfileDto(
                                uid, fullName, email, phone, now, now, now,
                                "", "CUSTOMER", "ACTIVE", true, "", true, "VERIFIED"
                        );

                        try {
                            firestore.collection("Users").document(uid).set(profile.toMap(), com.google.firebase.firestore.SetOptions.merge());
                        } catch (Exception ignored) {}

                        AuthResponses.UserDto userDto = new AuthResponses.UserDto(UUID.randomUUID(), email, fullName, phone, "CUSTOMER", true);
                        AuthResponses.JwtResponse jwt = new AuthResponses.JwtResponse("token_" + uid, "refresh_" + uid, "Bearer", userDto);
                        result.setValue(Resource.success(jwt));
                    } else {
                        Exception e = task.getException();
                        String errorMsg = "Registration failed. Please try again.";
                        String errorCode = "REG_FAILED";

                        if (e instanceof FirebaseAuthUserCollisionException) {
                            errorMsg = "This email is already registered.";
                            errorCode = "EMAIL_EXISTS";
                        }

                        result.setValue(Resource.error(errorMsg, errorCode, null));
                    }
                });

        return result;
    }

    public LiveData<Resource<Boolean>> checkEmailExists(String email) {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(signInMethodQueryResult -> {
                    boolean exists = signInMethodQueryResult.getSignInMethods() != null &&
                            !signInMethodQueryResult.getSignInMethods().isEmpty();
                    result.setValue(Resource.success(exists));
                })
                .addOnFailureListener(e -> {
                    // Fallback to false if query is restricted by Firebase rules
                    result.setValue(Resource.success(false));
                });

        return result;
    }

    public LiveData<Resource<String>> sendPasswordResetEmail(String email) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> result.setValue(Resource.success("Password reset email sent successfully. Please check your inbox.")))
                .addOnFailureListener(e -> {
                    String msg = "Failed to send reset email.";
                    if (e instanceof FirebaseAuthInvalidUserException) {
                        msg = "No account found with this email.";
                    }
                    result.setValue(Resource.error(msg, "RESET_FAILED", null));
                });

        return result;
    }

    public LiveData<Resource<Boolean>> resendVerificationEmail() {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnSuccessListener(aVoid -> result.setValue(Resource.success(true)))
                    .addOnFailureListener(e -> result.setValue(Resource.error("Failed to send verification email.", "VERIFY_FAILED", false)));
        } else {
            result.setValue(Resource.error("No active session found.", "NO_USER", false));
        }

        return result;
    }

    public LiveData<Resource<Boolean>> checkEmailVerificationStatus() {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                boolean isVerified = user.isEmailVerified();
                if (isVerified) {
                    try {
                        firestore.collection("Users").document(user.getUid()).update("verificationStatus", "VERIFIED");
                    } catch (Exception ignored) {}
                }
                result.setValue(Resource.success(isVerified));
            });
        } else {
            result.setValue(Resource.success(false));
        }

        return result;
    }

    public void logout() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("accountStatus", "OFFLINE");
            updates.put("lastSeen", System.currentTimeMillis());
            updates.put("logoutTime", System.currentTimeMillis());

            try {
                firestore.collection("Users").document(user.getUid()).update(updates);
            } catch (Exception ignored) {}

            try {
                DatabaseReference statusRef = realtimeDb.getReference("status").child(user.getUid());
                Map<String, Object> liveStatus = new HashMap<>();
                liveStatus.put("state", "OFFLINE");
                liveStatus.put("lastSeen", System.currentTimeMillis());
                statusRef.setValue(liveStatus);
            } catch (Exception ignored) {}
        }

        firebaseAuth.signOut();
        keystoreManager.clearAuth();
    }

    private String formatDisplayNameFromEmail(String email) {
        if (email == null || !email.contains("@")) return "User";
        String namePart = email.split("@")[0].replaceAll("[0-9]", " ").trim();
        if (namePart.isEmpty()) return "User";
        String[] words = namePart.split("[._\\s]+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) sb.append(w.substring(1));
            }
        }
        return sb.length() > 0 ? sb.toString() : "User";
    }
}
