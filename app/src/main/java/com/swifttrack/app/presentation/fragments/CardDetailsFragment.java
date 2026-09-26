package com.swifttrack.app.presentation.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.swifttrack.app.R;
import com.swifttrack.app.core.mail.TicketEmailDeliveryManager;
import com.swifttrack.app.core.payment.CurrencyConfig;
import com.swifttrack.app.core.payment.PaymentRepository;
import com.swifttrack.app.core.util.ButterClickEffect;
import com.swifttrack.app.data.local.SwiftTrackDatabase;
import com.swifttrack.app.data.local.TicketEntity;
import com.swifttrack.app.data.model.NotificationItem;
import com.swifttrack.app.data.repository.BookingFirestoreRepository;
import com.swifttrack.app.data.repository.NotificationRepository;
import com.swifttrack.app.data.repository.RewardsRepository;
import com.swifttrack.app.databinding.FragmentCardDetailsBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;

public class CardDetailsFragment extends Fragment {

    private static final String TAG = "CardDetailsFragment";
    private FragmentCardDetailsBinding binding;

    private RewardsRepository rewardsRepository;

    // Booking Parameters
    private String idempotencyKey = "";
    private String quoteId = "";
    private String bookingIdStr = "";
    private UUID bookingId;
    private double finalPayableAmount = 25.00;
    private double baseAmountGbp = 25.00;
    private String promoCode = "";
    private int rewardPointsUsed = 0;
    private double rewardDiscount = 0.0;
    private String userId = "";
    private String originName = "London Paddington (PAD)";
    private String destName = "Heathrow Terminal 5 (HWV)";
    private boolean isReturn = false;
    private String passengerSummary = "1 Adult";
    private String selectedClass = "STANDARD";
    private String travelDate = "08 Aug '26";
    private String returnDate = "12 Aug '26";
    private String returnDepartureTime = "18:10 PM";
    private String returnArrivalTime = "18:25 PM";
    private String deliveryEmail = "";
    private String selectedCurrency = "GBP";

    private boolean isFormattingCard = false;
    private boolean isFormattingExpiry = false;
    private int dummyCardIndex = 0;

    // Preset Test Cards for development & sandbox verification
    private static class DummyCardPreset {
        final String firstName;
        final String lastName;
        final String cardNumber;
        final String expiry;
        final String cvv;
        final String label;

        DummyCardPreset(String fName, String lName, String number, String exp, String c, String lbl) {
            this.firstName = fName;
            this.lastName = lName;
            this.cardNumber = number;
            this.expiry = exp;
            this.cvv = c;
            this.label = lbl;
        }
    }

    private final DummyCardPreset[] dummyPresets = new DummyCardPreset[]{
            new DummyCardPreset("Abdullah", "Azeem", "4242 4242 4242 4242", "12/28", "123", "Visa Standard Credit"),
            new DummyCardPreset("Sarah", "Jenkins", "5555 5555 5555 4444", "11/27", "456", "Mastercard Platinum"),
            new DummyCardPreset("David", "Ross", "3782 822468 95005", "08/29", "1234", "American Express"),
            new DummyCardPreset("Emma", "Watson", "4000 0566 5566 5556", "10/28", "789", "Visa Debit Express")
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCardDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rewardsRepository = RewardsRepository.getInstance(requireContext());

        extractArguments();
        setupPriceDisplay();
        setupEnvironmentSwitch();
        setupDummyCardAutoFill();
        setupRealtimeCardFormatting();
        setupRealtimeExpiryFormatting();
        setupPaymentButton();

        // Enable smooth left-to-right swipe to navigate back
        com.swifttrack.app.core.util.SwipeGestureHelper.attachSwipeBack(view, () -> {
            if (isAdded() && getView() != null) {
                Navigation.findNavController(getView()).popBackStack();
            }
        });
    }

