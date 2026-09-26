package com.swifttrack.app.presentation.dialogs;

import android.os.Build;
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
import com.swifttrack.app.R;
import com.swifttrack.app.core.util.AlertUtil;
import com.swifttrack.app.core.util.ButterClickEffect;
import com.swifttrack.app.databinding.BottomSheetReportProblemBinding;
import com.swifttrack.app.presentation.viewmodels.ProblemReportViewModel;

public class ReportProblemBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetReportProblemBinding binding;
    private ProblemReportViewModel viewModel;

    public static ReportProblemBottomSheetDialogFragment newInstance() {
        return new ReportProblemBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetReportProblemBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProblemReportViewModel.class);

        // Micro-interactions & Haptics
        ButterClickEffect.apply(binding.btnCloseReportProblem);
        ButterClickEffect.apply(binding.btnSubmitProblemReport);
        ButterClickEffect.apply(binding.btnProblemDone);

        binding.btnCloseReportProblem.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            dismiss();
        });

        setupDeviceMetadata();
        setupCategoryChipsHaptics();
        setupSubmitAction();
        observeViewModel();
    }

    private void setupDeviceMetadata() {
        String device = "📱 Device: Android " + Build.VERSION.RELEASE + " • " + Build.MANUFACTURER + " " + Build.MODEL;
        binding.tvProblemDeviceInfo.setText(device);
        binding.tvProblemAppVersion.setText("App Version: 1.0 (Build 34)");
    }

    private void setupCategoryChipsHaptics() {
        for (int i = 0; i < binding.chipGroupProblemCategories.getChildCount(); i++) {
            View child = binding.chipGroupProblemCategories.getChildAt(i);
            if (child instanceof Chip) {
                ButterClickEffect.apply(child);
                child.setOnClickListener(v -> v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY));
            }
        }
    }

    private String getSelectedCategoryString() {
        int checkedId = binding.chipGroupProblemCategories.getCheckedChipId();
        if (checkedId == R.id.chip_prob_booking) return "Booking Failed";
        if (checkedId == R.id.chip_prob_payment) return "Payment Error";
        if (checkedId == R.id.chip_prob_live) return "Live Data Incorrect";
        if (checkedId == R.id.chip_prob_qr) return "Ticket / QR Glitch";
        if (checkedId == R.id.chip_prob_biometric) return "Biometric Unlock Issue";
        if (checkedId == R.id.chip_prob_other) return "Other Issue";
        return "App Crash / Freeze";
    }

    private void setupSubmitAction() {
        binding.btnSubmitProblemReport.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            String category = getSelectedCategoryString();
            String subject = binding.etProblemSubject.getText() != null ? binding.etProblemSubject.getText().toString().trim() : "";
            String desc = binding.etProblemDesc.getText() != null ? binding.etProblemDesc.getText().toString().trim() : "";
            String bookingRef = binding.etProblemBookingRef.getText() != null ? binding.etProblemBookingRef.getText().toString().trim() : "";

            viewModel.submitProblemReport(category, subject, desc, "Account / More Hub", bookingRef);
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (binding == null || state == null) return;
            switch (state.state) {
                case IDLE:
                    binding.btnSubmitProblemReport.setEnabled(true);
                    binding.btnSubmitProblemReport.setText("Submit Problem Report");
                    break;
                case VALIDATING:
                    binding.tilProblemSubject.setError(null);
                    binding.tilProblemDesc.setError(null);
                    break;
                case SUBMITTING:
                    binding.btnSubmitProblemReport.setEnabled(false);
                    binding.btnSubmitProblemReport.setText("Submitting Report...");
                    break;
                case SUCCESS:
                    binding.containerProblemForm.setVisibility(View.GONE);
                    binding.containerProblemSuccess.setVisibility(View.VISIBLE);
                    binding.tvProblemSuccessRef.setText("Report Ref: " + state.reportId);
                    binding.btnProblemDone.setOnClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        dismiss();
                    });
                    break;
                case ERROR:
                    binding.btnSubmitProblemReport.setEnabled(true);
                    binding.btnSubmitProblemReport.setText("Submit Problem Report");
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
