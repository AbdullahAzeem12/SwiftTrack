package com.swifttrack.app;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.swifttrack.app.core.security.KeystoreManager;

public class SwiftTrackApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        KeystoreManager keystoreManager = new KeystoreManager(this);
        int nightMode = keystoreManager.getNightMode();
        AppCompatDelegate.setDefaultNightMode(nightMode);

        try {
            FirebaseApp.initializeApp(this);
            FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
            firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance());
        } catch (Exception ignored) {}
    }
}