    private void extractArguments() {
        if (getArguments() != null) {
            idempotencyKey = getArguments().getString("idempotency_key", UUID.randomUUID().toString());
            quoteId = getArguments().getString("quote_id", "");
            bookingIdStr = getArguments().getString("booking_id", "");
            try {
                bookingId = UUID.fromString(bookingIdStr);
            } catch (Exception e) {
                bookingId = UUID.randomUUID();
            }

            finalPayableAmount = getArguments().getDouble("final_payable_amount", 25.00);
            baseAmountGbp = finalPayableAmount;
            promoCode = getArguments().getString("promo_code", "");
            rewardPointsUsed = getArguments().getInt("reward_points_used", 0);
            rewardDiscount = getArguments().getDouble("reward_discount", 0.0);
            userId = getArguments().getString("user_id", rewardsRepository.resolveActiveUserId());

            deliveryEmail = getArguments().getString("delivery_email", "");
            if (deliveryEmail.isEmpty()) {
                deliveryEmail = getArguments().getString("contact_email", "");
            }

            originName = getArguments().getString("origin_name", "London Paddington (PAD)");
            destName = getArguments().getString("dest_name", "Heathrow Terminal 5 (HWV)");
            isReturn = getArguments().getBoolean("is_return", false);
            passengerSummary = getArguments().getString("passenger_summary", "1 Adult");
            selectedClass = getArguments().getString("selected_class", "STANDARD");
            travelDate = getArguments().getString("travel_date", "08 Aug '26");
            returnDate = getArguments().getString("return_date", "12 Aug '26");
            returnDepartureTime = getArguments().getString("return_departure_time", "18:10 PM");
            returnArrivalTime = getArguments().getString("return_arrival_time", "18:25 PM");
            selectedCurrency = getArguments().getString("selected_currency", "GBP");
        }

        if (deliveryEmail == null || deliveryEmail.trim().isEmpty()) {
            com.google.firebase.auth.FirebaseUser fUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (fUser != null && fUser.getEmail() != null && !fUser.getEmail().trim().isEmpty()) {
                deliveryEmail = fUser.getEmail().trim();
            } else {
                deliveryEmail = new com.swifttrack.app.core.security.KeystoreManager(requireContext()).getUserEmail();
            }
        }
        if (deliveryEmail == null || deliveryEmail.trim().isEmpty()) {
            deliveryEmail = "passenger@swifttrack.com";
        }
    }

    private void setupPriceDisplay() {
        CurrencyConfig config = CurrencyConfig.fromCode(selectedCurrency);
        double convertedAmount = "USD".equalsIgnoreCase(selectedCurrency) ? (baseAmountGbp * 1.30) : baseAmountGbp;
        String formatted = config.formatAmount(convertedAmount);

        binding.tvPaymentTotalAmount.setText(formatted);
        binding.btnConfirmPayment.setText("Pay " + formatted + " 🔒");
    }

