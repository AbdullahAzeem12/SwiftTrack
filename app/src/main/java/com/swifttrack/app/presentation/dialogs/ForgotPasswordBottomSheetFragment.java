package com.swifttrack.app.presentation.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.swifttrack.app.core.network.Resource;
import com.swifttrack.app.core.util.AlertUtil;
import com.swifttrack.app.core.util.NetworkUtils;
import com.swifttrack.app.core.util.ValidationUtils;
import com.swifttrack.app.databinding.BottomSheetForgotPasswordBinding;
import com.swifttrack.app.presentation.viewmodels.AuthViewModel;

public class ForgotPasswordBottomSheetFragment extends BottomSheetDialogFragment {

    private BottomSheetForgotPasswordBinding binding;
    private AuthViewModel authViewModel;

    public static ForgotPasswordBottomSheetFragment newInstance(String defaultEmail) {
        ForgotPasswordBottomSheetFragment fragment = new ForgotPasswordBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("email", defaultEmail);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        if (getArguments() != null) {
            String initialEmail = getArguments().getString("email", "");
            if (binding.etSheetEmail != null && !initialEmail.isEmpty()) {
                binding.etSheetEmail.setText(initialEmail);
            }
        }

        binding.btnSheetSubmit.setOnClickListener(v -> performPasswordReset());
    }

    private void performPasswordReset() {
        String email = binding.etSheetEmail.getText() != null ? binding.etSheetEmail.getText().toString().trim() : "";

        if (!ValidationUtils.isValidEmail(email)) {
            binding.tilSheetEmail.setError("Please enter a valid email address");
            return;
        }
        binding.tilSheetEmail.setError(null);

        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            AlertUtil.showErrorSnackbar(requireView(), "No internet connection. Please check your network.");
            return;
        }

        binding.btnSheetSubmit.setEnabled(false);
        binding.btnSheetSubmit.setText("Verifying...");

        authViewModel.checkEmailExists(email).observe(getViewLifecycleOwner(), resource -> {
            if (resource == null || resource.status == Resource.Status.LOADING) return;

            if (resource.status == Resource.Status.SUCCESS && Boolean.TRUE.equals(resource.data)) {
                binding.btnSheetSubmit.setText("Sending Email...");
                authViewModel.sendPasswordResetEmail(email).observe(getViewLifecycleOwner(), resetRes -> {
                    if (resetRes == null || resetRes.status == Resource.Status.LOADING) return;
                    if (resetRes.status == Resource.Status.SUCCESS) {
                        if (getActivity() != null && getActivity().findViewById(android.R.id.content) != null) {
                            AlertUtil.showSuccessSnackbar(getActivity().findViewById(android.R.id.content), "Password reset email sent successfully. Please check your inbox.");
                        }
                        dismissAllowingStateLoss();
                    } else {
                        binding.btnSheetSubmit.setEnabled(true);
                        binding.btnSheetSubmit.setText("Send Reset Link");
                        AlertUtil.showErrorSnackbar(requireView(), resetRes.message != null ? resetRes.message : "Failed to send reset link.");
                    }
                });
            } else {
                binding.btnSheetSubmit.setEnabled(true);
                binding.btnSheetSubmit.setText("Send Reset Link");
                AlertUtil.showErrorSnackbar(requireView(), "No account found with this email.");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
