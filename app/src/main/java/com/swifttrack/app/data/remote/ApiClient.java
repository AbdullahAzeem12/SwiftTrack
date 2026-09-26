package com.swifttrack.app.data.remote;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.swifttrack.app.core.network.AuthInterceptor;
import com.swifttrack.app.core.security.KeystoreManager;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:8080/"; // Standard Android Emulator localhost endpoint
    private static SwiftTrackApi apiInstance;

    public static synchronized SwiftTrackApi getInstance(Context context) {
        if (apiInstance == null) {
            KeystoreManager keystoreManager = new KeystoreManager(context);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(keystoreManager))
                    .addInterceptor(logging)
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .readTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(3, TimeUnit.SECONDS)
                    .build();

            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();

            apiInstance = retrofit.create(SwiftTrackApi.class);
        }
        return apiInstance;
    }
}
