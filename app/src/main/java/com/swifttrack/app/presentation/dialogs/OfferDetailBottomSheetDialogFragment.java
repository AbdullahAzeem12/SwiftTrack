package com.swifttrack.app.presentation.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.swifttrack.app.R;
import com.swifttrack.app.core.util.AlertUtil;
import com.swifttrack.app.data.model.Offer;
import com.swifttrack.app.data.model.OfferEligibilityResult;
import com.swifttrack.app.data.repository.OfferRepository;
import com.swifttrack.app.data.repository.RewardsRepository;
import com.swifttrack.app.databinding.BottomSheetOfferDetailBinding;
import com.swifttrack.app.presentation.viewmodels.HomeBookViewModel;

public class OfferDetailBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_OFFER_ID = "arg_offer_id";

    private BottomSheetOfferDetailBinding binding;
    private OfferRepository offerRepository;
    private RewardsRepository rewardsRepository;
    private HomeBookViewModel homeBookViewModel;

    private Offer currentOffer;

    public static OfferDetailBottomSheetDialogFragment newInstance(String offerId) {
        OfferDetailBottomSheetDialogFragment fragment = new OfferDetailBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OFFER_ID, offerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetOfferDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        offerRepository = OfferRepository.getInstance(requireContext());
        rewardsRepository = RewardsRepository.getInstance(requireContext());

        if (getActivity() != null) {
            homeBookViewModel = new ViewModelProvider(requireActivity()).get(HomeBookViewModel.class);
        }

        String offerId = getArguments() != null ? getArguments().getString(ARG_OFFER_ID) : "offer_swifttrack20";
        currentOffer = offerRepository.getOfferById(offerId);

        binding.btnCloseOfferDetail.setOnClickListener(v -> dismiss());

        renderOfferDetails();
    }

    private void renderOfferDetails() {
        if (currentOffer == null || binding == null) return;

        binding.tvDetailOfferIcon.setText(currentOffer.getIcon());
        binding.tvDetailOfferTitle.setText(currentOffer.getTitle());
        binding.tvDetailOfferSubtitle.setText(currentOffer.getDescription());

        String userId = rewardsRepository.resolveActiveUserId();
        int availablePoints = rewardsRepository.getAvailablePoints(userId);

        String journeyType = "SINGLE";
        long travelDateMillis = System.currentTimeMillis() + (15 * 24 * 60 * 60 * 1000L); // Default 15 days ahead
        double baseFare = 25.0;

        if (homeBookViewModel != null) {
            Boolean isRet = homeBookViewModel.getIsReturnJourney().getValue();
            if (Boolean.TRUE.equals(isRet)) journeyType = "RETURN";
            Double fare = homeBookViewModel.getStandardBaseFare().getValue();
            if (fare != null) baseFare = fare;
        }

        OfferEligibilityResult result = offerRepository.evaluateOfferEligibility(
                currentOffer,
                journeyType,
                travelDateMillis,
                baseFare,
                availablePoints
        );

        binding.tvDetailOfferBadge.setText(result.getStatusBadgeText());
        if (result.isEligible()) {
            binding.tvDetailOfferBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green));
            binding.tvDetailOfferBadge.setBackgroundResource(R.drawable.bg_pill_badge);
        } else if ("INSUFFICIENT POINTS".equals(result.getStatusBadgeText())) {
            binding.tvDetailOfferBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_amber));
            binding.tvDetailOfferBadge.setBackgroundResource(R.drawable.bg_pill_badge);
        } else {
            binding.tvDetailOfferBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
            binding.tvDetailOfferBadge.setBackgroundResource(R.drawable.bg_pill_badge);
        }

        binding.tvDetailEligibilityReason.setText(result.getReason());

        // Points summary setup
        if (currentOffer.getPointsRequired() > 0) {
            binding.cardPointsSummary.setVisibility(View.VISIBLE);
            binding.tvSummaryUserPoints.setText(availablePoints + " Points");
            binding.tvSummaryRequiredPoints.setText(currentOffer.getPointsRequired() + " Points");

            if (availablePoints >= currentOffer.getPointsRequired()) {
                int remaining = availablePoints - currentOffer.getPointsRequired();
                binding.tvSummaryRemainingLabel.setText("Balance After Redemption:");
                binding.tvSummaryRemainingPoints.setText(remaining + " Points");
                binding.tvSummaryRemainingPoints.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green));
            } else {
                int shortfall = currentOffer.getPointsRequired() - availablePoints;
                binding.tvSummaryRemainingLabel.setText("Points Shortfall:");
                binding.tvSummaryRemainingPoints.setText("-" + shortfall + " Points Needed");
                binding.tvSummaryRemainingPoints.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red));
            }
        } else {
            binding.cardPointsSummary.setVisibility(View.GONE);
        }

        // Action Button setup
        if (!result.isEligible()) {
            binding.btnActionOffer.setEnabled(false);
            binding.btnActionOffer.setAlpha(0.6f);
            if ("INSUFFICIENT POINTS".equals(result.getStatusBadgeText())) {
                binding.btnActionOffer.setText("Earn " + result.getPointsShortfall() + " More Points to Unlock 🔒");
            } else {
                binding.btnActionOffer.setText("Requirements Not Met 🔒");
            }
        } else {
            binding.btnActionOffer.setEnabled(true);
            binding.btnActionOffer.setAlpha(1.0f);

            if (currentOffer.getOfferType() == Offer.OfferType.PROMO_CODE) {
                binding.btnActionOffer.setText("Apply Code " + currentOffer.getPromoCode() + " 🎉");
                binding.btnActionOffer.setOnClickListener(v -> {
                    if (homeBookViewModel != null) {
                        homeBookViewModel.applyCheckoutPromo(currentOffer.getPromoCode());
                    }
                    dismiss();
                    if (getActivity() != null && getActivity().findViewById(android.R.id.content) != null) {
                        AlertUtil.showSuccessSnackbar(getActivity().findViewById(android.R.id.content), "✅ Code SWIFTTRACK20 Applied! 20% discount active.");
                    }
                });
            } else if (currentOffer.getOfferType() == Offer.OfferType.ADVANCE_BOOKING_DISCOUNT) {
                binding.btnActionOffer.setText("Apply 30% Return Discount ✨");
                binding.btnActionOffer.setOnClickListener(v -> {
                    if (homeBookViewModel != null) {
                        homeBookViewModel.setReturnJourney(true);
                        homeBookViewModel.applyCheckoutPromo("SWIFTTRACK20");
                    }
                    dismiss();
                    if (getActivity() != null && getActivity().findViewById(android.R.id.content) != null) {
                        AlertUtil.showSuccessSnackbar(getActivity().findViewById(android.R.id.content), "✈️ Airport Return Special Applied! 30% discount active.");
                    }
                });
            } else if (currentOffer.getOfferType() == Offer.OfferType.REWARD_POINTS_REDEMPTION) {
                binding.btnActionOffer.setText("Redeem Upgrade for 500 Points ⭐");
                binding.btnActionOffer.setOnClickListener(v -> performFirstClassRedemption(userId));
            }
        }
    }

    private void performFirstClassRedemption(String userId) {
        binding.btnActionOffer.setEnabled(false);
        binding.btnActionOffer.setText("Redeeming Points... ⏳");

        offerRepository.confirmFirstClassUpgradeRedemption(userId, "bkg_upgrade", new OfferRepository.OfferRedemptionCallback() {
            @Override
            public void onSuccess(String redemptionId) {
                if (homeBookViewModel != null) {
                    homeBookViewModel.setSelectedClass("PREMIER");
                }
                dismiss();
                if (getActivity() != null && getActivity().findViewById(android.R.id.content) != null) {
                    AlertUtil.showSuccessSnackbar(getActivity().findViewById(android.R.id.content), "⭐ First Class Upgrade Redeemed! 500 points deducted.");
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (binding != null) {
                    binding.btnActionOffer.setEnabled(true);
                    binding.btnActionOffer.setText("Redeem Upgrade for 500 Points ⭐");
                }
                Toast.makeText(requireContext(), "Redemption Error: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
