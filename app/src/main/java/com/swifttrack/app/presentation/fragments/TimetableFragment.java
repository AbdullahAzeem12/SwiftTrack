package com.swifttrack.app.presentation.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.swifttrack.app.databinding.FragmentTimetableBinding;
import com.swifttrack.app.presentation.adapters.TimetableAdapter;
import com.swifttrack.app.presentation.viewmodels.TimetableViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TimetableFragment extends Fragment {

    private FragmentTimetableBinding binding;
    private TimetableViewModel viewModel;
    private TimetableAdapter adapter;
    private final Calendar selectedCalendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTimetableBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TimetableViewModel.class);

        setupRecyclerView();
        setupListeners();
        observeViewModel();
        setupThresholdScrollAnimations();
    }

    private void setupRecyclerView() {
        adapter = new TimetableAdapter();
        binding.rvTimetable.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTimetable.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.swipeRefreshTimetable.setOnRefreshListener(() -> viewModel.fetchLiveTimetable(true));

        binding.btnSwapStations.setOnClickListener(v -> {
            binding.btnSwapStations.animate()
                    .rotationBy(180f)
                    .setDuration(300)
                    .start();
            viewModel.swapStations();
        });

        binding.cardSelectDate.setOnClickListener(v -> showDatePickerDialog());

        binding.btnCloseNotice.setOnClickListener(v -> viewModel.setNoticeBannerVisible(false));
    }

    /**
     * Threshold-based Scroll Animation Engine for Timetable RecyclerView:
     * Applies exact fade in/out and pop in/out effects on child rows during scroll
     */
    private void setupThresholdScrollAnimations() {
        if (binding == null) return;

        binding.nestedScrollViewTimetable.post(this::applyAllScrollEffects);

        binding.nestedScrollViewTimetable.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            applyAllScrollEffects();
        });
    }

    private void applyAllScrollEffects() {
        if (binding == null || binding.rvTimetable == null) return;

        for (int i = 0; i < binding.rvTimetable.getChildCount(); i++) {
            applyEffect(binding.rvTimetable.getChildAt(i));
        }
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

    private void observeViewModel() {
        viewModel.getFromStationName().observe(getViewLifecycleOwner(), fromName -> {
            if (binding == null) return;
            binding.tvFromStation.setText(fromName);
            updateColumnHeaders();
        });

        viewModel.getToStationName().observe(getViewLifecycleOwner(), toName -> {
            if (binding == null) return;
            binding.tvToStation.setText(toName);
            updateColumnHeaders();
        });

        viewModel.getSelectedDateStr().observe(getViewLifecycleOwner(), dateStr -> {
            if (binding == null) return;
            binding.tvSelectedDate.setText(dateStr);
        });

        viewModel.getTimetableList().observe(getViewLifecycleOwner(), list -> {
            if (binding == null) return;
            adapter.setItems(list);
            if (list == null || list.isEmpty()) {
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.rvTimetable.setVisibility(View.GONE);
            } else {
                binding.tvEmptyState.setVisibility(View.GONE);
                binding.rvTimetable.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) return;
            binding.swipeRefreshTimetable.setRefreshing(Boolean.TRUE.equals(loading));
            binding.pbTimetableLoading.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
        });

        viewModel.getIsNoticeBannerVisible().observe(getViewLifecycleOwner(), visible -> {
            if (binding == null) return;
            binding.cardNoticeBanner.setVisibility(Boolean.TRUE.equals(visible) ? View.VISIBLE : View.GONE);
        });

        viewModel.getLastUpdatedText().observe(getViewLifecycleOwner(), ts -> {
            if (binding == null) return;
            binding.tvLastUpdatedTime.setText(ts + " • Auto-refreshes every 30s");
        });
    }

    private void updateColumnHeaders() {
        if (binding == null) return;
        String from = binding.tvFromStation.getText().toString();
        boolean isFromHeathrow = from.toLowerCase().contains("heathrow");

        if (isFromHeathrow) {
            binding.tvCol1Header.setText("DEPARTS\nAirport\nTerminal 5");
            binding.tvCol2Header.setText("DEPARTS\nAirport\nTerminal 2 & 3");
            binding.tvCol3Header.setText("ARRIVES\nPaddington\nStation");
        } else {
            binding.tvCol1Header.setText("DEPARTS\nPaddington\nStation");
            binding.tvCol2Header.setText("ARRIVES\nAirport\nTerminal 2 & 3");
            binding.tvCol3Header.setText("ARRIVES\nAirport\nTerminal 5");
        }
    }

    private void showDatePickerDialog() {
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (v, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    SimpleDateFormat sdf = new SimpleDateFormat("d MMM ''yy", Locale.getDefault());
                    String formatted = sdf.format(selectedCalendar.getTime());
                    viewModel.setSelectedDateStr(formatted);
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
