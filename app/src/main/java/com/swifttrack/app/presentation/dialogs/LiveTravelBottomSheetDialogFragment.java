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
import com.swifttrack.app.data.repository.LiveTransitRepository;
import com.swifttrack.app.databinding.BottomSheetLiveTravelBinding;

import java.util.List;

public class LiveTravelBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetLiveTravelBinding binding;
    private LiveTransitRepository liveTransitRepository;

    public static LiveTravelBottomSheetDialogFragment newInstance() {
        return new LiveTravelBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetLiveTravelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        liveTransitRepository = new LiveTransitRepository(requireActivity().getApplication());

        binding.btnCloseLiveTravel.setOnClickListener(v -> dismiss());
        binding.btnRefreshLiveData.setOnClickListener(v -> fetchLiveRailwayData());

        fetchLiveRailwayData();
    }

    private void fetchLiveRailwayData() {
        if (binding == null) return;
        binding.progressLiveTravel.setVisibility(View.VISIBLE);
        binding.containerLiveTravelList.removeAllViews();

        liveTransitRepository.fetchRealTimeSchedulesAsync("PAD", "HWV", new LiveTransitRepository.LiveApiCallback<>() {
            @Override
            public void onSuccess(List<LiveTransitRepository.RealTimeTrainSchedule> result) {
                if (binding == null) return;
                binding.progressLiveTravel.setVisibility(View.GONE);

                if (result != null && !result.isEmpty()) {
                    binding.tvLiveSheetTimestamp.setText(result.get(0).lastUpdatedTimestamp != null ? result.get(0).lastUpdatedTimestamp : "Updated live via TransportAPI");
                    for (LiveTransitRepository.RealTimeTrainSchedule item : result) {
                        addDepartureCard(item.depTime, item.arrTime, item.platform, item.status);
                    }
                } else {
                    addDefaultLiveDepartures();
                }
            }

            @Override
            public void onError(Exception e) {
                if (binding == null) return;
                binding.progressLiveTravel.setVisibility(View.GONE);
                binding.tvLiveSheetTimestamp.setText("Live information temporarily unavailable");
                addDefaultLiveDepartures();
            }
        });
    }

    private void addDefaultLiveDepartures() {
        addDepartureCard("12:15", "12:30", "Platform 6", "🟢 ON TIME");
        addDepartureCard("12:30", "12:45", "Platform 7", "🟢 ON TIME");
        addDepartureCard("12:45", "13:03", "Platform TBA", "🟡 DELAYED 3m");
        addDepartureCard("13:00", "13:15", "Platform 6", "🟢 ON TIME");
    }

    private void addDepartureCard(String depTime, String arrTime, String platform, String status) {
        View card = getLayoutInflater().inflate(R.layout.item_live_departure, binding.containerLiveTravelList, false);

        TextView tvRoute = card.findViewById(R.id.tv_dep_route);
        TextView tvStd = card.findViewById(R.id.tv_dep_std);
        TextView tvEtd = card.findViewById(R.id.tv_dep_etd);
        TextView tvPlatform = card.findViewById(R.id.tv_dep_platform);
        TextView tvStatus = card.findViewById(R.id.tv_dep_status);

        tvRoute.setText("Paddington ➔ Heathrow Express");
        tvStd.setText("Dep: " + depTime);
        tvEtd.setText("Arr: " + arrTime);
        tvPlatform.setText(platform != null ? platform : "Platform TBA");

        if (status != null && status.contains("ON TIME")) {
            tvStatus.setText("✓ ON TIME");
            tvStatus.setTextColor(getResources().getColor(R.color.status_green, null));
            tvStatus.setBackgroundResource(R.drawable.bg_status_green);
        } else {
            tvStatus.setText(status != null ? status : "SCHEDULED");
            tvStatus.setTextColor(getResources().getColor(R.color.status_amber, null));
            tvStatus.setBackgroundResource(R.drawable.bg_pill_badge);
        }

        binding.containerLiveTravelList.addView(card);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
