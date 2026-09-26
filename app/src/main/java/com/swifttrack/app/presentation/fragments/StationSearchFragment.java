package com.swifttrack.app.presentation.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.swifttrack.app.R;
import com.swifttrack.app.data.local.StationEntity;
import com.swifttrack.app.databinding.FragmentStationSearchBinding;
import com.swifttrack.app.presentation.viewmodels.HomeBookViewModel;

import java.util.ArrayList;
import java.util.List;

public class StationSearchFragment extends Fragment {

    private FragmentStationSearchBinding binding;
    private HomeBookViewModel viewModel;
    private StationAdapter adapter;
    private final List<StationEntity> allStations = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStationSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeBookViewModel.class);

        String searchTarget = getArguments() != null ? getArguments().getString("search_target", "TO") : "TO";

        binding.rvStations.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new StationAdapter(station -> {
            if ("FROM".equalsIgnoreCase(searchTarget)) {
                viewModel.setOriginStation(station);
            } else {
                viewModel.setDestinationStation(station);
            }
            Navigation.findNavController(view).navigateUp();
        });
        binding.rvStations.setAdapter(adapter);

        viewModel.getStationsLiveData().observe(getViewLifecycleOwner(), stations -> {
            if (stations != null) {
                allStations.clear();
                allStations.addAll(stations);
                adapter.setStations(allStations);
            }
        });

        binding.etSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStations(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterStations(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.setStations(allStations);
            return;
        }
        String q = query.toLowerCase().trim();
        List<StationEntity> filtered = new ArrayList<>();
        for (StationEntity s : allStations) {
            if (s.name.toLowerCase().contains(q) || s.code.toLowerCase().contains(q)) {
                filtered.add(s);
            }
        }
        adapter.setStations(filtered);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class StationAdapter extends RecyclerView.Adapter<StationAdapter.ViewHolder> {
        private final List<StationEntity> items = new ArrayList<>();
        private final OnStationClickListener listener;

        interface OnStationClickListener { void onStationClick(StationEntity station); }

        StationAdapter(OnStationClickListener listener) { this.listener = listener; }

        void setStations(List<StationEntity> stations) {
            items.clear();
            items.addAll(stations);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StationEntity s = items.get(position);
            TextView t1 = holder.itemView.findViewById(android.R.id.text1);
            TextView t2 = holder.itemView.findViewById(android.R.id.text2);

            String title = s.name + " (" + s.code + ")";
            if (s.isAirportTerminal) {
                title += " ✈️ Airport";
            }
            t1.setText(title);
            t1.setTextSize(16);
            t2.setText(s.city != null ? s.city : "London");

            holder.itemView.setOnClickListener(v -> listener.onStationClick(s));
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) { super(itemView); }
        }
    }
}
