package com.swifttrack.app.presentation.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.swifttrack.app.R;
import com.swifttrack.app.data.local.StationEntity;
import com.swifttrack.app.data.model.FareQuote;
import com.swifttrack.app.data.repository.RewardsRepository;
import com.swifttrack.app.databinding.FragmentCheckoutBinding;
import com.swifttrack.app.presentation.viewmodels.HomeBookViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.swifttrack.app.core.security.KeystoreManager;
import com.swifttrack.app.core.util.ValidationUtils;
import android.text.Editable;
import android.text.TextWatcher;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private HomeBookViewModel viewModel;
    private RewardsRepository rewardsRepository;

    private boolean isPromoExpanded = false;
    private boolean isRewardsExpanded = false;
    private String activeUserId;

    // Production Real-Time Fare Quote Timer Handler
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateQuoteTimerUI();
            timerHandler.postDelayed(this, 1000L);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeBookViewModel.class);
        rewardsRepository = RewardsRepository.getInstance(requireContext());
        activeUserId = rewardsRepository.resolveActiveUserId();

        // Ensure an active 15-minute fare quote hold is initialized
        viewModel.ensureActiveFareQuote(activeUserId);

        setupEmailDeliveryInput();
        setupExpandableAccordions();
        setupPromoSystem();
        setupRewardsSystem();
        setupQuoteRefreshFlow();
        observeRouteAndPassengerDetails();
        observeCheckoutPricing();

        binding.btnProceedPayment.setOnClickListener(v -> handleProceedToPayment(view));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && activeUserId != null) {
            viewModel.ensureActiveFareQuote(activeUserId);
        }
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void updateQuoteTimerUI() {
        if (binding == null || viewModel == null) return;
        FareQuote quote = viewModel.getCurrentFareQuote().getValue();
        if (quote == null) return;

        if (quote.isExpired()) {
            binding.tvQuoteTimer.setText("Price Quote Expired! Tap to Refresh Fare.");
            int errorColor = ContextCompat.getColor(requireContext(), R.color.status_red);
            binding.tvQuoteTimer.setTextColor(errorColor);
            binding.imgQuoteTimerIcon.setColorFilter(errorColor);
            binding.layoutQuoteBanner.setBackgroundResource(R.drawable.bg_pill_badge);
            binding.btnRefreshFare.setVisibility(View.VISIBLE);
            binding.btnProceedPayment.setEnabled(false);
            binding.btnProceedPayment.setAlpha(0.5f);
            binding.btnProceedPayment.setText("Quote Expired - Refresh Fare to Continue");
            return;
        }

        binding.btnProceedPayment.setEnabled(true);
        binding.btnProceedPayment.setAlpha(1.0f);
        binding.btnRefreshFare.setVisibility(View.GONE);

        long remainingSec = quote.getRemainingSeconds();
        long min = remainingSec / 60;
        long sec = remainingSec % 60;
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", min, sec);

        binding.tvQuoteTimer.setText("Price Quote Guaranteed for " + timeStr + " mins");

        int activeColor;
        if (remainingSec > 300) {
            activeColor = ContextCompat.getColor(requireContext(), R.color.quote_banner_text);
        } else if (remainingSec > 60) {
            activeColor = ContextCompat.getColor(requireContext(), R.color.accent_amber_dark);
        } else {
            activeColor = ContextCompat.getColor(requireContext(), R.color.status_red);
        }
        binding.tvQuoteTimer.setTextColor(activeColor);
        binding.imgQuoteTimerIcon.setColorFilter(activeColor);
        binding.layoutQuoteBanner.setBackgroundResource(R.drawable.bg_pill_badge);
    }

    private void setupQuoteRefreshFlow() {
        binding.btnRefreshFare.setOnClickListener(v -> {
            viewModel.refreshFareQuote(activeUserId);
            updateQuoteTimerUI();
            Toast.makeText(requireContext(), "✓ Fare revalidated & guaranteed for 15 mins!", Toast.LENGTH_SHORT).show();
        });

        binding.layoutQuoteBanner.setOnClickListener(v -> {
            FareQuote q = viewModel.getCurrentFareQuote().getValue();
            if (q != null && q.isExpired()) {
                viewModel.refreshFareQuote(activeUserId);
                updateQuoteTimerUI();
                Toast.makeText(requireContext(), "✓ Fare revalidated & guaranteed for 15 mins!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupExpandableAccordions() {
        binding.layoutHeaderPromo.setOnClickListener(v -> togglePromoAccordion());
        binding.layoutHeaderRewards.setOnClickListener(v -> toggleRewardsAccordion());
    }

    private void togglePromoAccordion() {
        isPromoExpanded = !isPromoExpanded;
        TransitionManager.beginDelayedTransition(binding.cardCheckoutPromo, new AutoTransition());
        binding.layoutExpandablePromo.setVisibility(isPromoExpanded ? View.VISIBLE : View.GONE);
        binding.tvTogglePromoIndicator.setText(isPromoExpanded ? "-" : "+");
    }

    private void toggleRewardsAccordion() {
        isRewardsExpanded = !isRewardsExpanded;
        TransitionManager.beginDelayedTransition(binding.cardCheckoutRewards, new AutoTransition());
        binding.layoutExpandableRewards.setVisibility(isRewardsExpanded ? View.VISIBLE : View.GONE);
        binding.tvToggleRewardsIndicator.setText(isRewardsExpanded ? "-" : "+");

        if (isRewardsExpanded) {
            int availablePts = rewardsRepository.getUserRewardsBalance(activeUserId);
            binding.tvRewardsAvailablePoints.setText("Available Points: " + availablePts + " pts");
        }
    }

    private void setupPromoSystem() {
        binding.btnApplyCheckoutPromo.setOnClickListener(v -> {
            hideKeyboard();
            String code = binding.etCheckoutPromoInput.getText() != null ? binding.etCheckoutPromoInput.getText().toString() : "";
            if (code.trim().isEmpty()) {
                showPromoStatus("Please enter a valid promo code", false);
                return;
            }

            boolean applied = viewModel.applyCheckoutPromo(code);
            if (applied) {
                binding.btnRemoveCheckoutPromo.setVisibility(View.VISIBLE);
                showPromoStatus("✓ 20% discount applied (SWIFTTRACK20)", true);
            } else {
                showPromoStatus("Invalid or expired promo code", false);
            }
        });

        binding.btnRemoveCheckoutPromo.setOnClickListener(v -> {
            hideKeyboard();
            viewModel.removeCheckoutPromo();
            binding.etCheckoutPromoInput.setText("");
            binding.btnRemoveCheckoutPromo.setVisibility(View.GONE);
            binding.tvPromoStatusMsg.setVisibility(View.GONE);
        });
    }

    private void showPromoStatus(String msg, boolean isSuccess) {
        binding.tvPromoStatusMsg.setVisibility(View.VISIBLE);
        binding.tvPromoStatusMsg.setText(msg);
        binding.tvPromoStatusMsg.setTextColor(ContextCompat.getColor(requireContext(),
                isSuccess ? R.color.status_green : R.color.status_red));
    }

    private void setupRewardsSystem() {
        rewardsRepository.getRewardSummaryLiveData().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null && binding != null) {
                int userAvailablePts = summary.getPointsBalance();
                binding.tvRewardsAvailablePoints.setText("Available Points: " + userAvailablePts + " pts");
                Integer pointsUsed = viewModel.getRewardPointsToUse().getValue();
                if (pointsUsed != null && pointsUsed > 0) {
                    updateRewardsSummaryUI(pointsUsed, userAvailablePts);
                }
            }
        });

        binding.btnApplyRewards.setOnClickListener(v -> {
            hideKeyboard();
            String ptsStr = binding.etRewardsPointsInput.getText() != null ? binding.etRewardsPointsInput.getText().toString() : "";
            if (ptsStr.trim().isEmpty()) {
                showRewardsStatus("Please enter points to redeem", false);
                return;
            }

            try {
                int points = Integer.parseInt(ptsStr.trim());
                int currentAvailable = rewardsRepository.getUserRewardsBalance(activeUserId);

                if (points <= 0) {
                    showRewardsStatus("Points must be greater than 0", false);
                    return;
                }
                if (points > currentAvailable) {
                    showRewardsStatus("Insufficient points. Available: " + currentAvailable + " pts", false);
                    return;
                }

                // Reserve points atomically
                boolean reserved = rewardsRepository.reservePoints(activeUserId, points, viewModel.getCurrentFareQuote().getValue() != null ? viewModel.getCurrentFareQuote().getValue().getBookingId() : "temp");
                if (reserved) {
                    viewModel.applyCheckoutRewards(points);
                    updateRewardsSummaryUI(viewModel.getRewardPointsToUse().getValue(), rewardsRepository.getUserRewardsBalance(activeUserId));
                    showRewardsStatus("✓ Rewards discount applied & points reserved!", true);
                } else {
                    showRewardsStatus("Unable to reserve points. Please try again.", false);
                }

            } catch (NumberFormatException e) {
                showRewardsStatus("Invalid number format", false);
            }
        });

        binding.btnRemoveRewards.setOnClickListener(v -> {
            hideKeyboard();
            Integer ptsUsed = viewModel.getRewardPointsToUse().getValue();
            if (ptsUsed != null && ptsUsed > 0) {
                rewardsRepository.releaseReservedPoints(activeUserId, ptsUsed, viewModel.getCurrentFareQuote().getValue() != null ? viewModel.getCurrentFareQuote().getValue().getBookingId() : "temp");
            }
            viewModel.removeCheckoutRewards();
            binding.etRewardsPointsInput.setText("");
            binding.btnRemoveRewards.setVisibility(View.GONE);
            binding.layoutRewardsSummaryPreview.setVisibility(View.GONE);
            binding.tvRewardsStatusMsg.setVisibility(View.GONE);
        });
    }

    private void updateRewardsSummaryUI(int pointsUsed, int availablePts) {
        if (pointsUsed <= 0) return;
        double discount = rewardsRepository.calculateRewardDiscount(pointsUsed);
        int remaining = Math.max(0, availablePts);

        binding.layoutRewardsSummaryPreview.setVisibility(View.VISIBLE);
        binding.btnRemoveRewards.setVisibility(View.VISIBLE);
        binding.tvRewardsPointsUsedLabel.setText("Points Reserved: " + pointsUsed + " pts");
        binding.tvRewardsDiscountCalcLabel.setText(String.format(Locale.getDefault(), "Reward Discount: -£%.2f", discount));
        binding.tvRewardsRemainingPointsLabel.setText("Spendable Balance: " + remaining + " pts");
    }

    private void showRewardsStatus(String msg, boolean isSuccess) {
        binding.tvRewardsStatusMsg.setVisibility(View.VISIBLE);
        binding.tvRewardsStatusMsg.setText(msg);
        binding.tvRewardsStatusMsg.setTextColor(ContextCompat.getColor(requireContext(),
                isSuccess ? R.color.status_green : R.color.status_red));
    }

    private void observeRouteAndPassengerDetails() {
        viewModel.getOriginStation().observe(getViewLifecycleOwner(), origin -> updateRouteText());
        viewModel.getDestinationStation().observe(getViewLifecycleOwner(), dest -> updateRouteText());
        viewModel.getIsReturnJourney().observe(getViewLifecycleOwner(), isRet -> updateRouteText());
        viewModel.getPassengerSummary().observe(getViewLifecycleOwner(), pax -> updateRouteText());
        viewModel.getSelectedClass().observe(getViewLifecycleOwner(), cls -> updateRouteText());
        viewModel.getTravelDate().observe(getViewLifecycleOwner(), date -> updateRouteText());
        viewModel.getReturnDate().observe(getViewLifecycleOwner(), retDate -> updateRouteText());
        viewModel.getReturnDepartureTime().observe(getViewLifecycleOwner(), retTime -> updateRouteText());
    }

    private void updateRouteText() {
        if (binding == null || viewModel == null) return;

        StationEntity originStation = viewModel.getOriginStation().getValue();
        StationEntity destStation = viewModel.getDestinationStation().getValue();

        String originName = originStation != null ? originStation.name : "London Paddington";
        String originCode = originStation != null ? originStation.code : "PAD";

        String destName = destStation != null ? destStation.name : "Heathrow Airport";
        String destCode = destStation != null ? destStation.code : "LHR";

        boolean isReturn = Boolean.TRUE.equals(viewModel.getIsReturnJourney().getValue());
        String arrow = isReturn ? " ↔ " : " ➔ ";

        binding.tvCheckoutRoute.setText(originName + " (" + originCode + ")" + arrow + destName + " (" + destCode + ")");

        String travelDate = viewModel.getTravelDate().getValue() != null ? viewModel.getTravelDate().getValue() : "Today";
        String returnDate = viewModel.getReturnDate().getValue() != null ? viewModel.getReturnDate().getValue() : "";
        String returnTime = viewModel.getReturnDepartureTime().getValue() != null ? viewModel.getReturnDepartureTime().getValue() : "18:10 PM";
        String paxSummary = viewModel.getPassengerSummary().getValue() != null ? viewModel.getPassengerSummary().getValue() : "1 Adult";
        String selectedClass = viewModel.getSelectedClass().getValue() != null ? viewModel.getSelectedClass().getValue() : "STANDARD";
        String classLabel = "PREMIER".equalsIgnoreCase(selectedClass) ? "PREMIER+ CLASS" : "STANDARD CLASS";

        binding.tvCheckoutClassBadge.setText(classLabel);
        String returnSnippet = isReturn ? (" (Return: " + returnDate + " • " + returnTime + ")") : " (Single Journey)";
        binding.tvCheckoutDetailsSubtitle.setText(travelDate + " • " + paxSummary + " • " + ("PREMIER".equalsIgnoreCase(selectedClass) ? "Premier+ Class" : "Standard Class") + returnSnippet);

        if ("PREMIER".equalsIgnoreCase(selectedClass)) {
            binding.tvClassFeaturesTitle.setText("INCLUDED WITH PREMIER+ CLASS (" + paxSummary.toUpperCase() + ")");
            binding.tvClassFeaturesBody.setText("✨ Gourmet refreshments & dining • 🪑 Extra legroom reclining seats • ⚡ Power & USB at every seat • 📶 Fast 5G Wi-Fi • 🛡️ 100% Flexible cancellation & priority gate boarding");
            binding.tvClassFeaturesTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_amber_dark));
        } else {
            binding.tvClassFeaturesTitle.setText("INCLUDED WITH STANDARD CLASS (" + paxSummary.toUpperCase() + ")");
            binding.tvClassFeaturesBody.setText("⚡ Power sockets at every seat • 📶 Free High-Speed Wi-Fi • 🧳 Spacious luggage racks • 💺 Ergonomic seating • 🛡️ Flexible digital ticket with 100% refund protection");
            binding.tvClassFeaturesTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_indigo));
        }
    }

    private void observeCheckoutPricing() {
        viewModel.getPromoDiscountAmount().observe(getViewLifecycleOwner(), promo -> updateCheckoutPricingDisplay());
        viewModel.getRewardDiscountAmount().observe(getViewLifecycleOwner(), rwd -> updateCheckoutPricingDisplay());
        viewModel.getFinalPayableTotal().observe(getViewLifecycleOwner(), total -> updateCheckoutPricingDisplay());
    }

    private void updateCheckoutPricingDisplay() {
        if (binding == null || viewModel == null) return;

        double promo = viewModel.getPromoDiscountAmount().getValue() != null ? viewModel.getPromoDiscountAmount().getValue() : 0.00;
        double rwd = viewModel.getRewardDiscountAmount().getValue() != null ? viewModel.getRewardDiscountAmount().getValue() : 0.00;
        double total = viewModel.getFinalPayableTotal().getValue() != null ? viewModel.getFinalPayableTotal().getValue() : 25.00;
        double base = total + promo + rwd;

        binding.tvCheckoutSubtotal.setText(String.format(Locale.getDefault(), "£%.2f", base));

        if (promo > 0) {
            binding.rowPromo.setVisibility(View.VISIBLE);
            binding.tvCheckoutDiscount.setText(String.format(Locale.getDefault(), "-£%.2f", promo));
        } else {
            binding.rowPromo.setVisibility(View.GONE);
        }

        if (rwd > 0) {
            binding.rowRewardsDiscount.setVisibility(View.VISIBLE);
            binding.tvCheckoutRewardsDiscount.setText(String.format(Locale.getDefault(), "-£%.2f", rwd));
        } else {
            binding.rowRewardsDiscount.setVisibility(View.GONE);
        }

        binding.tvTotalPrice.setText(String.format(Locale.getDefault(), "£%.2f", total));
        binding.btnProceedPayment.setText(String.format(Locale.getDefault(), "Proceed to Payment • £%.2f 🔒", total));
    }

    private void setupEmailDeliveryInput() {
        if (binding == null) return;
        
        // Auto-fill registered user email if available
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userEmail = "";
        if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().trim().isEmpty()) {
            userEmail = currentUser.getEmail().trim();
        } else {
            KeystoreManager keystoreManager = new KeystoreManager(requireContext());
            userEmail = keystoreManager.getUserEmail();
        }

        if (!userEmail.isEmpty()) {
            binding.etContactEmail.setText(userEmail);
        }

        // Real-time error dismissal when typing
        binding.etContactEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (binding != null && binding.tilContactEmail != null) {
                    binding.tilContactEmail.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void handleProceedToPayment(View view) {
        FareQuote quote = viewModel.getCurrentFareQuote().getValue();
        if (quote != null && quote.isExpired()) {
            Toast.makeText(requireContext(), "Your price quote expired. Tap 'Refresh Fare' to get guaranteed price.", Toast.LENGTH_LONG).show();
            return;
        }

        // Validate Contact Delivery Email Address
        String deliveryEmail = binding.etContactEmail.getText() != null ? binding.etContactEmail.getText().toString().trim() : "";
        if (deliveryEmail.isEmpty()) {
            binding.tilContactEmail.setError("Please enter an email address to receive your ticket.");
            binding.etContactEmail.requestFocus();
            Toast.makeText(requireContext(), "Ticket delivery email is required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationUtils.isValidEmail(deliveryEmail)) {
            binding.tilContactEmail.setError("Please enter a valid email address format.");
            binding.etContactEmail.requestFocus();
            Toast.makeText(requireContext(), "Invalid email format. Please check your delivery address.", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.tilContactEmail.setError(null);

        // Validate Terms and Conditions Checkbox
        if (!binding.cbTerms.isChecked()) {
            Toast.makeText(requireContext(), "Please accept the fare conditions and travel terms to proceed.", Toast.LENGTH_LONG).show();
            return;
        }

        StationEntity origStation = viewModel.getOriginStation().getValue();
        StationEntity destStation = viewModel.getDestinationStation().getValue();

        String orig = origStation != null ? origStation.name + " (" + origStation.code + ")" : "London Paddington (PAD)";
        String dest = destStation != null ? destStation.name + " (" + destStation.code + ")" : "Heathrow Terminal 5 (HWV)";
        boolean isRet = Boolean.TRUE.equals(viewModel.getIsReturnJourney().getValue());
        String paxSum = viewModel.getPassengerSummary().getValue() != null ? viewModel.getPassengerSummary().getValue() : "1 Adult";
        String selCls = viewModel.getSelectedClass().getValue() != null ? viewModel.getSelectedClass().getValue() : "STANDARD";
        SimpleDateFormat todayFmt = new SimpleDateFormat("dd MMM ''yy", Locale.UK);
        Calendar cal = Calendar.getInstance();
        String fallbackToday = todayFmt.format(cal.getTime());
        cal.add(Calendar.DAY_OF_MONTH, 4);
        String fallbackReturn = todayFmt.format(cal.getTime());

        String tDate = viewModel.getTravelDate().getValue() != null ? viewModel.getTravelDate().getValue() : fallbackToday;
        String rDate = viewModel.getReturnDate().getValue() != null ? viewModel.getReturnDate().getValue() : fallbackReturn;

        double total = viewModel.getFinalPayableTotal().getValue() != null ? viewModel.getFinalPayableTotal().getValue() : 25.00;
        String promo = viewModel.getActivePromoCode().getValue() != null ? viewModel.getActivePromoCode().getValue() : "";
        int rPoints = viewModel.getRewardPointsToUse().getValue() != null ? viewModel.getRewardPointsToUse().getValue() : 0;
        double rDisc = viewModel.getRewardDiscountAmount().getValue() != null ? viewModel.getRewardDiscountAmount().getValue() : 0.00;

        String bookingId = quote != null ? quote.getBookingId() : "ST-BKG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String quoteId = quote != null ? quote.getQuoteId() : "QT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Sync delivery email to Firestore active booking session
        try {
            java.util.Map<String, Object> emailSessionUpdate = new java.util.HashMap<>();
            emailSessionUpdate.put("deliveryEmail", deliveryEmail);
            emailSessionUpdate.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            com.swifttrack.app.data.repository.BookingFirestoreRepository.getInstance(requireContext())
                    .syncBookingSessionToFirestore(activeUserId, bookingId, emailSessionUpdate);
        } catch (Exception e) {
            android.util.Log.w("CheckoutFragment", "Could not sync delivery email to session: " + e.getMessage());
        }

        Bundle args = new Bundle();
        args.putString("idempotency_key", UUID.randomUUID().toString());
        args.putString("quote_id", quoteId);
        args.putString("booking_id", bookingId);
        args.putDouble("final_payable_amount", total);
        args.putString("promo_code", promo);
        args.putInt("reward_points_used", rPoints);
        args.putDouble("reward_discount", rDisc);
        args.putString("user_id", activeUserId);
        args.putString("delivery_email", deliveryEmail);
        args.putString("contact_email", deliveryEmail);
        args.putString("origin_name", orig);
        args.putString("dest_name", dest);
        args.putBoolean("is_return", isRet);
        args.putString("passenger_summary", paxSum);
        args.putString("selected_class", selCls);
        String rDepTime = viewModel.getReturnDepartureTime().getValue() != null ? viewModel.getReturnDepartureTime().getValue() : "18:10 PM";
        String rArrTime = viewModel.getReturnArrivalTime().getValue() != null ? viewModel.getReturnArrivalTime().getValue() : "18:25 PM";

        args.putString("travel_date", tDate);
        args.putString("return_date", rDate);
        args.putString("return_departure_time", rDepTime);
        args.putString("return_arrival_time", rArrTime);

        Navigation.findNavController(view).navigate(R.id.action_checkout_to_payment, args);
    }

    private void hideKeyboard() {
        if (getActivity() != null && getActivity().getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
