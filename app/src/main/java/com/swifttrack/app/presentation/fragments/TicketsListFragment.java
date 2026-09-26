package com.swifttrack.app.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.swifttrack.app.R;
import com.swifttrack.app.data.local.TicketEntity;
import com.swifttrack.app.data.repository.TicketRepository;
import com.swifttrack.app.databinding.FragmentTicketsListBinding;

import java.util.ArrayList;
import java.util.List;

public class TicketsListFragment extends Fragment {

    private FragmentTicketsListBinding binding;
    private TicketRepository repository;
    private TicketListAdapter adapter;

    private List<TicketEntity> masterTicketList = new ArrayList<>();
    private int currentTabPosition = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTicketsListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new TicketRepository(requireContext());

        setupRecyclerView(view);
        setupTabs();
        setupObservers();

        binding.swipeRefresh.setOnRefreshListener(() -> {
            repository.refreshTickets();
            binding.swipeRefresh.setRefreshing(false);
        });

        binding.btnEmptyBook.setOnClickListener(v -> {
            try {
                Navigation.findNavController(v).navigate(R.id.homeBookFragment);
            } catch (Exception ignored) {}
        });

        setupThresholdScrollAnimations();
    }

    private void setupRecyclerView(@NonNull View root) {
        binding.rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TicketListAdapter(ticket -> {
            if (ticket == null) return;
            Bundle args = new Bundle();
            args.putString("ticket_id", ticket.ticketId);
            args.putString("ticket_code", ticket.ticketCode);
            try {
                Navigation.findNavController(root).navigate(R.id.action_ticketsList_to_ticketDetails, args);
            } catch (Exception ignored) {}
        });
        binding.rvTickets.setAdapter(adapter);
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                filterAndDisplayTickets();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupObservers() {
        repository.getLocalTicketsLiveData().observe(getViewLifecycleOwner(), tickets -> {
            if (tickets != null) {
                masterTicketList = new ArrayList<>(tickets);
                filterAndDisplayTickets();
            }
        });
        repository.refreshTickets();
    }

    private void filterAndDisplayTickets() {
        if (binding == null) return;

        List<TicketEntity> filteredList = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (TicketEntity ticket : masterTicketList) {
            if (ticket == null) continue;

            String status = ticket.status != null ? ticket.status.toUpperCase() : "ACTIVE";
            boolean isCancelledOrRefunded = status.contains("CANCEL") || status.contains("REFUND");
            boolean isUsedOrExpired = status.contains("USED") || status.contains("COMPLETED") || status.contains("EXPIRED");

            switch (currentTabPosition) {
                case 0: // Active Tab
                    if (!isCancelledOrRefunded && !isUsedOrExpired) {
                        filteredList.add(ticket);
                    }
                    break;
                case 1: // Upcoming Tab
                    if (!isCancelledOrRefunded && !isUsedOrExpired) {
                        filteredList.add(ticket);
                    }
                    break;
                case 2: // Past Tab
                    if (isUsedOrExpired) {
                        filteredList.add(ticket);
                    }
                    break;
                case 3: // Refunded / Cancelled Tab
                    if (isCancelledOrRefunded) {
                        filteredList.add(ticket);
                    }
                    break;
            }
        }

        adapter.submitList(filteredList);

        if (filteredList.isEmpty()) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.swipeRefresh.setVisibility(View.GONE);
            updateEmptyStateText();
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.swipeRefresh.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyStateText() {
        if (binding == null) return;
        switch (currentTabPosition) {
            case 0:
                binding.tvEmptyIcon.setText("🎫");
                binding.tvEmptyTitle.setText("No Active Tickets");
                binding.tvEmptyDescription.setText("Your currently active tickets will appear here.");
                break;
            case 1:
                binding.tvEmptyIcon.setText("🚆");
                binding.tvEmptyTitle.setText("No Upcoming Journeys");
                binding.tvEmptyDescription.setText("You don't have any upcoming tickets yet.");
                break;
            case 2:
                binding.tvEmptyIcon.setText("🕘");
                binding.tvEmptyTitle.setText("No Past Tickets");
                binding.tvEmptyDescription.setText("Completed journeys will appear here.");
                break;
            case 3:
                binding.tvEmptyIcon.setText("↩");
                binding.tvEmptyTitle.setText("No Refunded or Cancelled Tickets");
                binding.tvEmptyDescription.setText("Your refunded or cancelled tickets will appear here.");
                break;
        }
    }

    private void setupThresholdScrollAnimations() {
        if (binding == null) return;

        binding.rvTickets.post(this::applyAllScrollEffects);
        binding.rvTickets.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                applyAllScrollEffects();
            }
        });
    }

    private void applyAllScrollEffects() {
        if (binding == null || binding.rvTickets == null) return;

        for (int i = 0; i < binding.rvTickets.getChildCount(); i++) {
            applyEffect(binding.rvTickets.getChildAt(i));
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
            float ratio = Math.max(0.1f, (float) (screenHeight - viewTop) / 450f);
            alpha = ratio;
            scale = 0.9f + (ratio * 0.1f);
        }

        view.setAlpha(Math.max(0.1f, Math.min(1.0f, alpha)));
        view.setScaleX(Math.max(0.9f, Math.min(1.0f, scale)));
        view.setScaleY(Math.max(0.9f, Math.min(1.0f, scale)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class TicketListAdapter extends ListAdapter<TicketEntity, TicketListAdapter.ViewHolder> {
        private final OnTicketClickListener listener;

        interface OnTicketClickListener { void onTicketClick(TicketEntity ticket); }

        private static final DiffUtil.ItemCallback<TicketEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(@NonNull TicketEntity oldItem, @NonNull TicketEntity newItem) {
                return oldItem.ticketId.equals(newItem.ticketId);
            }

            @Override
            public boolean areContentsTheSame(@NonNull TicketEntity oldItem, @NonNull TicketEntity newItem) {
                return oldItem.ticketId.equals(newItem.ticketId) &&
                        java.util.Objects.equals(oldItem.status, newItem.status) &&
                        java.util.Objects.equals(oldItem.ticketCode, newItem.ticketCode) &&
                        java.util.Objects.equals(oldItem.departureTime, newItem.departureTime);
            }
        };

        TicketListAdapter(OnTicketClickListener listener) {
            super(DIFF_CALLBACK);
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticket_card, parent, false);
            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TicketEntity t = getItem(position);
            if (t == null) return;

            String origin = t.originStationName != null ? t.originStationName : "London Paddington";
            String dest = t.destStationName != null ? t.destStationName : "Heathrow Airport";
            holder.tvRoute.setText(origin + " ➔ " + dest);

            holder.tvCode.setText("Code: " + (t.ticketCode != null ? t.ticketCode : t.ticketId));

            String cls = t.travelClass != null ? t.travelClass : "Standard Class";
            String pax = t.passengerCategory != null ? t.passengerCategory : "1 Adult";
            holder.tvClass.setText(cls + " • " + pax);

            if (holder.tvDate != null) {
                holder.tvDate.setText(t.departureTime != null ? t.departureTime : "Today");
            }

            if (holder.tvStatusBadge != null) {
                String status = t.status != null ? t.status.toUpperCase() : "ACTIVE";
                if (status.contains("CANCEL") || status.contains("REFUND")) {
                    holder.tvStatusBadge.setText("CANCELLED");
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_badge);
                    holder.tvStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_red));
                } else if (status.contains("USED") || status.contains("COMPLETED")) {
                    holder.tvStatusBadge.setText("COMPLETED");
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_badge);
                    holder.tvStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_blue));
                } else if (status.contains("EXPIRED")) {
                    holder.tvStatusBadge.setText("EXPIRED");
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_badge);
                    holder.tvStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_red));
                } else {
                    holder.tvStatusBadge.setText("ACTIVE");
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_green);
                    holder.tvStatusBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_green));
                }
            }

            holder.itemView.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                android.view.animation.Animation popAnim = android.view.animation.AnimationUtils.loadAnimation(v.getContext(), R.anim.anim_pop_in);
                v.startAnimation(popAnim);
                v.postDelayed(() -> listener.onTicketClick(t), 120);
            });
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRoute, tvCode, tvClass, tvDate, tvStatusBadge;
            ViewHolder(View itemView) {
                super(itemView);
                tvRoute = itemView.findViewById(R.id.tv_ticket_item_route);
                tvCode = itemView.findViewById(R.id.tv_ticket_item_code);
                tvClass = itemView.findViewById(R.id.tv_ticket_item_class);
                tvDate = itemView.findViewById(R.id.tv_ticket_item_date);
                tvStatusBadge = itemView.findViewById(R.id.tv_ticket_item_status_badge);
            }
        }
    }
}
