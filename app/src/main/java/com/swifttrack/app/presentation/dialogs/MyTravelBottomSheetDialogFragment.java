package com.swifttrack.app.presentation.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.swifttrack.app.R;
import com.swifttrack.app.data.local.TicketEntity;
import com.swifttrack.app.data.repository.TicketRepository;
import com.swifttrack.app.databinding.BottomSheetMyTravelBinding;

import java.util.List;

public class MyTravelBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private BottomSheetMyTravelBinding binding;
    private TicketRepository ticketRepository;

    public static MyTravelBottomSheetDialogFragment newInstance() {
        return new MyTravelBottomSheetDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetMyTravelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ticketRepository = new TicketRepository(requireActivity().getApplication());

        binding.btnCloseMyTravel.setOnClickListener(v -> dismiss());
        binding.btnBookNewJourney.setOnClickListener(v -> {
            dismiss();
            try {
                NavHostFragment.findNavController(this).navigate(R.id.homeBookFragment);
            } catch (Exception ignored) {}
        });

        loadUserTickets();
    }

    private void loadUserTickets() {
        ticketRepository.getLocalTicketsLiveData().observe(getViewLifecycleOwner(), tickets -> {
            if (binding == null) return;
            binding.containerMyTravelList.removeAllViews();

            if (tickets == null || tickets.isEmpty()) {
                TextView emptyTv = new TextView(requireContext());
                emptyTv.setText("No active or historical bookings found.");
                emptyTv.setTextColor(getResources().getColor(R.color.text_secondary, null));
                emptyTv.setPadding(16, 32, 16, 32);
                binding.containerMyTravelList.addView(emptyTv);
                return;
            }

            binding.tvMyTravelSheetSub.setText(tickets.size() + (tickets.size() == 1 ? " verified booking record" : " verified booking records"));

            for (TicketEntity ticket : tickets) {
                View card = getLayoutInflater().inflate(R.layout.item_user_ticket_card, binding.containerMyTravelList, false);

                TextView tvRoute = card.findViewById(R.id.tv_ticket_card_route);
                TextView tvTimes = card.findViewById(R.id.tv_ticket_card_times);
                TextView tvRef = card.findViewById(R.id.tv_ticket_card_ref);
                TextView tvStatus = card.findViewById(R.id.tv_ticket_card_status);

                String orig = ticket.originStationName != null ? ticket.originStationName : "Paddington";
                String dest = ticket.destStationName != null ? ticket.destStationName : "Heathrow T2&3";

                tvRoute.setText(orig + " ➔ " + dest);
                tvTimes.setText("Dep: " + (ticket.departureTime != null ? ticket.departureTime : "12:00") + " • Class: " + (ticket.travelClass != null ? ticket.travelClass : "Express"));
                tvRef.setText("Ref: " + (ticket.bookingReference != null ? ticket.bookingReference : ticket.ticketId));

                String st = ticket.status != null ? ticket.status.toUpperCase() : "VALID";
                tvStatus.setText(st);

                if ("ACTIVE".equalsIgnoreCase(st) || "VALID".equalsIgnoreCase(st)) {
                    tvStatus.setTextColor(getResources().getColor(R.color.status_green, null));
                    tvStatus.setBackgroundResource(R.drawable.bg_status_green);
                } else {
                    tvStatus.setTextColor(getResources().getColor(R.color.text_secondary, null));
                    tvStatus.setBackgroundResource(R.drawable.bg_pill_badge);
                }

                binding.containerMyTravelList.addView(card);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
