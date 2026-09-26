package com.swifttrack.app.core.network;

import com.swifttrack.app.core.security.KeystoreManager;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class AuthInterceptor implements Interceptor {

    private final KeystoreManager keystoreManager;

    public AuthInterceptor(KeystoreManager keystoreManager) {
        this.keystoreManager = keystoreManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String token = keystoreManager.getAccessToken();

        if (token != null && !token.isEmpty()) {
            Request authenticatedRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .build();
            return chain.proceed(authenticatedRequest);
        }

        return chain.proceed(originalRequest);
    }
}
