package com.swifttrack.app.presentation.dialogs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.swifttrack.app.core.network.Resource;
import com.swifttrack.app.core.util.AlertUtil;
import com.swifttrack.app.databinding.DialogEmailVerificationBinding;
import com.swifttrack.app.presentation.viewmodels.AuthViewModel;

public class EmailVerificationDialogFragment extends DialogFragment {

    private DialogEmailVerificationBinding binding;
    private AuthViewModel authViewModel;
    private OnVerifiedListener verifiedListener;

    public interface OnVerifiedListener {
        void onVerified();
    }

    public static EmailVerificationDialogFragment newInstance() {
        return new EmailVerificationDialogFragment();
    }

    public void setOnVerifiedListener(OnVerifiedListener listener) {
        this.verifiedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogEmailVerificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        binding.btnOpenGmail.setOnClickListener(v -> openEmailClient());
        binding.btnRefreshVerification.setOnClickListener(v -> checkVerificationStatus());
        binding.tvResendEmail.setOnClickListener(v -> resendEmail());
    }

    private void openEmailClient() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Intent launchIntent = requireContext().getPackageManager().getLaunchIntentForPackage("com.google.android.gm");
            if (launchIntent != null) {
                startActivity(launchIntent);
            } else {
                AlertUtil.showWarningSnackbar(requireView(), "No email client application found.");
            }
        }
    }

    private void checkVerificationStatus() {
        binding.btnRefreshVerification.setEnabled(false);
        binding.btnRefreshVerification.setText("Checking Status...");

        authViewModel.checkEmailVerificationStatus().observe(getViewLifecycleOwner(), resource -> {
            binding.btnRefreshVerification.setEnabled(true);
            binding.btnRefreshVerification.setText("Refresh Verification Status");

            if (resource == null) return;

            if (resource.status == Resource.Status.SUCCESS && Boolean.TRUE.equals(resource.data)) {
                AlertUtil.showSuccessSnackbar(requireView(), "✓ Email Verified Successfully!");
                if (verifiedListener != null) {
                    verifiedListener.onVerified();
                }
                dismissAllowingStateLoss();
            } else {
                AlertUtil.showErrorSnackbar(requireView(), "Email is not verified yet. Please check your inbox and click the verification link.");
            }
        });
    }

    private void resendEmail() {
        binding.tvResendEmail.setEnabled(false);
        authViewModel.resendVerificationEmail().observe(getViewLifecycleOwner(), resource -> {
            binding.tvResendEmail.setEnabled(true);
            if (resource == null) return;
            if (resource.status == Resource.Status.SUCCESS) {
                AlertUtil.showSuccessSnackbar(requireView(), "Verification email sent successfully.");
            } else {
                AlertUtil.showErrorSnackbar(requireView(), resource.message != null ? resource.message : "Failed to resend verification email.");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
