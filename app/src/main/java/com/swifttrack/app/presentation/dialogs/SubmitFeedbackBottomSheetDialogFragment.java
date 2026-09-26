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
import com.swifttrack.app.R;
import com.swifttrack.app.core.util.AlertUtil;
import com.swifttrack.app.core.util.ButterClickEffect;
import com.swifttrack.app.databinding.BottomSheetSubmitFeedbackBinding;
import com.swifttrack.app.presentation.viewmodels.FeedbackViewModel;

public class SubmitFeedbackBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetSubmitFeedbackBinding binding;
    private FeedbackViewModel viewModel;

    public static SubmitFeedbackBottomSheetDialogFragment newInstance() {
        return new SubmitFeedbackBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetSubmitFeedbackBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FeedbackViewModel.class);

        // Micro-interactions & Haptics
        ButterClickEffect.apply(binding.btnCloseFeedback);
        ButterClickEffect.apply(binding.btnSubmitFeedback);
        ButterClickEffect.apply(binding.btnFeedbackDone);

        binding.btnCloseFeedback.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            dismiss();
        });

        setupCategoryChipsHaptics();
        setupRatingListener();
        setupSubmitAction();
        observeViewModel();
    }

    private void setupCategoryChipsHaptics() {
        for (int i = 0; i < binding.chipGroupFeedbackCategories.getChildCount(); i++) {
            View child = binding.chipGroupFeedbackCategories.getChildAt(i);
            if (child instanceof Chip) {
                ButterClickEffect.apply(child);
                child.setOnClickListener(v -> v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY));
            }
        }
        for (int i = 0; i < binding.chipGroupRecommend.getChildCount(); i++) {
            View child = binding.chipGroupRecommend.getChildAt(i);
            if (child instanceof Chip) {
                ButterClickEffect.apply(child);
                child.setOnClickListener(v -> v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY));
            }
        }
    }

    private void setupRatingListener() {
        binding.ratingBarFeedback.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (binding == null) return;
            ratingBar.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (rating <= 2) {
                binding.tvDynamicRatingQuestion.setText("What could we improve?");
            } else if (rating >= 4) {
                binding.tvDynamicRatingQuestion.setText("What do you like most about SwiftTrack?");
            } else {
                binding.tvDynamicRatingQuestion.setText("Tell us what you think about your experience...");
            }
        });
    }

    private String getSelectedFeedbackCategoryString() {
        int checkedId = binding.chipGroupFeedbackCategories.getCheckedChipId();
        if (checkedId == R.id.chip_fb_ui) return "UI / UX Design";
        if (checkedId == R.id.chip_fb_perf) return "Performance & Speed";
        if (checkedId == R.id.chip_fb_booking) return "Booking Experience";
        if (checkedId == R.id.chip_fb_live) return "Live Train Data";
        if (checkedId == R.id.chip_fb_tickets) return "Tickets & QR Scanner";
        if (checkedId == R.id.chip_fb_account) return "Account & Biometrics";
        if (checkedId == R.id.chip_fb_feature) return "Feature Request";
        if (checkedId == R.id.chip_fb_bug) return "Bug Report";
        return "General Feedback";
    }

    private void setupSubmitAction() {
        binding.btnSubmitFeedback.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            float rating = binding.ratingBarFeedback.getRating();
            String category = getSelectedFeedbackCategoryString();
            String message = binding.etFeedbackMessage.getText() != null ? binding.etFeedbackMessage.getText().toString().trim() : "";

            String recommend = "YES";
            int checkedChipId = binding.chipGroupRecommend.getCheckedChipId();
            if (checkedChipId == R.id.chip_rec_maybe) recommend = "MAYBE";
            else if (checkedChipId == R.id.chip_rec_no) recommend = "NO";

            viewModel.submitFeedback(rating, category, message, recommend);
        });
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (binding == null || state == null) return;
            switch (state.state) {
                case IDLE:
                    binding.btnSubmitFeedback.setEnabled(true);
                    binding.btnSubmitFeedback.setText("Submit Feedback");
                    break;
                case VALIDATING:
                    binding.tilFeedbackMessage.setError(null);
                    break;
                case SUBMITTING:
                    binding.btnSubmitFeedback.setEnabled(false);
                    binding.btnSubmitFeedback.setText("Sending Feedback...");
                    break;
                case SUCCESS:
                    binding.containerFeedbackForm.setVisibility(View.GONE);
                    binding.containerFeedbackSuccess.setVisibility(View.VISIBLE);
                    binding.btnFeedbackDone.setOnClickListener(v -> {
                        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        dismiss();
                    });
                    break;
                case ERROR:
                    binding.btnSubmitFeedback.setEnabled(true);
                    binding.btnSubmitFeedback.setText("Submit Feedback");
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
