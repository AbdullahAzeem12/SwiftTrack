package com.swifttrack.app.presentation.dialogs;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.swifttrack.app.R;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.core.util.AlertUtil;
import com.swifttrack.app.core.util.ButterClickEffect;
import com.swifttrack.app.databinding.BottomSheetContactSupportBinding;
import com.swifttrack.app.presentation.viewmodels.SupportViewModel;

public class ContactSupportBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetContactSupportBinding binding;
    private SupportViewModel viewModel;
    private KeystoreManager keystoreManager;

    public static ContactSupportBottomSheetDialogFragment newInstance() {
        return new ContactSupportBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetContactSupportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SupportViewModel.class);
        keystoreManager = new KeystoreManager(requireContext());

        // Micro-interactions & Haptic feedback
        ButterClickEffect.apply(binding.btnCloseContactSupport);
        ButterClickEffect.apply(binding.btnSubmitSupportTicket);
        ButterClickEffect.apply(binding.btnSupportViewRequests);
        ButterClickEffect.apply(binding.btnSupportDone);

        binding.btnCloseContactSupport.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            dismiss();
        });

        setupCategoryChipsHaptics();
        loadUserInfo();
        setupSubmitAction();
        observeViewModel();
    }

    private void setupCategoryChipsHaptics() {
        for (int i = 0; i < binding.chipGroupSupportCategories.getChildCount(); i++) {
            View child = binding.chipGroupSupportCategories.getChildAt(i);
            if (child instanceof Chip) {
                ButterClickEffect.apply(child);
                child.setOnClickListener(v -> v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY));
            }
        }
    }

    private void loadUserInfo() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = keystoreManager.getUserName();
        String email = keystoreManager.getUserEmail();
        if (user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
            email = user.getEmail();
        }
        binding.tvSupportUserName.setText("User: " + name);
        binding.tvSupportUserEmail.setText("Email: " + email);
    }

    private String getSelectedCategoryString() {
        int checkedId = binding.chipGroupSupportCategories.getCheckedChipId();
        if (checkedId == R.id.chip_cat_payment) return "Payment Issue";
        if (checkedId == R.id.chip_cat_ticket) return "Ticket Issue";
        if (checkedId == R.id.chip_cat_journey) return "Journey Issue";
        if (checkedId == R.id.chip_cat_live) return "Live Train Information";
        if (checkedId == R.id.chip_cat_account) return "Account Issue";
        if (checkedId == R.id.chip_cat_biometric) return "Biometric Issue";
        if (checkedId == R.id.chip_cat_rewards) return "Rewards Issue";
        if (checkedId == R.id.chip_cat_tech) return "Technical Problem";
        if (checkedId == R.id.chip_cat_other) return "Other";
        return "Booking Issue";
    }

    private void setupSubmitAction() {
        binding.btnSubmitSupportTicket.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            String category = getSelectedCategoryString();
            String subject = binding.etSupportSubject.getText() != null ? binding.etSupportSubject.getText().toString().trim() : "";
            String desc = binding.etSupportDesc.getText() != null ? binding.etSupportDesc.getText().toString().trim() : "";
            String bookingRef = binding.etSupportBookingRef.getText() != null ? binding.etSupportBookingRef.getText().toString().trim() : "";

            viewModel.submitSupportTicket(category, subject, desc, bookingRef);
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (binding == null || state == null) return;
            switch (state.state) {
                case IDLE:
                    binding.btnSubmitSupportTicket.setEnabled(true);
                    binding.btnSubmitSupportTicket.setText("Submit Support Request");
                    break;
                case VALIDATING:
                    binding.tilSupportSubject.setError(null);
                    binding.tilSupportDesc.setError(null);
                    break;
                case SUBMITTING:
                    binding.btnSubmitSupportTicket.setEnabled(false);
                    binding.btnSubmitSupportTicket.setText("Submitting Request...");
                    break;
                case SUCCESS:
                    binding.containerSupportForm.setVisibility(View.GONE);
                    binding.containerSupportSuccess.setVisibility(View.VISIBLE);
                    binding.tvSupportSuccessRef.setText("Reference: " + state.ticketReference);

                    binding.btnSupportViewRequests.setOnClickListener(v1 -> {
                        v1.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        dismiss();
                        MySupportRequestsBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "my_support_requests");
                    });
                    binding.btnSupportDone.setOnClickListener(v1 -> {
                        v1.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        dismiss();
                    });
                    break;
                case ERROR:
                    binding.btnSubmitSupportTicket.setEnabled(true);
                    binding.btnSubmitSupportTicket.setText("Submit Support Request");
                    AlertUtil.showErrorSnackbar(requireView(), state.errorMessage);
                    break;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
