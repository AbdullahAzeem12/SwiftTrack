package com.swifttrack.app.core.security;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.security.KeyStore;
import java.util.concurrent.Executor;
import javax.crypto.KeyGenerator;

public class BiometricAuthManager {

    private static final String KEY_ALIAS = "swifttrack_biometric_key";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    public interface BiometricAuthCallback {
        void onSuccess(@NonNull BiometricPrompt.AuthenticationResult result);
        void onError(int errorCode, @NonNull CharSequence errString);
        void onFailed();
    }

    public enum BiometricStatus {
        AVAILABLE,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        NONE_ENROLLED,
        UNSUPPORTED
    }

    public static BiometricStatus getBiometricStatus(Context context) {
        if (context == null) return BiometricStatus.UNSUPPORTED;
        BiometricManager biometricManager = BiometricManager.from(context);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK;
        int status = biometricManager.canAuthenticate(authenticators);

        switch (status) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return BiometricStatus.AVAILABLE;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return BiometricStatus.NO_HARDWARE;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                return BiometricStatus.HARDWARE_UNAVAILABLE;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return BiometricStatus.NONE_ENROLLED;
            default:
                return BiometricStatus.UNSUPPORTED;
        }
    }

    public static boolean isBiometricAvailable(Context context) {
        return getBiometricStatus(context) == BiometricStatus.AVAILABLE;
    }

    public static boolean generateBiometricKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER
            );

            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(true);
            }

            keyGenerator.init(builder.build());
            keyGenerator.generateKey();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean removeBiometricKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean hasBiometricKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            return keyStore.containsAlias(KEY_ALIAS);
        } catch (Exception e) {
            return false;
        }
    }

    public static void showBiometricPrompt(
            @NonNull Fragment fragment,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull String negativeButtonText,
            @NonNull BiometricAuthCallback callback
    ) {
        showBiometricPrompt(
                fragment.requireActivity(),
                fragment,
                title,
                subtitle,
                negativeButtonText,
                callback
        );
    }

    public static void showBiometricPrompt(
            @NonNull FragmentActivity activity,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull String negativeButtonText,
            @NonNull BiometricAuthCallback callback
    ) {
        showBiometricPrompt(
                activity,
                null,
                title,
                subtitle,
                negativeButtonText,
                callback
        );
    }

    private static void showBiometricPrompt(
            @NonNull FragmentActivity activity,
            Fragment fragment,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull String negativeButtonText,
            @NonNull BiometricAuthCallback callback
    ) {
        Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt.AuthenticationCallback promptCallback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                callback.onError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                callback.onSuccess(result);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                callback.onFailed();
            }
        };

        BiometricPrompt biometricPrompt;
        if (fragment != null) {
            biometricPrompt = new BiometricPrompt(fragment, executor, promptCallback);
        } else {
            biometricPrompt = new BiometricPrompt(activity, executor, promptCallback);
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
