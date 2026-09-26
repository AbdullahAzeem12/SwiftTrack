package com.swifttrack.app.presentation.fragments;

import android.animation.ValueAnimator;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.swifttrack.app.R;
import com.swifttrack.app.data.repository.LiveTransitRepository;
import com.swifttrack.app.databinding.FragmentHomeBookBinding;
import com.swifttrack.app.presentation.viewmodels.HomeBookViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeBookFragment extends Fragment {

    private FragmentHomeBookBinding binding;
    private HomeBookViewModel viewModel;

    // Lightweight 1-second presentation ticker for smooth seconds display
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // Single continuous breathing pulse animator for live status dot
    private ValueAnimator liveDotPulseAnimator;

    // Authoritative shared live departure models observed from repository
    private LiveTransitRepository.LiveDepartureInfo latestOutboundInfo;
    private LiveTransitRepository.LiveDepartureInfo latestReturnInfo;

    private boolean isStandardExpanded = false;
    private boolean isPremierExpanded = false;
    private boolean isReturnTrip = false;
    private final Calendar selectedCalendar = Calendar.getInstance();
    private final Calendar returnCalendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBookBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeBookViewModel.class);

        startLiveDotBreathingAnimation();
        setupLightweightDisplayTicker();
        setupGlassStationSwap(view);
        setupAdjustTicketSection();
        setupExpandableTicketCards();
        setupActionButtons(view);
        setupThresholdScrollAnimations();
        observeViewModel();
    }

    /**
     * Dual-Layer Realtime Pulsing & Glowing Breathing Halo Animation:
     * Synchronously pulses the core indicator dot and expands the translucent glowing halo shadow
     * (scale 1.00 <-> 1.55, alpha 0.65 <-> 0.20) for a state-of-the-art live breathing radar effect.
     */
    private void startLiveDotBreathingAnimation() {
        if (binding == null || binding.imgLiveDot == null || binding.imgLiveDotHalo == null) return;
        if (liveDotPulseAnimator != null && liveDotPulseAnimator.isRunning()) return;

        liveDotPulseAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        liveDotPulseAnimator.setDuration(1600);
        liveDotPulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        liveDotPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        liveDotPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        liveDotPulseAnimator.addUpdateListener(animation -> {
            if (binding == null || binding.imgLiveDot == null || binding.imgLiveDotHalo == null) return;
            float fraction = (float) animation.getAnimatedValue();

            // Core Dot subtle breath
            float coreScale = 0.95f + (0.15f * fraction);
            binding.imgLiveDot.setScaleX(coreScale);
            binding.imgLiveDot.setScaleY(coreScale);

            // Outer Glowing Halo breathing expansion & alpha bloom
            float haloScale = 1.0f + (0.55f * fraction);
            float haloAlpha = 0.65f - (0.45f * fraction);
            binding.imgLiveDotHalo.setScaleX(haloScale);
            binding.imgLiveDotHalo.setScaleY(haloScale);
            binding.imgLiveDotHalo.setAlpha(haloAlpha);
        });
        liveDotPulseAnimator.start();
    }

    /**
     * Lightweight Local Display Ticker:
     * Only calculates smooth seconds countdown from authoritative departure epoch timestamps.
     * Never invents or modifies railway data.
     */
    private void setupLightweightDisplayTicker() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding == null) return;

                long nowMs = System.currentTimeMillis();

                // Digital Clock Display
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
                binding.tvClockTime.setText(sdf.format(new Date(nowMs)));

                // Outbound Live Countdown Calculation
                if (latestOutboundInfo != null && latestOutboundInfo.departureEpochMs > 0) {
                    long remSec = Math.max(0, (latestOutboundInfo.departureEpochMs - nowMs) / 1000L);
                    if (remSec <= 0) {
                        binding.tvLiveCountdown.setText("Departing now");
                        if (viewModel != null) {
                            viewModel.reconcileDepartures();
                        }
                    } else {
                        long min = remSec / 60;
                        long sec = remSec % 60;
                        binding.tvLiveCountdown.setText(String.format(Locale.getDefault(), "Next in %d min %02d sec", min, sec));
                    }
                }

                // Return Live Countdown Calculation
                if (latestReturnInfo != null && latestReturnInfo.departureEpochMs > 0) {
                    long remSec = Math.max(0, (latestReturnInfo.departureEpochMs - nowMs) / 1000L);
                    if (remSec <= 0) {
                        binding.tvLiveCountdownReturn.setText("Departing now");
                        if (viewModel != null) {
                            viewModel.reconcileDepartures();
                        }
                    } else {
                        long min = remSec / 60;
                        long sec = remSec % 60;
                        binding.tvLiveCountdownReturn.setText(String.format(Locale.getDefault(), "Next in %d min %02d sec", min, sec));
                    }
                }

                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    /**
     * Glass Circular Swap Button ("From ↔ To") Aligned to Right Boundary
     * Scale Down -> Rotate 180° -> Swap -> Scale Up -> Glow -> Haptic Feedback
     */
    private void setupGlassStationSwap(@NonNull View root) {
        binding.fabSwap.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

            v.animate()
                    .scaleX(0.85f)
                    .scaleY(0.85f)
                    .rotationBy(180f)
                    .setDuration(220)
                    .withEndAction(() -> {
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                        if (viewModel != null) {
                            viewModel.swapStations();
                        }
                        updateRouteSpecs();
                    })
                    .start();
        });

        binding.boxFrom.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("search_target", "FROM");
            try {
                NavHostFragment.findNavController(HomeBookFragment.this)
                        .navigate(R.id.action_home_to_stationSearch, args);
            } catch (Exception e) {
                try {
                    Navigation.findNavController(v).navigate(R.id.action_home_to_stationSearch, args);
                } catch (Exception ignored) {}
            }
        });
        binding.boxTo.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("search_target", "TO");
            try {
                NavHostFragment.findNavController(HomeBookFragment.this)
                        .navigate(R.id.action_home_to_stationSearch, args);
            } catch (Exception e) {
                try {
                    Navigation.findNavController(v).navigate(R.id.action_home_to_stationSearch, args);
                } catch (Exception ignored) {}
            }
        });
    }

    /**
     * "Adjust your ticket" Section Listeners (How, When, Who, Return Date & Time)
     */
    private void setupAdjustTicketSection() {
        returnCalendar.setTime(selectedCalendar.getTime());
        returnCalendar.add(Calendar.DAY_OF_MONTH, 1);

        // How: Toggle Single / Return
        binding.rowAdjustHow.setOnClickListener(v -> {
            isReturnTrip = !isReturnTrip;
            viewModel.setReturnJourney(isReturnTrip);
            binding.tvAdjustHowValue.setText(isReturnTrip ? "Return" : "Single");
            String ticketTypeStr = isReturnTrip ? "TICKET TYPE: Return" : "TICKET TYPE: Single";
            binding.tvStandardTicketLabel.setText(ticketTypeStr);
            binding.tvPremierTicketLabel.setText(ticketTypeStr);

            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());
            if (isReturnTrip) {
                binding.rowAdjustReturnWhen.setVisibility(View.VISIBLE);
                binding.rowAdjustReturnTime.setVisibility(View.VISIBLE);

                // Prompt user to pick/verify their custom return date
                openReturnDatePicker();
            } else {
                binding.rowAdjustReturnWhen.setVisibility(View.GONE);
                binding.rowAdjustReturnTime.setVisibility(View.GONE);
            }
        });

        // When: Outbound Date Picker Dialog
        binding.rowAdjustWhen.setOnClickListener(v -> {
            int year = selectedCalendar.get(Calendar.YEAR);
            int month = selectedCalendar.get(Calendar.MONTH);
            int day = selectedCalendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, pickedYear, pickedMonth, pickedDay) -> {
                        selectedCalendar.set(pickedYear, pickedMonth, pickedDay);
                        SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM ''yy", Locale.getDefault());
                        String formattedDate = dateFmt.format(selectedCalendar.getTime());

                        String fullDateStr = formattedDate + " (Selected)";
                        binding.tvAdjustWhenValue.setText(fullDateStr);
                        viewModel.setTravelDate(fullDateStr);

                        // If return date is earlier or same, advance it to next day
                        if (returnCalendar.before(selectedCalendar) || returnCalendar.equals(selectedCalendar)) {
                            returnCalendar.setTime(selectedCalendar.getTime());
                            returnCalendar.add(Calendar.DAY_OF_MONTH, 1);
                            String autoReturnDate = dateFmt.format(returnCalendar.getTime()) + " (Tomorrow)";
                            binding.tvAdjustReturnWhenValue.setText(autoReturnDate);
                            viewModel.setReturnDate(autoReturnDate);
                        }
                    },
                    year, month, day
            );
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        // Return When: Return Journey Date Picker Dialog
        binding.rowAdjustReturnWhen.setOnClickListener(v -> openReturnDatePicker());

        // Return Time: Return Journey Preferred Time Selector Dialog
        binding.rowAdjustReturnTime.setOnClickListener(v -> openReturnTimeSelector());

        // Who: Interactive Passenger Counter Bottom Sheet (Adults, Children, Infants)
        binding.rowAdjustWho.setOnClickListener(v -> showPassengerSelectorBottomSheet());
    }

    private void openReturnDatePicker() {
        int year = returnCalendar.get(Calendar.YEAR);
        int month = returnCalendar.get(Calendar.MONTH);
        int day = returnCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog returnDatePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, pickedYear, pickedMonth, pickedDay) -> {
                    returnCalendar.set(pickedYear, pickedMonth, pickedDay);
                    SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM ''yy", Locale.getDefault());
                    String formattedDate = dateFmt.format(returnCalendar.getTime());

                    // Check if it's strictly next day from outbound
                    Calendar checkTomorrow = (Calendar) selectedCalendar.clone();
                    checkTomorrow.add(Calendar.DAY_OF_MONTH, 1);
                    boolean isTomorrow = (checkTomorrow.get(Calendar.YEAR) == pickedYear
                            && checkTomorrow.get(Calendar.MONTH) == pickedMonth
                            && checkTomorrow.get(Calendar.DAY_OF_MONTH) == pickedDay);

                    String fullReturnDateStr = formattedDate + (isTomorrow ? " (Tomorrow)" : " (Selected)");
                    binding.tvAdjustReturnWhenValue.setText(fullReturnDateStr);
                    viewModel.setReturnDate(fullReturnDateStr);
                },
                year, month, day
        );
        // Minimum selectable return date is outbound travel date
        returnDatePickerDialog.getDatePicker().setMinDate(selectedCalendar.getTimeInMillis());
        returnDatePickerDialog.show();
    }

    private void openReturnTimeSelector() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    String amPm = hourOfDay >= 12 ? "PM" : "AM";
                    int hour12 = hourOfDay % 12;
                    if (hour12 == 0) hour12 = 12;
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm);

                    int arrMinute = (minute + 15) % 60;
                    int arrHour = (hourOfDay + (minute + 15) / 60) % 24;
                    String arrAmPm = arrHour >= 12 ? "PM" : "AM";
                    int arrHour12 = arrHour % 12;
                    if (arrHour12 == 0) arrHour12 = 12;
                    String formattedArrTime = String.format(Locale.getDefault(), "%02d:%02d %s", arrHour12, arrMinute, arrAmPm);

                    binding.tvAdjustReturnTimeValue.setText(formattedTime + " (Selected)");
                    viewModel.setReturnTime(formattedTime, formattedArrTime);
                },
                18, 10, false
        );
        timePickerDialog.show();
    }

    /**
     * Interactive Passenger Bottom Sheet Dialog (Adults, Children, Infants)
     */
    private void showPassengerSelectorBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_passengers, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvAdults = sheetView.findViewById(R.id.tv_count_adults);
        TextView tvChildren = sheetView.findViewById(R.id.tv_count_children);
        TextView tvInfants = sheetView.findViewById(R.id.tv_count_infants);

        MaterialButton btnMinusAdults = sheetView.findViewById(R.id.btn_minus_adults);
        MaterialButton btnPlusAdults = sheetView.findViewById(R.id.btn_plus_adults);
        MaterialButton btnMinusChildren = sheetView.findViewById(R.id.btn_minus_children);
        MaterialButton btnPlusChildren = sheetView.findViewById(R.id.btn_plus_children);
        MaterialButton btnMinusInfants = sheetView.findViewById(R.id.btn_minus_infants);
        MaterialButton btnPlusInfants = sheetView.findViewById(R.id.btn_plus_infants);
        MaterialButton btnApply = sheetView.findViewById(R.id.btn_apply_passengers);

        final int[] counts = new int[]{
                viewModel.getAdultCount().getValue() != null ? viewModel.getAdultCount().getValue() : 1,
                viewModel.getChildCount().getValue() != null ? viewModel.getChildCount().getValue() : 0,
                viewModel.getInfantCount().getValue() != null ? viewModel.getInfantCount().getValue() : 0
        };

        tvAdults.setText(String.valueOf(counts[0]));
        tvChildren.setText(String.valueOf(counts[1]));
        tvInfants.setText(String.valueOf(counts[2]));

        btnMinusAdults.setOnClickListener(v -> {
            if (counts[0] > 1) {
                counts[0]--;
                tvAdults.setText(String.valueOf(counts[0]));
            }
        });
        btnPlusAdults.setOnClickListener(v -> {
            if (counts[0] < 9) {
                counts[0]++;
                tvAdults.setText(String.valueOf(counts[0]));
            }
        });

        btnMinusChildren.setOnClickListener(v -> {
            if (counts[1] > 0) {
                counts[1]--;
                tvChildren.setText(String.valueOf(counts[1]));
            }
        });
        btnPlusChildren.setOnClickListener(v -> {
            if (counts[1] < 9) {
                counts[1]++;
                tvChildren.setText(String.valueOf(counts[1]));
            }
        });

        btnMinusInfants.setOnClickListener(v -> {
            if (counts[2] > 0) {
                counts[2]--;
                tvInfants.setText(String.valueOf(counts[2]));
            }
        });
        btnPlusInfants.setOnClickListener(v -> {
            if (counts[2] < 9) {
                counts[2]++;
                tvInfants.setText(String.valueOf(counts[2]));
            }
        });

        btnApply.setOnClickListener(v -> {
            viewModel.setPassengers(counts[0], counts[1], counts[2]);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    /**
     * Smooth Sliding / Expandable Details for Standard and Premier+ Cards
     */
    private void setupExpandableTicketCards() {
        // Standard Class Expand Toggle
        binding.btnToggleDetailsStandard.setOnClickListener(v -> {
            isStandardExpanded = !isStandardExpanded;
            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());

            if (isStandardExpanded) {
                binding.layoutDetailsStandard.setVisibility(View.VISIBLE);
                binding.tvToggleStandardText.setText("Hide full ticket details");
                binding.imgChevronStandard.setImageResource(R.drawable.ic_chevron_up);
            } else {
                binding.layoutDetailsStandard.setVisibility(View.GONE);
                binding.tvToggleStandardText.setText("Show full ticket details");
                binding.imgChevronStandard.setImageResource(R.drawable.ic_chevron_down);
            }
        });

        // Premier+ Class Expand Toggle
        binding.btnToggleDetailsPremier.setOnClickListener(v -> {
            isPremierExpanded = !isPremierExpanded;
            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), new AutoTransition());

            if (isPremierExpanded) {
                binding.layoutDetailsPremier.setVisibility(View.VISIBLE);
                binding.tvTogglePremierText.setText("Hide full ticket details");
                binding.imgChevronPremier.setImageResource(R.drawable.ic_chevron_up);
            } else {
                binding.layoutDetailsPremier.setVisibility(View.GONE);
                binding.tvTogglePremierText.setText("Show full ticket details");
                binding.imgChevronPremier.setImageResource(R.drawable.ic_chevron_down);
            }
        });

        // Card Selection Pop-in Click Animations & Direct Checkout Navigation
        binding.cardStandardOption.setOnClickListener(v -> {
            playPopInAnimation(binding.cardStandardOption);
            navigateToCheckoutDirectly(v, "STANDARD");
        });

        binding.cardPremierOption.setOnClickListener(v -> {
            playPopInAnimation(binding.cardPremierOption);
            navigateToCheckoutDirectly(v, "PREMIER");
        });
    }

    /**
     * Threshold-based Scroll Animation Engine
     */
    private void setupThresholdScrollAnimations() {
        final View[] cardViews = new View[]{
                binding.cardLiveStatus,
                binding.cardBookingForm,
                binding.cardAdjustTicketSection,
                binding.cardStandardOption,
                binding.cardPremierOption,
                binding.btnSearchTrains
        };

        binding.scrollViewHome.post(() -> {
            if (binding == null) return;
            for (View view : cardViews) {
                applyEffect(view);
            }
        });

        binding.scrollViewHome.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            for (View view : cardViews) {
                applyEffect(view);
            }
        });
    }

    private void applyEffect(View view) {
        if (view == null || getResources() == null) return;

        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewTop = location[1];
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        float alpha = 1.0f;
        float scale = 1.0f;

        int topThreshold = 250;
        int bottomThreshold = screenHeight - 450;

        if (viewTop < topThreshold) {
            float ratio = Math.max(0.1f, (float) viewTop / topThreshold);
            alpha = ratio;
            scale = 0.9f + (ratio * 0.1f);
        } else if (viewTop > bottomThreshold) {
            float ratio = Math.max(0f, (float) (screenHeight - viewTop) / 450f);
            alpha = ratio;
            scale = 0.9f + (ratio * 0.1f);
        }

        view.setAlpha(Math.max(0.0f, Math.min(1.0f, alpha)));
        view.setScaleX(Math.max(0.9f, Math.min(1.0f, scale)));
        view.setScaleY(Math.max(0.9f, Math.min(1.0f, scale)));
    }

    private void playPopInAnimation(View targetCard) {
        targetCard.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        Animation popAnim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_pop_in);
        targetCard.startAnimation(popAnim);
    }

    private void setupActionButtons(@NonNull View root) {
        binding.btnSelectStandard.setOnClickListener(v -> {
            navigateToCheckoutDirectly(v, "STANDARD");
        });

        binding.btnSelectPremier.setOnClickListener(v -> {
            navigateToCheckoutDirectly(v, "PREMIER");
        });

        binding.btnSearchTrains.setOnClickListener(v -> {
            applyPromoAndNavigate(v);
        });
    }

    private void navigateToCheckoutDirectly(View v, String travelClass) {
        if (viewModel != null) {
            viewModel.onTicketOptionSelected(travelClass);
        }
        try {
            NavHostFragment.findNavController(HomeBookFragment.this)
                    .navigate(R.id.action_home_to_checkout);
        } catch (Exception e) {
            try {
                if (v != null) {
                    Navigation.findNavController(v).navigate(R.id.action_home_to_checkout);
                }
            } catch (Exception ignored) {}
        }
    }

    private void applyPromoAndNavigate(@NonNull View v) {
        try {
            NavHostFragment.findNavController(HomeBookFragment.this)
                    .navigate(R.id.action_home_to_searchResults);
        } catch (Exception e) {
            try {
                Navigation.findNavController(v).navigate(R.id.action_home_to_searchResults);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Shared Reactive Live Rail Data Observers:
     * Consumes the exact same TransportAPI/Darwin stream used by TimetableFragment.
     */
    private void observeViewModel() {
        // Station Selection Observers
        viewModel.getOriginStation().observe(getViewLifecycleOwner(), station -> {
            if (station != null && binding != null) {
                binding.tvFromStation.setText(station.name + " (" + station.code + ")");
                updateRouteSpecs();
            }
        });

        viewModel.getDestinationStation().observe(getViewLifecycleOwner(), station -> {
            if (station != null && binding != null) {
                binding.tvToStation.setText(station.name + " (" + station.code + ")");
                updateRouteSpecs();
            }
        });

        viewModel.getTravelDate().observe(getViewLifecycleOwner(), dateStr -> {
            if (dateStr != null && !dateStr.isEmpty() && binding != null) {
                binding.tvAdjustWhenValue.setText(dateStr);
            }
        });

        viewModel.getIsReturnJourney().observe(getViewLifecycleOwner(), isRet -> {
            if (binding == null) return;
            isReturnTrip = Boolean.TRUE.equals(isRet);
            binding.tvAdjustHowValue.setText(isReturnTrip ? "Return" : "Single");
            String ticketTypeStr = isReturnTrip ? "TICKET TYPE: Return" : "TICKET TYPE: Single";
            binding.tvStandardTicketLabel.setText(ticketTypeStr);
            binding.tvPremierTicketLabel.setText(ticketTypeStr);

            binding.rowAdjustReturnWhen.setVisibility(isReturnTrip ? View.VISIBLE : View.GONE);
            binding.rowAdjustReturnTime.setVisibility(isReturnTrip ? View.VISIBLE : View.GONE);
        });

        viewModel.getReturnDate().observe(getViewLifecycleOwner(), retDateStr -> {
            if (retDateStr != null && !retDateStr.isEmpty() && binding != null) {
                binding.tvAdjustReturnWhenValue.setText(retDateStr);
            }
        });

        viewModel.getReturnDepartureTime().observe(getViewLifecycleOwner(), retTimeStr -> {
            if (retTimeStr != null && !retTimeStr.isEmpty() && binding != null) {
                binding.tvAdjustReturnTimeValue.setText(retTimeStr + " (Flexible)");
            }
        });

        viewModel.getPassengerSummary().observe(getViewLifecycleOwner(), summary -> {
            if (summary != null && binding != null) {
                binding.tvAdjustWhoValue.setText(summary);
                binding.tvStandardPassengerBreakdownLabel.setText(summary + " Base Fare");
                binding.tvPremierPassengerBreakdownLabel.setText(summary + " Base Fare");
            }
        });

        // 1. Shared Outbound Live Departure (Paddington -> Heathrow)
        viewModel.getOutboundLiveDeparture().observe(getViewLifecycleOwner(), outboundInfo -> {
            if (outboundInfo == null || binding == null) return;
            latestOutboundInfo = outboundInfo;

            binding.tvLabelOutboundDir.setText(outboundInfo.originStation + " ➔ " + outboundInfo.destStation);
            binding.tvPlatformInfo.setText(outboundInfo.platform != null && !outboundInfo.platform.isEmpty() ? outboundInfo.platform : "Plat —");
            binding.tvLiveCountdown.setText(outboundInfo.formattedCountdown);
            if (binding.tvOutboundStatusTag != null) {
                binding.tvOutboundStatusTag.setText(outboundInfo.isCancelled ? "Cancelled" : (outboundInfo.delayMinutes > 0 ? "Late +" + outboundInfo.delayMinutes + "m" : "Live • On Time"));
            }
        });

        // 2. Shared Return Live Departure (Heathrow -> Paddington)
        viewModel.getReturnLiveDeparture().observe(getViewLifecycleOwner(), returnInfo -> {
            if (returnInfo == null || binding == null) return;
            latestReturnInfo = returnInfo;

            binding.tvLabelReturnDir.setText(returnInfo.originStation + " ➔ " + returnInfo.destStation);
            binding.tvPlatformInfoReturn.setText(returnInfo.platform != null && !returnInfo.platform.isEmpty() ? returnInfo.platform : "Plat —");
            binding.tvLiveCountdownReturn.setText(returnInfo.formattedCountdown);
            if (binding.tvReturnStatusTag != null) {
                binding.tvReturnStatusTag.setText(returnInfo.isCancelled ? "Cancelled" : (returnInfo.delayMinutes > 0 ? "Late +" + returnInfo.delayMinutes + "m" : "Live • On Time"));
            }
        });

        // 3. Operational Status & Glowing Live Breathing Dot with Halo
        viewModel.getRailLineStatus().observe(getViewLifecycleOwner(), status -> {
            if (status == null || binding == null) return;

            binding.tvLiveStatusTitle.setText(status.label);

            boolean isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            int statusColor = Color.parseColor(isDark ? status.colorHexDark : status.colorHex);

            binding.tvLiveStatusTitle.setTextColor(statusColor);
            binding.imgLiveDot.setColorFilter(statusColor);
            if (binding.imgLiveDotHalo.getBackground() != null) {
                binding.imgLiveDotHalo.getBackground().setColorFilter(statusColor, android.graphics.PorterDuff.Mode.SRC_IN);
            }
        });

        // 4. Data-Derived Service Frequency Summary
        viewModel.getDynamicFrequencySummary().observe(getViewLifecycleOwner(), freqSummary -> {
            if (freqSummary != null && binding != null) {
                binding.tvFrequencySummary.setText(freqSummary);
            }
        });

        // Fare Engine Observers
        viewModel.getStandardDiscountedFare().observe(getViewLifecycleOwner(), discFare -> {
            if (discFare != null && binding != null) {
                Double baseFare = viewModel.getStandardBaseFare().getValue();
                String discStr = String.format(Locale.getDefault(), "£%.2f", discFare);
                binding.tvStandardPrice.setText(discStr);
                binding.tvStandardBreakdownTotal.setText(discStr);
                binding.btnSelectStandard.setText("Select Standard Class (" + discStr + ")");

                if (baseFare != null && baseFare > discFare) {
                    String baseStr = String.format(Locale.getDefault(), "£%.2f", baseFare);
                    binding.tvStandardOriginalPrice.setText(baseStr);
                    binding.tvStandardOriginalPrice.setPaintFlags(binding.tvStandardOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    binding.tvStandardOriginalPrice.setVisibility(View.VISIBLE);
                    binding.tvStandardBreakdownBase.setText(baseStr);

                    double savings = baseFare - discFare;
                    binding.tvStandardBreakdownSavings.setText(String.format(Locale.getDefault(), "-£%.2f", savings));
                } else {
                    binding.tvStandardOriginalPrice.setVisibility(View.GONE);
                    binding.tvStandardBreakdownBase.setText(discStr);
                    binding.tvStandardBreakdownSavings.setText("£0.00");
                }
            }
        });

        viewModel.getPremierDiscountedFare().observe(getViewLifecycleOwner(), discFare -> {
            if (discFare != null && binding != null) {
                Double baseFare = viewModel.getPremierBaseFare().getValue();
                String discStr = String.format(Locale.getDefault(), "£%.2f", discFare);
                binding.tvPremierPrice.setText(discStr);
                binding.tvPremierBreakdownTotal.setText(discStr);
                binding.btnSelectPremier.setText("Select PREMIER+ Class (" + discStr + ")");

                if (baseFare != null && baseFare > discFare) {
                    String baseStr = String.format(Locale.getDefault(), "£%.2f", baseFare);
                    binding.tvPremierOriginalPrice.setText(baseStr);
                    binding.tvPremierOriginalPrice.setPaintFlags(binding.tvPremierOriginalPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    binding.tvPremierOriginalPrice.setVisibility(View.VISIBLE);
                    binding.tvPremierBreakdownBase.setText(baseStr);

                    double savings = baseFare - discFare;
                    binding.tvPremierBreakdownSavings.setText(String.format(Locale.getDefault(), "-£%.2f", savings));
                } else {
                    binding.tvPremierOriginalPrice.setVisibility(View.GONE);
                    binding.tvPremierBreakdownBase.setText(discStr);
                    binding.tvPremierBreakdownSavings.setText("£0.00");
                }
            }
        });
    }

    private void updateRouteSpecs() {
        if (binding == null) return;
        String from = binding.tvFromStation.getText().toString();
        String to = binding.tvToStation.getText().toString();
        String routeText = from + " to " + to;

        binding.tvStandardRouteSpec.setText(routeText);
        binding.tvPremierRouteSpec.setText(routeText);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (liveDotPulseAnimator != null) {
            liveDotPulseAnimator.cancel();
            liveDotPulseAnimator = null;
        }
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        binding = null;
    }
}
