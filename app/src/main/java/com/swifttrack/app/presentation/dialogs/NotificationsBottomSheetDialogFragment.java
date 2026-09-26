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
import com.swifttrack.app.data.model.NotificationItem;
import com.swifttrack.app.data.repository.NotificationRepository;
import com.swifttrack.app.databinding.BottomSheetNotificationsBinding;

import java.util.List;

public class NotificationsBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetNotificationsBinding binding;
    private NotificationRepository notificationRepository;

    public static NotificationsBottomSheetDialogFragment newInstance() {
        return new NotificationsBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        notificationRepository = NotificationRepository.getInstance();

        binding.btnCloseNotifications.setOnClickListener(v -> dismiss());
        binding.btnMarkAllRead.setOnClickListener(v -> {
            notificationRepository.markAllAsRead(requireContext());
            loadNotificationsList();
        });

        loadNotificationsList();
    }

    private void loadNotificationsList() {
        if (binding == null) return;
        binding.containerNotificationsList.removeAllViews();

        List<NotificationItem> items = notificationRepository.getNotifications(requireContext());
        int unreadCount = notificationRepository.getUnreadCount(requireContext());

        binding.tvNotificationsSheetUnreadCount.setText(unreadCount + (unreadCount == 1 ? " unread service alert" : " unread service alerts"));

        if (items.isEmpty()) {
            TextView emptyTv = new TextView(requireContext());
            emptyTv.setText("You have no notifications.");
            emptyTv.setTextColor(getResources().getColor(R.color.text_secondary, null));
            emptyTv.setPadding(16, 32, 16, 32);
            binding.containerNotificationsList.addView(emptyTv);
            return;
        }

        for (NotificationItem item : items) {
            View card = getLayoutInflater().inflate(R.layout.item_notification_card, binding.containerNotificationsList, false);

            TextView tvTitle = card.findViewById(R.id.tv_notif_title);
            TextView tvDesc = card.findViewById(R.id.tv_notif_desc);
            TextView tvDate = card.findViewById(R.id.tv_notif_date);
            View indicator = card.findViewById(R.id.view_unread_indicator);

            tvTitle.setText(item.title);
            tvDesc.setText(item.description);
            tvDate.setText(item.timestamp);
            indicator.setVisibility(item.isRead ? View.GONE : View.VISIBLE);

            card.setOnClickListener(v -> {
                notificationRepository.markAsRead(requireContext(), item.id);
                loadNotificationsList();
            });

            binding.containerNotificationsList.addView(card);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
