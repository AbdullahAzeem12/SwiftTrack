package com.swifttrack.app.presentation.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.swifttrack.app.R;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.core.util.AlertUtil;
import com.swifttrack.app.databinding.BottomSheetAccountSecurityBinding;
import com.swifttrack.app.presentation.viewmodels.AuthViewModel;

public class AccountSecurityBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetAccountSecurityBinding binding;
    private KeystoreManager keystoreManager;

    public static AccountSecurityBottomSheetDialogFragment newInstance() {
        return new AccountSecurityBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAccountSecurityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        keystoreManager = new KeystoreManager(requireContext());

        binding.btnCloseAccountSecurity.setOnClickListener(v -> dismiss());

        loadUserSecurityState();
        setupLogoutAction();
    }

    private void loadUserSecurityState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = keystoreManager.getUserName();
        String email = keystoreManager.getUserEmail();
        String uid = user != null ? user.getUid() : keystoreManager.getUserUid();

        if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
            email = user.getEmail();
        }

        binding.tvSheetProfileName.setText("Name: " + name);
        binding.tvSheetProfileEmail.setText("Email: " + email);
        binding.tvSheetProfileUid.setText("Firebase UID: " + (uid != null && uid.length() > 16 ? uid.substring(0, 16) + "..." : uid));

        boolean bioEnabled = keystoreManager.isBiometricEnabled();
        if (bioEnabled) {
            binding.tvSheetBiometricStatus.setText("Biometric Unlock: ENABLED 🔒");
            binding.tvSheetBiometricStatus.setTextColor(getResources().getColor(R.color.status_green, null));
        } else {
            binding.tvSheetBiometricStatus.setText("Biometric Unlock: DISABLED 🔓");
            binding.tvSheetBiometricStatus.setTextColor(getResources().getColor(R.color.text_secondary, null));
        }
    }

    private void setupLogoutAction() {
        AuthViewModel authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        binding.btnSheetLogout.setOnClickListener(v -> {
            dismiss();
            authViewModel.logout();
            keystoreManager.clearAuth();
            if (getActivity() != null && getActivity().findViewById(android.R.id.content) != null) {
                AlertUtil.showSuccessSnackbar(getActivity().findViewById(android.R.id.content), "🚪 Logged out successfully");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
