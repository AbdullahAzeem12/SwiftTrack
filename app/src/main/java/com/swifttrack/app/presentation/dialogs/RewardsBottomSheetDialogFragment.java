package com.swifttrack.app.presentation.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.swifttrack.app.R;
import com.swifttrack.app.data.model.Offer;
import com.swifttrack.app.data.model.OfferEligibilityResult;
import com.swifttrack.app.data.repository.OfferRepository;
import com.swifttrack.app.data.repository.RewardsRepository;
import com.swifttrack.app.databinding.BottomSheetRewardsBinding;

import java.util.List;
import java.util.Locale;

public class RewardsBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetRewardsBinding binding;
    private RewardsRepository rewardsRepository;
    private OfferRepository offerRepository;

    public static RewardsBottomSheetDialogFragment newInstance() {
        return new RewardsBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetRewardsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rewardsRepository = RewardsRepository.getInstance(requireActivity().getApplication());
        offerRepository = OfferRepository.getInstance(requireContext());

        binding.btnCloseRewards.setOnClickListener(v -> dismiss());

        setupRewardsObservation();
        setupPromoCodeApply();
        observeActiveOffers();
    }

    private void setupRewardsObservation() {
        rewardsRepository.getRewardSummaryLiveData().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null && binding != null) {
                int pts = summary.getPointsBalance();
                double poundValue = (double) pts / 100.0;
                binding.tvRewardsSheetPoints.setText(pts + " Points");
                binding.tvRewardsSheetValue.setText(String.format(Locale.UK, "Equivalent to £%.2f discount on Heathrow Express tickets", poundValue));
                observeActiveOffers();
            }
        });
    }

    private void setupPromoCodeApply() {
        binding.btnApplyPromoCode.setOnClickListener(v -> {
            String code = binding.etPromoCode.getText() != null ? binding.etPromoCode.getText().toString().trim().toUpperCase() : "";
            if (code.isEmpty()) {
                binding.tilPromoCode.setError("Enter a promo code");
                return;
            }
            binding.tilPromoCode.setError(null);

            boolean isValid = rewardsRepository.isValidPromoCode(code);
            binding.tvPromoResultMsg.setVisibility(View.VISIBLE);

            if (isValid) {
                binding.tvPromoResultMsg.setText("✅ Code SWIFTTRACK20 Applied! 20% discount on Peak & Off-Peak Express fares.");
                binding.tvPromoResultMsg.setTextColor(getResources().getColor(R.color.status_green, null));
            } else {
                binding.tvPromoResultMsg.setText("❌ Invalid or expired promo code.");
                binding.tvPromoResultMsg.setTextColor(getResources().getColor(R.color.status_red, null));
            }
        });
    }

    private void observeActiveOffers() {
        if (binding == null) return;
        offerRepository.getOffersLiveData().observe(getViewLifecycleOwner(), offers -> {
            if (offers != null && binding != null) {
                binding.containerOffersList.removeAllViews();
                String userId = rewardsRepository.resolveActiveUserId();
                int availablePts = rewardsRepository.getAvailablePoints(userId);

                for (Offer offer : offers) {
                    OfferEligibilityResult result = offerRepository.evaluateOfferEligibility(
                            offer,
                            "SINGLE",
                            System.currentTimeMillis() + (15 * 24 * 60 * 60 * 1000L),
                            25.0,
                            availablePts
                    );
                    addOfferCard(offer, result);
                }
            }
        });
    }

    private void addOfferCard(Offer offer, OfferEligibilityResult result) {
        View card = getLayoutInflater().inflate(R.layout.item_offer_card, binding.containerOffersList, false);
        TextView tvTitle = card.findViewById(R.id.tv_offer_title);
        TextView tvDesc = card.findViewById(R.id.tv_offer_desc);
        TextView tvStatus = card.findViewById(R.id.tv_offer_status);

        tvTitle.setText(offer.getTitle());
        tvDesc.setText(offer.getDescription());
        tvStatus.setText(result.getStatusBadgeText());

        card.setOnClickListener(v -> {
            OfferDetailBottomSheetDialogFragment detailSheet = OfferDetailBottomSheetDialogFragment.newInstance(offer.getOfferId());
            detailSheet.show(getParentFragmentManager(), "OfferDetailSheet_" + offer.getOfferId());
        });

        binding.containerOffersList.addView(card);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
