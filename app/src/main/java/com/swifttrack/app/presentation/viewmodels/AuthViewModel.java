package com.swifttrack.app.presentation.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.swifttrack.app.core.network.Resource;
import com.swifttrack.app.data.remote.dto.AuthResponses;
import com.swifttrack.app.data.repository.AuthRepository;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
    }

    public LiveData<Resource<AuthResponses.JwtResponse>> login(String email, String password, boolean rememberMe) {
        return authRepository.login(email, password, rememberMe);
    }

    public LiveData<Resource<AuthResponses.JwtResponse>> login(String email, String password) {
        return authRepository.login(email, password, false);
    }

    public LiveData<Resource<AuthResponses.JwtResponse>> register(String fullName, String email, String password, String phone) {
        return authRepository.register(fullName, email, password, phone);
    }

    public LiveData<Resource<Boolean>> checkEmailExists(String email) {
        return authRepository.checkEmailExists(email);
    }

    public LiveData<Resource<String>> sendPasswordResetEmail(String email) {
        return authRepository.sendPasswordResetEmail(email);
    }

    public LiveData<Resource<Boolean>> resendVerificationEmail() {
        return authRepository.resendVerificationEmail();
    }

    public LiveData<Resource<Boolean>> checkEmailVerificationStatus() {
        return authRepository.checkEmailVerificationStatus();
    }

    public void logout() {
        authRepository.logout();
    }
}