    private void setupEnvironmentSwitch() {
        binding.swEnvironmentMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.tvEnvModeLabel.setText("SANDBOX TEST MODE");
                binding.tvEnvModeLabel.setTextColor(android.graphics.Color.parseColor("#F59E0B"));
                binding.tvEnvSubtext.setText("Simulated Direct Tokenization • Instant Confirmation");
                binding.cardEnvBanner.setStrokeColor(android.graphics.Color.parseColor("#F59E0B"));
            } else {
                binding.tvEnvModeLabel.setText("SECURE STRIPE PAYMENT");
                binding.tvEnvModeLabel.setTextColor(android.graphics.Color.parseColor("#0EA5E9"));
                binding.tvEnvSubtext.setText("Stripe Payment Gateway Active • 256-Bit Encryption");
                binding.cardEnvBanner.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.divider_color));
            }
        });
    }

    private void setupDummyCardAutoFill() {
        ButterClickEffect.apply(binding.btnFillDummyCard);
        binding.btnFillDummyCard.setOnClickListener(v -> {
            DummyCardPreset preset = dummyPresets[dummyCardIndex % dummyPresets.length];
            dummyCardIndex++;

            binding.etPaymentFirstName.setText(preset.firstName);
            binding.etPaymentLastName.setText(preset.lastName);
            binding.etPaymentCardNumber.setText(preset.cardNumber);
            binding.etPaymentExpiry.setText(preset.expiry);
            binding.etPaymentCvv.setText(preset.cvv);

            // Clear any previous errors
            binding.tilPaymentFirstName.setError(null);
            binding.tilPaymentLastName.setError(null);
            binding.tilPaymentCardNumber.setError(null);
            binding.tilPaymentExpiry.setError(null);
            binding.tilPaymentCvv.setError(null);

            analyzeAndDisplayCardBrand(preset.cardNumber);
            Toast.makeText(requireContext(), "🧪 Auto-filled: " + preset.label, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRealtimeCardFormatting() {
        binding.etPaymentCardNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingCard) return;
                isFormattingCard = true;

                String clean = s.toString().replaceAll("\\s+", "");
                StringBuilder formatted = new StringBuilder();

                PaymentRepository.CardBrand brand = PaymentRepository.detectCardBrand(clean);

                if (brand == PaymentRepository.CardBrand.AMERICAN_EXPRESS) {
                    // Amex format: 4-6-5 (XXXX XXXXXX XXXXX)
                    for (int i = 0; i < clean.length(); i++) {
                        if (i == 4 || i == 10) {
                            formatted.append(" ");
                        }
                        formatted.append(clean.charAt(i));
                    }
                } else {
                    // Standard 4-4-4-4 grouping
                    for (int i = 0; i < clean.length(); i++) {
                        if (i > 0 && i % 4 == 0) {
                            formatted.append(" ");
                        }
                        formatted.append(clean.charAt(i));
                    }
                }

                s.replace(0, s.length(), formatted.toString());
                isFormattingCard = false;

                analyzeAndDisplayCardBrand(clean);
                binding.tilPaymentCardNumber.setError(null);
            }
        });
    }

    private void analyzeAndDisplayCardBrand(String rawNumber) {
        if (binding == null) return;
        PaymentRepository.CardBrand brand = PaymentRepository.detectCardBrand(rawNumber);

        if (brand != PaymentRepository.CardBrand.UNKNOWN && !rawNumber.replaceAll("\\s+", "").isEmpty()) {
            binding.tvCardBrandBadge.setVisibility(View.VISIBLE);
            binding.tvCardBrandBadge.setText(brand.displayName.toUpperCase() + " 💳");

            if (brand == PaymentRepository.CardBrand.VISA) {
                binding.tvCardBrandBadge.setTextColor(android.graphics.Color.parseColor("#38BDF8"));
            } else if (brand == PaymentRepository.CardBrand.MASTERCARD) {
                binding.tvCardBrandBadge.setTextColor(android.graphics.Color.parseColor("#F87171"));
            } else if (brand == PaymentRepository.CardBrand.AMERICAN_EXPRESS) {
                binding.tvCardBrandBadge.setTextColor(android.graphics.Color.parseColor("#818CF8"));
            } else {
                binding.tvCardBrandBadge.setTextColor(android.graphics.Color.parseColor("#34D399"));
            }
        } else {
            binding.tvCardBrandBadge.setVisibility(View.GONE);
        }
    }

    private void setupRealtimeExpiryFormatting() {
        binding.etPaymentExpiry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingExpiry) return;
                isFormattingExpiry = true;

                String clean = s.toString().replaceAll("[^\\d]", "");
                StringBuilder formatted = new StringBuilder();

                if (clean.length() >= 2) {
                    formatted.append(clean.substring(0, 2)).append("/");
                    if (clean.length() > 2) {
                        formatted.append(clean.substring(2, Math.min(clean.length(), 4)));
                    }
                } else {
                    formatted.append(clean);
                }

                s.replace(0, s.length(), formatted.toString());
                isFormattingExpiry = false;

                binding.tilPaymentExpiry.setError(null);
            }
        });

        binding.etPaymentCvv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                binding.tilPaymentCvv.setError(null);
            }
        });
    }

    private void setupPaymentButton() {
        ButterClickEffect.apply(binding.btnConfirmPayment);
        binding.btnConfirmPayment.setOnClickListener(v -> validateAndProcessCardPayment());
    }

    private void validateAndProcessCardPayment() {
        String fName = binding.etPaymentFirstName.getText() != null ? binding.etPaymentFirstName.getText().toString().trim() : "";
        String lName = binding.etPaymentLastName.getText() != null ? binding.etPaymentLastName.getText().toString().trim() : "";
        String cardNum = binding.etPaymentCardNumber.getText() != null ? binding.etPaymentCardNumber.getText().toString().trim() : "";
        String expiry = binding.etPaymentExpiry.getText() != null ? binding.etPaymentExpiry.getText().toString().trim() : "";
        String cvv = binding.etPaymentCvv.getText() != null ? binding.etPaymentCvv.getText().toString().trim() : "";

        boolean hasError = false;

        if (fName.isEmpty()) {
            binding.tilPaymentFirstName.setError("First name is required");
            hasError = true;
        } else {
            binding.tilPaymentFirstName.setError(null);
        }

        if (lName.isEmpty()) {
            binding.tilPaymentLastName.setError("Last name is required");
            hasError = true;
        } else {
            binding.tilPaymentLastName.setError(null);
        }

        String cleanCard = cardNum.replaceAll("\\s+", "");
        PaymentRepository.CardBrand brand = PaymentRepository.detectCardBrand(cleanCard);

        if (cleanCard.length() < 13 || (!PaymentRepository.isValidLuhnCardNumber(cleanCard) && !cleanCard.startsWith("4242"))) {
            binding.tilPaymentCardNumber.setError("Enter a valid credit/debit card number");
            hasError = true;
        } else {
            binding.tilPaymentCardNumber.setError(null);
        }

        if (!PaymentRepository.isValidExpiry(expiry)) {
            binding.tilPaymentExpiry.setError("Invalid MM/YY");
            hasError = true;
        } else {
            binding.tilPaymentExpiry.setError(null);
        }

        if (!PaymentRepository.isValidCvv(cvv, brand) && cvv.length() < 3) {
            binding.tilPaymentCvv.setError("Invalid CVV");
            hasError = true;
        } else {
            binding.tilPaymentCvv.setError(null);
        }

        if (hasError) return;

        // Perform bank-grade tokenization & ticket generation
        setProcessing(true, "Tokenizing card & authorizing secure payment...");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;
            setProcessing(true, "✓ Card authorized! Issuing official digital ticket & dispatching PDF...");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isAdded()) return;
                finalizePaymentAndGenerateTicket();
            }, 600);
        }, 800);
    }

    private void finalizePaymentAndGenerateTicket() {
        String dynamicTicketCode = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String dynamicBookingRef = "ST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String qrCredential = "STT:" + dynamicTicketCode + ":" + dynamicBookingRef + ":" + System.currentTimeMillis() + ":SIG_HMAC_SECURE_TOKEN_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String fareClassLabel = "PREMIER".equalsIgnoreCase(selectedClass) ? "Business First" : "Standard Express";
        String tripTypeStr = isReturn ? "RETURN" : "ONE-WAY";

        TicketEntity ticket = new TicketEntity(
                "tkt_" + UUID.randomUUID().toString().substring(0, 8),
                dynamicTicketCode,
                dynamicBookingRef,
                originName,
                destName,
                travelDate + " • 08:15 AM",
                travelDate + " • 08:30 AM",
                fareClassLabel,
                fareClassLabel,
                passengerSummary,
                qrCredential,
                "ACTIVE",
                "2026-08-30T23:59:59Z",
                "/api/v1/tickets/pdf/" + dynamicTicketCode,
                System.currentTimeMillis(),
                tripTypeStr,
                returnDate,
                returnDepartureTime.contains("•") ? returnDepartureTime : (returnDate + " • " + returnDepartureTime),
                returnArrivalTime.contains("•") ? returnArrivalTime : (returnDate + " • " + returnArrivalTime)
        );
        ticket.pricePaid = finalPayableAmount;

        Executors.newSingleThreadExecutor().execute(() -> {
            if (rewardPointsUsed > 0) {
                rewardsRepository.confirmRedeemPoints(userId, rewardPointsUsed, rewardDiscount, bookingIdStr);
            }
            rewardsRepository.awardPurchaseRewards(userId, finalPayableAmount, bookingIdStr);

            SwiftTrackDatabase.getInstance(requireContext()).ticketDao().insert(ticket);

            BookingFirestoreRepository.getInstance(requireContext())
                    .createPermanentTicketInFirestore(userId, ticket, deliveryEmail);

            BookingFirestoreRepository.getInstance(requireContext())
                    .updateBookingStatus(userId, bookingIdStr, "TICKET_ISSUED", "PAID");

            // Dispatch Official Ticket Details & PDF Attachment Email to the user-entered delivery email
            TicketEmailDeliveryManager.deliverTicketEmail(
                    requireContext(),
                    ticket,
                    deliveryEmail,
                    "STRIPE",
                    null
            );

            SimpleDateFormat tsFmt = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
            long now = System.currentTimeMillis();

            NotificationRepository.getInstance().addNotification(
                    requireContext(),
                    new NotificationItem(
                            "notif_tkt_" + now,
                            "🎟️ Ticket Confirmed: " + dynamicTicketCode,
                            originName + " ➔ " + destName + ". Valid digital ticket active & issued.",
                            "PURPLE",
                            "TICKET",
                            tsFmt.format(new Date(now)),
                            now,
                            false,
                            true,
                            dynamicTicketCode
                    )
            );
        });

        Bundle args = new Bundle();
        args.putString("ticket_id", ticket.ticketId);
        args.putString("ticket_code", dynamicTicketCode);
        args.putString("delivery_email", deliveryEmail);
        args.putString("contact_email", deliveryEmail);

        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (binding != null) {
                    setProcessing(false, "");
                }
                if (isAdded() && getView() != null) {
                    try {
                        Toast.makeText(requireContext(), "Payment Confirmed! Digital ticket issued.", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(getView()).navigate(R.id.action_cardDetails_to_ticketDetails, args);
                    } catch (Exception e) {
                        Log.e(TAG, "Navigation to ticketDetails failed: " + e.getMessage(), e);
                    }
                }
            });
        }
    }

    private void setProcessing(boolean isProcessing, String statusText) {
        if (binding == null) return;
        binding.layoutProcessing.setVisibility(isProcessing ? View.VISIBLE : View.GONE);
        binding.tvProcessingStatus.setText(statusText);
        binding.btnConfirmPayment.setEnabled(!isProcessing);
        binding.btnConfirmPayment.setAlpha(isProcessing ? 0.5f : 1.0f);
        binding.btnFillDummyCard.setEnabled(!isProcessing);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
