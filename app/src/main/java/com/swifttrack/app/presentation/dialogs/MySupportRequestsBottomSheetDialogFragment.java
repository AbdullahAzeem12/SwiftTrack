package com.swifttrack.app.presentation.dialogs;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.swifttrack.app.R;
import com.swifttrack.app.core.util.ButterClickEffect;
import com.swifttrack.app.data.model.SupportTicket;
import com.swifttrack.app.databinding.BottomSheetMySupportRequestsBinding;
import com.swifttrack.app.presentation.viewmodels.SupportViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MySupportRequestsBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetMySupportRequestsBinding binding;
    private SupportViewModel viewModel;

    public static MySupportRequestsBottomSheetDialogFragment newInstance() {
        return new MySupportRequestsBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetMySupportRequestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SupportViewModel.class);

        // Micro-interactions & Haptics
        ButterClickEffect.apply(binding.btnCloseMySupportRequests);
        ButterClickEffect.apply(binding.btnNewSupportRequest);

        binding.btnCloseMySupportRequests.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            dismiss();
        });

        binding.btnNewSupportRequest.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            dismiss();
            ContactSupportBottomSheetDialogFragment.newInstance().show(getParentFragmentManager(), "contact_support");
        });

        binding.progressSupportRequests.setVisibility(View.VISIBLE);

        viewModel.getUserTickets().observe(getViewLifecycleOwner(), this::renderTicketsList);
        viewModel.startObservingUserTickets();
    }

    private void renderTicketsList(List<SupportTicket> tickets) {
        if (binding == null) return;
        binding.progressSupportRequests.setVisibility(View.GONE);
        binding.containerSupportRequestsList.removeAllViews();

        if (tickets == null || tickets.isEmpty()) {
            TextView emptyTv = new TextView(requireContext());
            emptyTv.setText("You have no active support requests.");
            emptyTv.setTextColor(getResources().getColor(R.color.text_secondary, null));
            emptyTv.setPadding(16, 32, 16, 32);
            binding.containerSupportRequestsList.addView(emptyTv);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.UK);

        for (SupportTicket ticket : tickets) {
            View card = getLayoutInflater().inflate(R.layout.item_support_ticket, binding.containerSupportRequestsList, false);

            TextView tvRef = card.findViewById(R.id.tv_item_ticket_ref);
            TextView tvCategory = card.findViewById(R.id.tv_item_ticket_category);
            TextView tvSubject = card.findViewById(R.id.tv_item_ticket_subject);
            TextView tvStatus = card.findViewById(R.id.tv_item_ticket_status);
            TextView tvDate = card.findViewById(R.id.tv_item_ticket_date);

            tvRef.setText(ticket.getTicketId());
            tvCategory.setText(ticket.getCategory() != null ? ticket.getCategory() : "Support Request");
            tvSubject.setText(ticket.getSubject() != null ? ticket.getSubject() : "Enquiry");
            tvDate.setText(ticket.getCreatedAtTimestamp() > 0 ? "Submitted: " + sdf.format(new Date(ticket.getCreatedAtTimestamp())) : "");

            String status = ticket.getStatus() != null ? ticket.getStatus() : "OPEN";

            if ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
                tvStatus.setText("✓ " + status);
                tvStatus.setTextColor(getResources().getColor(R.color.status_green, null));
                tvStatus.setBackgroundResource(R.drawable.bg_status_green);
            } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                tvStatus.setText("● IN PROGRESS");
                tvStatus.setTextColor(getResources().getColor(R.color.primary_indigo, null));
                tvStatus.setBackgroundResource(R.drawable.bg_pill_badge);
            } else {
                tvStatus.setText("● OPEN");
                tvStatus.setTextColor(getResources().getColor(R.color.status_amber, null));
                tvStatus.setBackgroundResource(R.drawable.bg_pill_badge);
            }

            binding.containerSupportRequestsList.addView(card);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
