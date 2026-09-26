package com.swifttrack.app.presentation.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import com.swifttrack.app.R;
import com.swifttrack.app.data.local.StationEntity;
import com.swifttrack.app.data.repository.LiveTransitRepository;
import com.swifttrack.app.databinding.FragmentSearchResultsBinding;
import com.swifttrack.app.presentation.viewmodels.HomeBookViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchResultsFragment extends Fragment implements OnMapReadyCallback {

    private FragmentSearchResultsBinding binding;
    private HomeBookViewModel viewModel;
    private LiveTransitRepository transitRepository;
    private GoogleMap googleMap;
    private boolean isGmsAvailable = false;

    // Station Coordinates (London Paddington ↔ Heathrow Airport)
    private final LatLng originLatLng = new LatLng(51.4700, -0.4543); // Heathrow Airport
    private final LatLng destLatLng = new LatLng(51.5167, -0.1755);   // London Paddington

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchResultsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private final android.os.Handler timerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable timerRunnable;
    private int apiPollCounter = 0;

    private TrainAdapter outboundAdapter;
    private TrainAdapter returnAdapter;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeBookViewModel.class);
        transitRepository = new LiveTransitRepository(requireContext());

        outboundAdapter = new TrainAdapter(new ArrayList<>(), item -> navigateToCheckout(view));
        returnAdapter = new TrainAdapter(new ArrayList<>(), item -> navigateToCheckout(view));

        binding.rvTrains.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTrains.setAdapter(outboundAdapter);

        if (binding.rvReturnTrains != null) {
            binding.rvReturnTrains.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.rvReturnTrains.setAdapter(returnAdapter);
        }

        // Multi-Device Map Check (Google Play Services vs HMS / Fallback)
        checkMapSupportAndInitialize(savedInstanceState);

        observeViewModelAndPopulateResults(view);
        setupLiveRealtimeTimer(view);
        setupThresholdScrollAnimations();
    }

    private void navigateToCheckout(@NonNull View root) {
        try {
            androidx.navigation.fragment.NavHostFragment.findNavController(SearchResultsFragment.this)
                    .navigate(R.id.action_searchResults_to_checkout);
        } catch (Exception e) {
            try {
                Navigation.findNavController(root).navigate(R.id.action_searchResults_to_checkout);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Checks if Google Play Services is supported.
     * If not available (e.g. Huawei / HMS devices), gracefully displays Fallback Route Visualizer.
     */
    private void checkMapSupportAndInitialize(Bundle savedInstanceState) {
        try {
            int googlePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
            isGmsAvailable = (googlePlayServicesAvailable == ConnectionResult.SUCCESS);

            if (isGmsAvailable && binding.mapViewRoute != null) {
                binding.mapViewRoute.setVisibility(View.VISIBLE);
                binding.layoutFallbackRouteMap.setVisibility(View.GONE);
                binding.mapViewRoute.onCreate(savedInstanceState);
                binding.mapViewRoute.getMapAsync(this);
            } else {
                showFallbackRouteVisualizer();
            }
        } catch (Exception e) {
            showFallbackRouteVisualizer();
        }
    }

    private void showFallbackRouteVisualizer() {
        if (binding == null) return;
        if (binding.mapViewRoute != null) {
            binding.mapViewRoute.setVisibility(View.GONE);
        }
        if (binding.layoutFallbackRouteMap != null) {
            binding.layoutFallbackRouteMap.setVisibility(View.VISIBLE);
        }
    }

    private void setupLiveRealtimeTimer(@NonNull View root) {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (binding == null) return;

                updateLiveHeaderTimestamp();

                apiPollCounter++;
                if (apiPollCounter % 15 == 0) {
                    fetchLiveTransportApiData(root);
                }

                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void updateLiveHeaderTimestamp() {
        if (binding == null || viewModel == null) return;

        StationEntity origin = viewModel.getOriginStation().getValue();
        StationEntity dest = viewModel.getDestinationStation().getValue();

        String fromName = origin != null ? origin.name : "London Paddington";
        String toName = dest != null ? dest.name : "Heathrow Airport";

        String travelDate = viewModel.getTravelDate().getValue() != null ? viewModel.getTravelDate().getValue() : "Today";
        String passengerSummary = viewModel.getPassengerSummary().getValue() != null ? viewModel.getPassengerSummary().getValue() : "1 Adult";
        String selectedClass = viewModel.getSelectedClass().getValue() != null ? viewModel.getSelectedClass().getValue() : "STANDARD";
        boolean isReturn = Boolean.TRUE.equals(viewModel.getIsReturnJourney().getValue());

        String tripTypeLabel = isReturn ? "Return" : "Single";
        String classLabel = "PREMIER".equalsIgnoreCase(selectedClass) ? "Premier+ Class" : "Standard Class";

        SimpleDateFormat tsFmt = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
        String liveTimestamp = "Live API data • " + tsFmt.format(new Date());

        binding.tvRouteHeader.setText(fromName + " ➔ " + toName);
        binding.tvDateHeader.setText(travelDate + " • " + passengerSummary + " • " + classLabel + " (" + tripTypeLabel + ")\n" + liveTimestamp);
    }

    private void fetchLiveTransportApiData(@NonNull View root) {
        if (binding == null || viewModel == null) return;

        StationEntity origin = viewModel.getOriginStation().getValue();
        StationEntity dest = viewModel.getDestinationStation().getValue();

        String fromCode = origin != null ? origin.code : "PAD";
        String toCode = dest != null ? dest.code : "LHR";

        String selectedClass = viewModel.getSelectedClass().getValue() != null ? viewModel.getSelectedClass().getValue() : "STANDARD";
        boolean isReturn = Boolean.TRUE.equals(viewModel.getIsReturnJourney().getValue());

        Double calculatedFare = "PREMIER".equalsIgnoreCase(selectedClass) ?
                viewModel.getPremierDiscountedFare().getValue() :
                viewModel.getStandardDiscountedFare().getValue();

        if (calculatedFare == null) calculatedFare = "PREMIER".equalsIgnoreCase(selectedClass) ? 28.80 : 23.40;
        final String formattedPrice = String.format(Locale.getDefault(), "£%.2f", calculatedFare);

        // Outbound Transport API live departures call (Smooth dataset update without resetting adapter)
        transitRepository.fetchRealTimeSchedulesAsync(fromCode, toCode, new LiveTransitRepository.LiveApiCallback<List<LiveTransitRepository.RealTimeTrainSchedule>>() {
            @Override
            public void onSuccess(List<LiveTransitRepository.RealTimeTrainSchedule> result) {
                if (binding == null || result == null) return;

                List<TrainItem> outboundTrains = new ArrayList<>();
                for (LiveTransitRepository.RealTimeTrainSchedule item : result) {
                    outboundTrains.add(new TrainItem(item.depTime, item.arrTime, item.duration, item.platform, item.status, formattedPrice, item.seatsLeft, item.operatorName));
                }

                binding.tvResultsCount.setText(outboundTrains.size() + " Direct Departures");
                if (outboundAdapter != null) {
                    outboundAdapter.updateItems(outboundTrains);
                }
                applyAllScrollEffects();
            }

            @Override
            public void onError(Exception e) {}
        });

        // Return Transport API live departures call
        if (isReturn) {
            transitRepository.fetchRealTimeSchedulesAsync(toCode, fromCode, new LiveTransitRepository.LiveApiCallback<List<LiveTransitRepository.RealTimeTrainSchedule>>() {
                @Override
                public void onSuccess(List<LiveTransitRepository.RealTimeTrainSchedule> result) {
                    if (binding == null || result == null) return;

                    List<TrainItem> returnTrains = new ArrayList<>();
                    for (LiveTransitRepository.RealTimeTrainSchedule item : result) {
                        returnTrains.add(new TrainItem(item.depTime, item.arrTime, item.duration, item.platform, item.status, formattedPrice, item.seatsLeft, item.operatorName));
                    }

                    if (returnAdapter != null) {
                        returnAdapter.updateItems(returnTrains);
                    }
                    applyAllScrollEffects();
                }

                @Override
                public void onError(Exception e) {}
            });
        }
    }

    private void observeViewModelAndPopulateResults(@NonNull View root) {
        StationEntity origin = viewModel.getOriginStation().getValue();
        StationEntity dest = viewModel.getDestinationStation().getValue();

        String fromName = origin != null ? origin.name : "London Paddington";
        String toName = dest != null ? dest.name : "Heathrow Airport";

        String fromCode = origin != null ? origin.code : "PAD";
        String toCode = dest != null ? dest.code : "LHR";

        binding.tvFallbackOrigin.setText(fromCode);
        binding.tvFallbackDest.setText(toCode);

        boolean isReturn = Boolean.TRUE.equals(viewModel.getIsReturnJourney().getValue());
        if (isReturn) {
            binding.layoutReturnJourneySection.setVisibility(View.VISIBLE);
            binding.tvReturnTitle.setText("Return Departures (" + toName + " ➔ " + fromName + ")");
        } else {
            binding.layoutReturnJourneySection.setVisibility(View.GONE);
        }

        updateLiveHeaderTimestamp();
        fetchLiveTransportApiData(root);

        viewModel.getStandardDiscountedFare().observe(getViewLifecycleOwner(), fare -> {
            if (binding != null && fare != null) fetchLiveTransportApiData(root);
        });

        viewModel.getPremierDiscountedFare().observe(getViewLifecycleOwner(), fare -> {
            if (binding != null && fare != null) fetchLiveTransportApiData(root);
        });
    }

    /**
     * Threshold-based Scroll Animation Engine for Search Results:
     * Applies exact fade in/out and pop in/out effects on RecyclerView child cards & map card
     * Matching HomeBookFragment and TimetableFragment animations smoothly.
     */
    private void setupThresholdScrollAnimations() {
        if (binding == null) return;

        binding.scrollViewSearchResults.post(this::applyAllScrollEffects);

        binding.scrollViewSearchResults.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            applyAllScrollEffects();
        });
    }

    private void applyAllScrollEffects() {
        if (binding == null) return;

        // Apply effect to Route Map Card
        applyEffect(binding.cardMapRoute);

        // Apply effect to all items in Outbound RecyclerView
        RecyclerView rvOutbound = binding.rvTrains;
        if (rvOutbound != null) {
            for (int i = 0; i < rvOutbound.getChildCount(); i++) {
                applyEffect(rvOutbound.getChildAt(i));
            }
        }

        // Apply effect to all items in Return RecyclerView
        RecyclerView rvReturn = binding.rvReturnTrains;
        if (rvReturn != null && binding.layoutReturnJourneySection.getVisibility() == View.VISIBLE) {
            for (int i = 0; i < rvReturn.getChildCount(); i++) {
                applyEffect(rvReturn.getChildAt(i));
            }
        }
    }

    /**
     * Exact Threshold Interaction Effect Engine matching TimetableFragment:
     * Applies exact fade in/out and pop in/out effects on cards during upward and downward scrolling.
     */
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
            float ratio = Math.max(0.1f, (float) (screenHeight - viewTop) / 450f);
            alpha = ratio;
            scale = 0.9f + (ratio * 0.1f);
        }

        view.setAlpha(Math.max(0.1f, Math.min(1.0f, alpha)));
        view.setScaleX(Math.max(0.9f, Math.min(1.0f, scale)));
        view.setScaleY(Math.max(0.9f, Math.min(1.0f, scale)));
    }

    private void playPopInAnimation(View targetCard) {
        if (targetCard == null || getContext() == null) return;
        targetCard.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        android.view.animation.Animation popAnim = android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.anim_pop_in);
        targetCard.startAnimation(popAnim);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        try {
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);

            // Station Markers
            googleMap.addMarker(new MarkerOptions()
                    .position(originLatLng)
                    .title("Origin Station")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            googleMap.addMarker(new MarkerOptions()
                    .position(destLatLng)
                    .title("Destination Station")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

            // Polyline Route Line
            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(originLatLng)
                    .add(new LatLng(51.4900, -0.3100))
                    .add(destLatLng)
                    .width(8f)
                    .color(Color.parseColor("#1A103C"))
                    .geodesic(true);
            googleMap.addPolyline(polylineOptions);

            LatLngBounds bounds = new LatLngBounds.Builder()
                    .include(originLatLng)
                    .include(destLatLng)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80));
        } catch (Exception e) {
            showFallbackRouteVisualizer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isGmsAvailable && binding != null && binding.mapViewRoute != null) {
            binding.mapViewRoute.onResume();
        }
    }

    @Override
    public void onPause() {
        if (isGmsAvailable && binding != null && binding.mapViewRoute != null) {
            binding.mapViewRoute.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        if (isGmsAvailable && binding != null && binding.mapViewRoute != null) {
            binding.mapViewRoute.onDestroy();
        }
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (isGmsAvailable && binding != null && binding.mapViewRoute != null) {
            binding.mapViewRoute.onLowMemory();
        }
    }

    private static class TrainItem {
        String depTime, arrTime, duration, platform, status, price, seatsLeft, operator;
        TrainItem(String d, String a, String dur, String p, String s, String pr, String seats, String op) {
            this.depTime = d; this.arrTime = a; this.duration = dur;
            this.platform = p; this.status = s; this.price = pr; this.seatsLeft = seats; this.operator = op;
        }
    }

    private static class TrainAdapter extends RecyclerView.Adapter<TrainAdapter.ViewHolder> {
        private final List<TrainItem> items;
        private final OnTrainSelect listener;

        interface OnTrainSelect { void onSelect(TrainItem item); }
        TrainAdapter(List<TrainItem> items, OnTrainSelect listener) {
            this.items = items;
            this.listener = listener;
        }

        public void updateItems(List<TrainItem> newItems) {
            this.items.clear();
            if (newItems != null) {
                this.items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_train_card, parent, false);
            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TrainItem item = items.get(position);
            holder.tvDepTime.setText(item.depTime);
            holder.tvArrTime.setText(item.arrTime);
            holder.tvDuration.setText(item.duration);
            holder.tvPlatform.setText(item.platform);
            holder.tvPrice.setText(item.price);
            holder.tvStatusBadge.setText(item.status);
            if (holder.tvOperator != null) {
                holder.tvOperator.setText(item.operator + " 🚆");
            }
            holder.tvSeatsLeft.setText(item.seatsLeft);
            holder.itemView.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                android.view.animation.Animation popAnim = android.view.animation.AnimationUtils.loadAnimation(v.getContext(), R.anim.anim_pop_in);
                v.startAnimation(popAnim);
                v.postDelayed(() -> listener.onSelect(item), 120);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDepTime, tvArrTime, tvDuration, tvPlatform, tvPrice, tvStatusBadge, tvSeatsLeft, tvOperator;
            ViewHolder(View itemView) {
                super(itemView);
                tvDepTime = itemView.findViewById(R.id.tv_dep_time);
                tvArrTime = itemView.findViewById(R.id.tv_arr_time);
                tvDuration = itemView.findViewById(R.id.tv_duration);
                tvPlatform = itemView.findViewById(R.id.tv_platform);
                tvPrice = itemView.findViewById(R.id.tv_price);
                tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
                tvSeatsLeft = itemView.findViewById(R.id.tv_seats_left);
                tvOperator = itemView.findViewById(R.id.tv_operator);
            }
        }
    }
}
