package com.swifttrack.app.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.swifttrack.app.R;
import com.swifttrack.app.databinding.FragmentFareSelectionBinding;
import com.swifttrack.app.presentation.viewmodels.HomeBookViewModel;

import java.util.Locale;

public class FareSelectionFragment extends Fragment {

    private FragmentFareSelectionBinding binding;
    private HomeBookViewModel viewModel;
    private boolean isStandardDetailsExpanded = false;
    private boolean isPremierDetailsExpanded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFareSelectionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeBookViewModel.class);

        // Apply Strikethrough Paint Flag to Base Prices
        binding.tvStandardBasePrice.setPaintFlags(binding.tvStandardBasePrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        binding.tvPremierBasePrice.setPaintFlags(binding.tvPremierBasePrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);

        viewModel.getIsReturnJourney().observe(getViewLifecycleOwner(), isRet -> updateTicketTypeLabels());
        viewModel.getReturnDate().observe(getViewLifecycleOwner(), retDate -> updateTicketTypeLabels());

        // Dynamic Live Base Fare Observation for Standard Class (No pre-applied discount)
        viewModel.getStandardBaseFare().observe(getViewLifecycleOwner(), base -> {
            if (base != null && binding != null) {
                String formattedBase = String.format(Locale.getDefault(), "£%.2f", base);
                binding.tvStandardPrice.setText(formattedBase);
                binding.tvStandardBreakdownBase.setText(formattedBase);
                binding.tvStandardBreakdownTotal.setText(formattedBase);
                binding.btnSelectStandard.setText("Select Standard Class (" + formattedBase + ")");

                binding.tvStandardBasePrice.setVisibility(View.GONE);
                binding.tvStandardBreakdownDiscount.setText("£0.00");
            }
        });

        // Dynamic Live Base Fare Observation for Premier Class (No pre-applied discount)
        viewModel.getPremierBaseFare().observe(getViewLifecycleOwner(), base -> {
            if (base != null && binding != null) {
                String formattedBase = String.format(Locale.getDefault(), "£%.2f", base);
                binding.tvPremierPrice.setText(formattedBase);
                binding.tvPremierBreakdownBase.setText(formattedBase);
                binding.tvPremierBreakdownTotal.setText(formattedBase);
                binding.btnSelectPremier.setText("Select PREMIER+ Class (" + formattedBase + ")");

                binding.tvPremierBasePrice.setVisibility(View.GONE);
                binding.tvPremierBreakdownDiscount.setText("£0.00");
            }
        });

        // Collapsible Ticket Details Toggles
        binding.tvToggleStandardDetails.setOnClickListener(v -> {
            isStandardDetailsExpanded = !isStandardDetailsExpanded;
            binding.layoutStandardFullDetails.setVisibility(isStandardDetailsExpanded ? View.VISIBLE : View.GONE);
            binding.tvToggleStandardDetails.setText(isStandardDetailsExpanded ? "ⓘ Hide full ticket details ˄" : "ⓘ Show full ticket details ˅");
        });

        binding.tvTogglePremierDetails.setOnClickListener(v -> {
            isPremierDetailsExpanded = !isPremierDetailsExpanded;
            binding.layoutPremierFullDetails.setVisibility(isPremierDetailsExpanded ? View.VISIBLE : View.GONE);
            binding.tvTogglePremierDetails.setText(isPremierDetailsExpanded ? "ⓘ Hide full ticket details ˄" : "ⓘ Show full ticket details ˅");
        });

        // Promo Code Application
        binding.btnApplyPromo.setOnClickListener(v -> {
            String code = binding.etPromoCode.getText() != null ? binding.etPromoCode.getText().toString().trim() : "";
            if (!code.isEmpty()) {
                viewModel.setPromoCode(code);
                Toast.makeText(requireContext(), "Promo code '" + code + "' applied!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Please enter a valid promo code", Toast.LENGTH_SHORT).show();
            }
        });

        // Card Click & Button Direct Smooth Navigation Handlers
        binding.cardStandard.setOnClickListener(v -> selectClassAndNavigate("STANDARD"));
        binding.btnSelectStandard.setOnClickListener(v -> selectClassAndNavigate("STANDARD"));

        binding.cardFirst.setOnClickListener(v -> selectClassAndNavigate("PREMIER"));
        binding.btnSelectPremier.setOnClickListener(v -> selectClassAndNavigate("PREMIER"));

        binding.btnContinueCheckout.setOnClickListener(v -> navigateToCheckout());
    }

    private void selectClassAndNavigate(String travelClass) {
        if (viewModel != null) {
            viewModel.setSelectedClass(travelClass);
        }
        navigateToCheckout();
    }

    private void navigateToCheckout() {
        try {
            NavHostFragment.findNavController(FareSelectionFragment.this)
                    .navigate(R.id.action_fareSelection_to_checkout);
        } catch (Exception e) {
            try {
                if (getView() != null) {
                    Navigation.findNavController(getView()).navigate(R.id.action_fareSelection_to_checkout);
                }
            } catch (Exception ignored) {}
        }
    }

    private void updateTicketTypeLabels() {
        if (binding == null || viewModel == null) return;
        boolean isReturn = Boolean.TRUE.equals(viewModel.getIsReturnJourney().getValue());
        String retDate = viewModel.getReturnDate().getValue() != null ? viewModel.getReturnDate().getValue() : "";
        String label = isReturn ? ("TICKET TYPE: Return (" + retDate + ")") : "TICKET TYPE: Single";
        binding.tvStandardTicketType.setText(label);
        binding.tvPremierTicketType.setText(label);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
