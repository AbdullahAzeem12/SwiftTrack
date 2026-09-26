package com.swifttrack.app.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.biometric.BiometricPrompt;
import com.google.firebase.auth.FirebaseAuth;
import com.swifttrack.app.R;
import com.swifttrack.app.core.network.Resource;
import com.swifttrack.app.core.security.BiometricAuthManager;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.core.util.AlertUtil;
import com.swifttrack.app.core.util.LoadingOverlayManager;
import com.swifttrack.app.core.util.NetworkUtils;
import com.swifttrack.app.core.util.ValidationUtils;
import com.swifttrack.app.databinding.FragmentLoginBinding;
import com.swifttrack.app.presentation.dialogs.EmailVerificationDialogFragment;
import com.swifttrack.app.presentation.dialogs.ForgotPasswordBottomSheetFragment;
import com.swifttrack.app.presentation.viewmodels.AuthViewModel;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel authViewModel;
    private KeystoreManager keystoreManager;
    private LoadingOverlayManager loadingOverlayManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        keystoreManager = new KeystoreManager(requireContext());
        loadingOverlayManager = new LoadingOverlayManager(binding.loadingOverlay.getRoot());

        // Pre-fill Email if passed from logout arguments or remembered email
        String emailToFill = "";
        if (getArguments() != null && getArguments().containsKey("email")) {
            emailToFill = getArguments().getString("email", "");
        }
        if (emailToFill.isEmpty() && keystoreManager.isRememberMeEnabled()) {
            emailToFill = keystoreManager.getSavedEmail();
            binding.cbRememberMe.setChecked(true);
        }

        if (emailToFill != null && !emailToFill.isEmpty()) {
            binding.etEmail.setText(emailToFill);
            binding.etPassword.requestFocus();
        }

        // Setup Biometric Re-entry unlock button if biometric is enabled and Firebase user exists
        boolean canUseBiometric = keystoreManager.isBiometricEnabled()
                && BiometricAuthManager.isBiometricAvailable(requireContext())
                && FirebaseAuth.getInstance().getCurrentUser() != null;

        if (canUseBiometric) {
            binding.btnBiometricLogin.setVisibility(View.VISIBLE);
            binding.btnBiometricLogin.setOnClickListener(v -> performBiometricUnlock());

            // Check if auto-prompt should be triggered
            boolean autoPrompt = getArguments() != null && getArguments().getBoolean("trigger_biometric", false);
            if (autoPrompt) {
                performBiometricUnlock();
            }
        } else {
            binding.btnBiometricLogin.setVisibility(View.GONE);
        }

        binding.btnLoginSubmit.setOnClickListener(v -> performLogin(view));
        binding.tvRegisterLink.setOnClickListener(v -> {
            try {
                Navigation.findNavController(view).navigate(R.id.action_login_to_register);
            } catch (Exception ignored) {}
        });

        binding.tvForgotPassword.setOnClickListener(v -> openForgotPasswordBottomSheet());
    }

    private void performBiometricUnlock() {
        if (!isAdded() || getContext() == null) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            if (getView() != null) {
                AlertUtil.showErrorSnackbar(getView(), "Session expired. Please log in with email and password.");
            }
            binding.btnBiometricLogin.setVisibility(View.GONE);
            return;
        }

        BiometricAuthManager.showBiometricPrompt(
                LoginFragment.this,
                "Unlock SwiftTrack",
                "Use your fingerprint or face to securely re-enter SwiftTrack",
                "Use Password",
                new BiometricAuthManager.BiometricAuthCallback() {
                    @Override
                    public void onSuccess(@NonNull BiometricPrompt.AuthenticationResult result) {
                        if (getView() != null) {
                            AlertUtil.showSuccessSnackbar(getView(), "👋 Welcome back! Authenticated successfully.");
                            try {
                                Navigation.findNavController(getView()).navigate(R.id.action_login_to_home);
                            } catch (Exception e) {
                                try {
                                    Navigation.findNavController(getView()).navigate(R.id.homeBookFragment);
                                } catch (Exception ignored) {}
                            }
                        }
                    }

                    @Override
                    public void onError(int errorCode, @NonNull CharSequence errString) {
                        if (getView() != null && errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            AlertUtil.showErrorSnackbar(getView(), "Biometric authentication: " + errString);
                        }
                    }

                    @Override
                    public void onFailed() {
                        if (getView() != null) {
                            AlertUtil.showErrorSnackbar(getView(), "Biometric scan failed. Please try again or use password.");
                        }
                    }
                }
        );
    }

    private void openForgotPasswordBottomSheet() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        ForgotPasswordBottomSheetFragment bottomSheet = ForgotPasswordBottomSheetFragment.newInstance(email);
        bottomSheet.show(getChildFragmentManager(), "ForgotPasswordBottomSheet");
    }

    private void performLogin(View view) {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";
        boolean rememberMe = binding.cbRememberMe.isChecked();

        // 1. Email validation
        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilEmail.setError("Please enter a valid email address");
            return;
        }
        binding.tilEmail.setError(null);

        // 2. Password validation
        if (password.isEmpty()) {
            binding.tilPassword.setError("Password cannot be empty");
            return;
        }
        binding.tilPassword.setError(null);

        // 3. Internet connection check
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            AlertUtil.showErrorSnackbar(view, "No internet connection. Please check your network.");
            return;
        }

        loadingOverlayManager.show("Logging In...");
        binding.btnLoginSubmit.setEnabled(false);

        android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable uiTimeoutRunnable = () -> {
            if (binding != null && loadingOverlayManager != null) {
                loadingOverlayManager.hide();
                binding.btnLoginSubmit.setEnabled(true);
            }
        };
        uiHandler.postDelayed(uiTimeoutRunnable, 4500);

        authViewModel.login(email, password, rememberMe).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null || resource.status == Resource.Status.LOADING) return;

            uiHandler.removeCallbacks(uiTimeoutRunnable);
            loadingOverlayManager.hide();
            binding.btnLoginSubmit.setEnabled(true);

            if (resource.status == Resource.Status.SUCCESS) {
                String userName = resource.data != null && resource.data.getUser() != null ?
                        resource.data.getUser().getFullName() : "User";
                AlertUtil.showSuccessSnackbar(view, "👋 Welcome back, " + userName + "! Successfully logged in.");
                try {
                    Navigation.findNavController(view).navigate(R.id.action_login_to_home);
                } catch (Exception e) {
                    try {
                        Navigation.findNavController(view).navigate(R.id.homeBookFragment);
                    } catch (Exception ignored) {}
                }
            } else if (resource.status == Resource.Status.ERROR) {
                if ("EMAIL_NOT_VERIFIED".equals(resource.errorCode)) {
                    showEmailVerificationPendingDialog();
                } else {
                    AlertUtil.showErrorSnackbar(view, resource.message != null ? resource.message : "Authentication failed");
                }
            }
        });
    }

    private void showEmailVerificationPendingDialog() {
        EmailVerificationDialogFragment dialog = EmailVerificationDialogFragment.newInstance();
        dialog.setOnVerifiedListener(() -> {
            View v = getView();
            if (v != null) {
                performLogin(v);
            }
        });
        dialog.show(getChildFragmentManager(), "EmailVerificationDialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
