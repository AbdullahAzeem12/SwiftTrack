package com.swifttrack.app.core.security;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

public class KeystoreManager {

    private static final String PREF_NAME = "swifttrack_secure_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_UID = "user_uid";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_SAVED_EMAIL = "saved_email";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_NIGHT_MODE = "night_mode";

    private final SharedPreferences prefs;

    public KeystoreManager(Context context) {
        SharedPreferences sharedPrefs;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            sharedPrefs = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    masterKeyAlias,
                    context.getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.values()[0],
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.values()[0]
            );
        } catch (Exception e) {
            sharedPrefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        this.prefs = sharedPrefs;
    }

    public void saveAuthTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public void saveUserData(String email, String name) {
        prefs.edit()
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_NAME, name)
                .apply();
    }

    public void saveUserUid(String uid) {
        prefs.edit().putString(KEY_USER_UID, uid).apply();
    }

    public String getUserUid() {
        return prefs.getString(KEY_USER_UID, "");
    }

    public void setRememberMe(boolean remember, String email) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_REMEMBER_ME, remember);
        if (remember && email != null) {
            editor.putString(KEY_SAVED_EMAIL, email);
        } else if (!remember) {
            editor.remove(KEY_SAVED_EMAIL);
        }
        editor.apply();
    }

    public boolean isRememberMeEnabled() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }

    public String getSavedEmail() {
        return prefs.getString(KEY_SAVED_EMAIL, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Guest User");
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public int getNightMode() {
        return prefs.getInt(KEY_NIGHT_MODE, androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setNightMode(int mode) {
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply();
    }

    public boolean isDarkMode(Context context) {
        int mode = getNightMode();
        if (mode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) return true;
        if (mode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) return false;
        int currentNightMode = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    public void clearAuth() {
        BiometricAuthManager.removeBiometricKey();
        SharedPreferences.Editor editor = prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_UID)
                .remove(KEY_BIOMETRIC_ENABLED);
        
        if (!isRememberMeEnabled()) {
            editor.remove(KEY_SAVED_EMAIL);
            editor.remove(KEY_REMEMBER_ME);
        }
        editor.apply();
    }
}
