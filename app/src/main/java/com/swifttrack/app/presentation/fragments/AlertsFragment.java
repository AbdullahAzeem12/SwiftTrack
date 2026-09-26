package com.swifttrack.app.presentation.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.swifttrack.app.R;
import com.swifttrack.app.data.model.NotificationItem;
import com.swifttrack.app.data.repository.LiveTransitRepository;
import com.swifttrack.app.data.repository.NotificationRepository;
import com.swifttrack.app.databinding.FragmentAlertsBinding;
import com.swifttrack.app.presentation.activities.MainActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertsFragment extends Fragment {

    private FragmentAlertsBinding binding;
    private NotificationRepository notifRepository;
    private LiveTransitRepository transitRepository;
    private AlertsAdapter adapter;
    private List<NotificationItem> currentAllList = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAlertsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        notifRepository = NotificationRepository.getInstance();
        transitRepository = new LiveTransitRepository(requireContext());

        binding.rvAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AlertsAdapter(new ArrayList<>(), new NotificationClickListener() {
            @Override
            public void onItemClick(NotificationItem item) {
                handleItemClick(item);
            }

            @Override
            public void onItemLongClick(NotificationItem item, View itemView) {
                showItemOptionsDialog(item);
            }
        });
        binding.rvAlerts.setAdapter(adapter);

        setupScrollAnimations();
        setupFilterChips();
        setupHeaderMenu();

        loadNotifications();

        binding.swipeRefreshAlerts.setOnRefreshListener(() -> {
            refreshLiveApiData();
            binding.swipeRefreshAlerts.setRefreshing(false);
        });

        binding.btnEmptyRefresh.setOnClickListener(v -> refreshLiveApiData());
    }

    private void setupHeaderMenu() {
        binding.btnAlertsMoreOptions.setOnClickListener(v -> showHeaderPopupMenu(v));
    }

    private void showHeaderPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, "🧹 Delete All Notifications");
        popup.getMenu().add(0, 2, 1, "✉️ Mark All as Read");
        popup.getMenu().add(0, 3, 2, "🔄 Refresh Live Alerts");
        popup.getMenu().add(0, 4, 3, "📌 Pin Unread Alerts to Top");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                confirmDeleteAll();
                return true;
            } else if (id == 2) {
                notifRepository.markAllAsRead(requireContext());
                loadNotifications();
                Toast.makeText(requireContext(), "All notifications marked as read", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == 3) {
                refreshLiveApiData();
                return true;
            } else if (id == 4) {
                List<NotificationItem> list = notifRepository.getNotifications(requireContext());
                for (NotificationItem n : list) {
                    if (!n.isRead) n.isPinned = true;
                }
                notifRepository.saveNotifications(requireContext(), list);
                loadNotifications();
                Toast.makeText(requireContext(), "Unread notifications pinned to top", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmDeleteAll() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear All Notifications?")
                .setMessage("Are you sure you want to delete all notifications? This action cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    notifRepository.deleteAllNotifications(requireContext());
                    loadNotifications();
                    Toast.makeText(requireContext(), "All notifications cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupFilterChips() {
        binding.chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_filter_unread) {
                currentFilter = "UNREAD";
            } else if (checkedId == R.id.chip_filter_tickets) {
                currentFilter = "TICKET";
            } else if (checkedId == R.id.chip_filter_trains) {
                currentFilter = "TRAIN_ARRIVAL";
            } else if (checkedId == R.id.chip_filter_disruptions) {
                currentFilter = "DISRUPTION";
            } else {
                currentFilter = "ALL";
            }
            applyCurrentFilter();
        });
    }

    private void loadNotifications() {
        currentAllList = notifRepository.getNotifications(requireContext());
        applyCurrentFilter();
        updateBadgeCount();
    }

    private void applyCurrentFilter() {
        List<NotificationItem> filtered = new ArrayList<>();
        for (NotificationItem item : currentAllList) {
            if ("UNREAD".equalsIgnoreCase(currentFilter)) {
                if (!item.isRead) filtered.add(item);
            } else if ("TICKET".equalsIgnoreCase(currentFilter)) {
                if ("TICKET".equalsIgnoreCase(item.category)) filtered.add(item);
            } else if ("TRAIN_ARRIVAL".equalsIgnoreCase(currentFilter)) {
                if ("TRAIN_ARRIVAL".equalsIgnoreCase(item.category)) filtered.add(item);
            } else if ("DISRUPTION".equalsIgnoreCase(currentFilter)) {
                if ("DISRUPTION".equalsIgnoreCase(item.category)) filtered.add(item);
            } else {
                filtered.add(item);
            }
        }

        adapter.setItems(filtered);

        if (filtered.isEmpty()) {
            binding.rvAlerts.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            binding.rvAlerts.setVisibility(View.VISIBLE);
            binding.layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void refreshLiveApiData() {
        binding.swipeRefreshAlerts.setRefreshing(true);
        // Query live service alerts from TransportAPI using live app credentials
        new Thread(() -> {
            List<LiveTransitRepository.RealTimeServiceAlert> apiAlerts = transitRepository.getLiveServiceAlerts();
            SimpleDateFormat tsFmt = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
            long now = System.currentTimeMillis();

            if (apiAlerts != null && !apiAlerts.isEmpty()) {
                for (LiveTransitRepository.RealTimeServiceAlert alert : apiAlerts) {
                    NotificationItem item = new NotificationItem(
                            "api_alert_" + Math.abs(alert.title.hashCode()),
                            alert.title,
                            alert.description,
                            alert.severity != null ? alert.severity : "GREEN",
                            "DISRUPTION",
                            tsFmt.format(new Date(now)),
                            now,
                            false,
                            false,
                            null
                    );
                    notifRepository.addNotification(requireContext(), item);
                }
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                binding.swipeRefreshAlerts.setRefreshing(false);
                loadNotifications();
                Toast.makeText(requireContext(), "⚡ Real-time alerts synced from TransportAPI", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void handleItemClick(NotificationItem item) {
        if (!item.isRead) {
            notifRepository.markAsRead(requireContext(), item.id);
            loadNotifications();
        }

        if (item.actionTarget != null && item.actionTarget.startsWith("TKT-")) {
            Bundle args = new Bundle();
            args.putString("ticket_code", item.actionTarget);
            NavHostFragment.findNavController(AlertsFragment.this)
                    .navigate(R.id.action_alerts_to_ticketDetails, args);
        } else {
            Toast.makeText(requireContext(), item.title, Toast.LENGTH_SHORT).show();
        }
    }

    private void showItemOptionsDialog(NotificationItem item) {
        String[] options = new String[]{
                "🗑️ Delete Notification",
                item.isRead ? "✉️ Mark as Unread" : "👁️ Mark as Read",
                item.isPinned ? "📌 Unpin from Top" : "📌 Pin to Top",
                "📋 Copy Alert Details"
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(item.title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        notifRepository.deleteNotification(requireContext(), item.id);
                        loadNotifications();
                        Toast.makeText(requireContext(), "Notification deleted", Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        notifRepository.toggleReadStatus(requireContext(), item.id);
                        loadNotifications();
                    } else if (which == 2) {
                        notifRepository.togglePin(requireContext(), item.id);
                        loadNotifications();
                    } else if (which == 3) {
                        ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Alert", item.title + "\n" + item.description);
                        if (cm != null) cm.setPrimaryClip(clip);
                        Toast.makeText(requireContext(), "Alert details copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void updateBadgeCount() {
        int unread = notifRepository.getUnreadCount(requireContext());
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateAlertsBadge(unread);
        }
    }

    private void setupScrollAnimations() {
        binding.rvAlerts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int firstVisible = layoutManager.findFirstVisibleItemPosition();
                int lastVisible = layoutManager.findLastVisibleItemPosition();

                for (int i = firstVisible; i <= lastVisible; i++) {
                    View child = layoutManager.findViewByPosition(i);
                    if (child != null) {
                        int top = child.getTop();
                        int height = child.getHeight();
                        int recyclerHeight = recyclerView.getHeight();

                        float factor = 1.0f;
                        if (top < 0) {
                            // Only fade out when item is scrolling off the top boundary
                            factor = Math.max(0.2f, (float) (height + top) / (float) height);
                        } else if (top + height > recyclerHeight - 120) {
                            // Smooth fade-in & scale pop-in as items enter from the bottom
                            int distFromBottom = recyclerHeight - (top + height);
                            factor = Math.max(0.4f, 1.0f - ((float) Math.abs(distFromBottom) / 120.0f));
                        }

                        child.setAlpha(factor);
                        float scale = 0.92f + (0.08f * factor);
                        child.setScaleX(scale);
                        child.setScaleY(scale);
                    }
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private interface NotificationClickListener {
        void onItemClick(NotificationItem item);
        void onItemLongClick(NotificationItem item, View itemView);
    }

    private static class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.ViewHolder> {
        private final List<NotificationItem> items = new ArrayList<>();
        private final NotificationClickListener listener;
        private int lastAnimatedPosition = -1;

        AlertsAdapter(List<NotificationItem> initialList, NotificationClickListener listener) {
            if (initialList != null) this.items.addAll(initialList);
            this.listener = listener;
        }

        void setItems(List<NotificationItem> newItems) {
            this.items.clear();
            if (newItems != null) this.items.addAll(newItems);
            this.lastAnimatedPosition = -1;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service_alert_card, parent, false);
            return new ViewHolder(card);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationItem item = items.get(position);
            Context ctx = holder.itemView.getContext();

            holder.tvTitle.setText(item.title);
            holder.tvDescription.setText(item.description);
            holder.tvTime.setText(item.timestamp);

            // Category & Badge tag formatting
            if ("TICKET".equalsIgnoreCase(item.category)) {
                holder.tvCategory.setText("🎫 TICKET");
                holder.tvCategory.setTextColor(ContextCompat.getColor(ctx, R.color.primary_indigo));
            } else if ("TRAIN_ARRIVAL".equalsIgnoreCase(item.category)) {
                holder.tvCategory.setText("🚆 LIVE TRAIN");
                holder.tvCategory.setTextColor(ContextCompat.getColor(ctx, R.color.accent_blue));
            } else if ("DISRUPTION".equalsIgnoreCase(item.category)) {
                holder.tvCategory.setText("⚠️ DISRUPTION");
                holder.tvCategory.setTextColor(ContextCompat.getColor(ctx, R.color.status_amber));
            } else {
                holder.tvCategory.setText("💡 SYSTEM");
                holder.tvCategory.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary));
            }

            // Severity color formatting
            if ("GREEN".equalsIgnoreCase(item.severity)) {
                holder.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.status_green));
            } else if ("AMBER".equalsIgnoreCase(item.severity)) {
                holder.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.status_amber));
            } else if ("RED".equalsIgnoreCase(item.severity)) {
                holder.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.status_red));
            } else if ("BLUE".equalsIgnoreCase(item.severity)) {
                holder.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.accent_blue));
            } else if ("PURPLE".equalsIgnoreCase(item.severity)) {
                holder.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.primary_indigo));
            } else {
                holder.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary));
            }

            // Read / Unread Indicator & Card Opacity
            if (item.isRead) {
                holder.vUnreadDot.setVisibility(View.GONE);
                holder.itemView.setAlpha(0.85f);
                holder.cardView.setStrokeColor(ContextCompat.getColor(ctx, R.color.divider_color));
            } else {
                holder.vUnreadDot.setVisibility(View.VISIBLE);
                holder.itemView.setAlpha(1.0f);
                holder.cardView.setStrokeColor(ContextCompat.getColor(ctx, R.color.primary_indigo));
            }

            // Pinned Tag
            holder.tvPinnedTag.setVisibility(item.isPinned ? View.VISIBLE : View.GONE);

            // Interactivity listeners
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onItemLongClick(item, v);
                return true;
            });

            // Entrance Pop-In & Fade-In Animation
            if (position == 0) {
                // Very first notification is 100% fully visible at start
                holder.itemView.setAlpha(item.isRead ? 0.85f : 1.0f);
                holder.itemView.setScaleX(1.0f);
                holder.itemView.setScaleY(1.0f);
                if (lastAnimatedPosition < 0) lastAnimatedPosition = 0;
            } else if (position > lastAnimatedPosition) {
                holder.itemView.setAlpha(0f);
                holder.itemView.setScaleX(0.92f);
                holder.itemView.setScaleY(0.92f);
                holder.itemView.animate()
                        .alpha(item.isRead ? 0.85f : 1.0f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(260)
                        .setStartDelay(position * 40L)
                        .start();
                lastAnimatedPosition = position;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            com.google.android.material.card.MaterialCardView cardView;
            TextView tvCategory, tvPinnedTag, tvTitle, tvDescription, tvTime;
            View vUnreadDot;

            ViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.card_alert_item);
                tvCategory = itemView.findViewById(R.id.tv_alert_category);
                tvPinnedTag = itemView.findViewById(R.id.tv_pinned_tag);
                tvTitle = itemView.findViewById(R.id.tv_alert_title);
                tvDescription = itemView.findViewById(R.id.tv_alert_description);
                tvTime = itemView.findViewById(R.id.tv_alert_time);
                vUnreadDot = itemView.findViewById(R.id.v_unread_indicator);
            }
        }
    }
}
