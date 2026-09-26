package com.swifttrack.app.presentation.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.swifttrack.app.R;
import com.swifttrack.app.core.network.Resource;
import com.swifttrack.app.core.util.AlertUtil;
import com.swifttrack.app.core.util.LoadingOverlayManager;
import com.swifttrack.app.core.util.NetworkUtils;
import com.swifttrack.app.core.util.PasswordStrengthUtil;
import com.swifttrack.app.core.util.ValidationUtils;
import com.swifttrack.app.databinding.FragmentRegisterBinding;
import com.swifttrack.app.presentation.dialogs.EmailVerificationDialogFragment;
import com.swifttrack.app.presentation.viewmodels.AuthViewModel;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel authViewModel;
    private LoadingOverlayManager loadingOverlayManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        loadingOverlayManager = new LoadingOverlayManager(binding.loadingOverlay.getRoot());

        setupPasswordWatcher();

        binding.btnRegisterSubmit.setOnClickListener(v -> performRegistration(view));
        binding.tvLoginLink.setOnClickListener(v -> {
            try {
                Navigation.findNavController(view).navigateUp();
            } catch (Exception ignored) {}
        });
    }

    private void setupPasswordWatcher() {
        binding.etRegisterPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordStrength(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updatePasswordStrength(String password) {
        PasswordStrengthUtil.StrengthLevel level = PasswordStrengthUtil.calculateStrength(password);

        binding.pbPasswordStrength.setProgress(level.getProgressPercentage());

        if (level == PasswordStrengthUtil.StrengthLevel.EMPTY) {
            binding.tvPasswordStrength.setText("None");
            binding.tvPasswordStrength.setTextColor(requireContext().getColor(R.color.text_muted));
            return;
        }

        binding.tvPasswordStrength.setText(level.getLabel());
        binding.tvPasswordStrength.setTextColor(level.getColor());
    }

    private void performRegistration(View view) {
        String fullName = binding.etRegisterName.getText() != null ? binding.etRegisterName.getText().toString().trim() : "";
        String email = binding.etRegisterEmail.getText() != null ? binding.etRegisterEmail.getText().toString().trim() : "";
        String password = binding.etRegisterPassword.getText() != null ? binding.etRegisterPassword.getText().toString() : "";
        String confirmPassword = binding.etRegisterConfirmPassword.getText() != null ? binding.etRegisterConfirmPassword.getText().toString() : "";

        // 1. Full Name validation
        if (fullName.isEmpty()) {
            binding.tilRegisterName.setError("Full Name cannot be empty");
            return;
        }
        if (!ValidationUtils.isValidFullName(fullName)) {
            binding.tilRegisterName.setError("Full Name must be at least 3 characters without invalid symbols");
            return;
        }
        binding.tilRegisterName.setError(null);

        // 2. Email validation
        if (email.isEmpty()) {
            binding.tilRegisterEmail.setError("Email Address cannot be empty");
            return;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilRegisterEmail.setError("Please enter a valid email format");
            return;
        }
        binding.tilRegisterEmail.setError(null);

        // 3. Password validation
        if (password.isEmpty()) {
            binding.tilRegisterPassword.setError("Password cannot be empty");
            return;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            binding.tilRegisterPassword.setError("Password must contain min 8 chars, uppercase, lowercase, number, & special character (e.g. Abdullah@2026)");
            return;
        }
        binding.tilRegisterPassword.setError(null);

        // 4. Confirm Password validation
        if (!ValidationUtils.doPasswordsMatch(password, confirmPassword)) {
            binding.tilRegisterConfirmPassword.setError("Passwords do not match.");
            return;
        }
        binding.tilRegisterConfirmPassword.setError(null);

        // 5. Terms & Conditions check
        if (!binding.cbTerms.isChecked()) {
            AlertUtil.showWarningSnackbar(view, "Please agree to the Terms & Privacy Policy to proceed.");
            return;
        }

        // 6. Network check
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            AlertUtil.showErrorSnackbar(view, "No internet connection. Please check your network.");
            return;
        }

        new com.swifttrack.app.core.security.KeystoreManager(requireContext()).saveUserData(email, fullName);

        loadingOverlayManager.show("Creating Account...");
        binding.btnRegisterSubmit.setEnabled(false);

        android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable uiTimeoutRunnable = () -> {
            if (binding != null && loadingOverlayManager != null) {
                loadingOverlayManager.hide();
                binding.btnRegisterSubmit.setEnabled(true);
            }
        };
        uiHandler.postDelayed(uiTimeoutRunnable, 4500);

        executeRegistration(view, fullName, email, password, uiHandler, uiTimeoutRunnable);
    }

    private void executeRegistration(View view, String fullName, String email, String password, android.os.Handler uiHandler, Runnable uiTimeoutRunnable) {
        authViewModel.register(fullName, email, password, "").observe(getViewLifecycleOwner(), resource -> {
            if (resource == null || resource.status == Resource.Status.LOADING) return;

            if (uiHandler != null && uiTimeoutRunnable != null) {
                uiHandler.removeCallbacks(uiTimeoutRunnable);
            }
            loadingOverlayManager.hide();
            binding.btnRegisterSubmit.setEnabled(true);

            if (resource.status == Resource.Status.SUCCESS) {
                if (getActivity() != null && getActivity().findViewById(android.R.id.content) != null) {
                    AlertUtil.showSuccessSnackbar(getActivity().findViewById(android.R.id.content), "🎉 Welcome, " + fullName + "! Account Created Successfully");
                }

                try {
                    Navigation.findNavController(view).navigate(R.id.action_register_to_home);
                } catch (Exception e) {
                    try {
                        Navigation.findNavController(view).navigate(R.id.homeBookFragment);
                    } catch (Exception ignored) {}
                }
            } else if (resource.status == Resource.Status.ERROR) {
                if ("EMAIL_EXISTS".equals(resource.errorCode)) {
                    binding.tilRegisterEmail.setError("This email is already registered.");
                }
                AlertUtil.showErrorSnackbar(view, resource.message != null ? resource.message : "Registration failed.");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
