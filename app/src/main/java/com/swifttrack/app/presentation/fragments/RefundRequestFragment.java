package com.swifttrack.app.presentation.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.swifttrack.app.core.util.ButterClickEffect;
import com.swifttrack.app.data.local.TicketEntity;
import com.swifttrack.app.data.model.NotificationItem;
import com.swifttrack.app.data.repository.NotificationRepository;
import com.swifttrack.app.data.repository.RewardsRepository;
import com.swifttrack.app.data.repository.TicketRepository;
import com.swifttrack.app.databinding.FragmentRefundRequestBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RefundRequestFragment extends Fragment {

    private FragmentRefundRequestBinding binding;
    private TicketRepository ticketRepository;
    private RewardsRepository rewardsRepository;

    private String ticketId = "";
    private String ticketCode = "";
    private String bookingRef = "";
    private TicketEntity targetTicket = null;

    private double passedOriginalPrice = 0.0;
    private double passedCancellationFee = 0.0;
    private double passedNetRefund = 0.0;
    private int passedRewardPoints = 0;
    private String generatedVoucherCode = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRefundRequestBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ticketRepository = new TicketRepository(requireContext());
        rewardsRepository = RewardsRepository.getInstance(requireContext());

        generatedVoucherCode = "SV-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        if (getArguments() != null) {
            ticketId = getArguments().getString("ticket_id", "");
            ticketCode = getArguments().getString("ticket_code", "TKT-ST-7X92Q4L8");
            bookingRef = getArguments().getString("booking_ref", "ST-9A72F6K1");
            passedOriginalPrice = getArguments().getDouble("original_price", 0.0);
            passedCancellationFee = getArguments().getDouble("cancellation_fee", 0.0);
            passedNetRefund = getArguments().getDouble("net_refund", 0.0);
            passedRewardPoints = getArguments().getInt("reward_points", 0);
        }

        bindTicketDetails();

        ButterClickEffect.apply(binding.btnConfirmRefund);
        binding.btnConfirmRefund.setOnClickListener(v -> handleConfirmRefund(view));
    }

    private void bindTicketDetails() {
        ticketRepository.getLocalTicketsLiveData().observe(getViewLifecycleOwner(), tickets -> {
            if (tickets != null && !tickets.isEmpty()) {
                for (TicketEntity t : tickets) {
                    if ((!ticketId.isEmpty() && ticketId.equals(t.ticketId)) ||
                        (!ticketCode.isEmpty() && ticketCode.equals(t.ticketCode))) {
                        targetTicket = t;
                        break;
                    }
                }
                if (targetTicket == null) {
                    targetTicket = tickets.get(0);
                }

                double origPrice = targetTicket != null ? targetTicket.getCalculatedOrStoredPrice() : (passedOriginalPrice > 0 ? passedOriginalPrice : 244.80);
                double fee = passedCancellationFee > 0 ? passedCancellationFee : Math.min(10.00, Math.max(5.00, origPrice * 0.05));
                double netRefund = Math.max(0.0, origPrice - fee);
                int rewardPoints = passedRewardPoints > 0 ? passedRewardPoints : (int) Math.round(netRefund * 100);

                passedOriginalPrice = origPrice;
                passedCancellationFee = fee;
                passedNetRefund = netRefund;
                passedRewardPoints = rewardPoints;

                binding.tvRefundTicketCode.setText("Ticket " + (targetTicket.ticketCode != null ? targetTicket.ticketCode : ticketCode));
                binding.tvRefundBookingRef.setText("Reference: " + (targetTicket.bookingReference != null ? targetTicket.bookingReference : bookingRef));

                String origin = targetTicket.originStationName != null ? targetTicket.originStationName : "London Paddington";
                String dest = targetTicket.destStationName != null ? targetTicket.destStationName : "Heathrow Airport";
                binding.tvRefundRoute.setText(origin + " ➔ " + dest);

                String cls = targetTicket.travelClass != null ? targetTicket.travelClass : "Standard Express";
                String trip = targetTicket.tripType != null ? targetTicket.tripType : "Return";

                String origStr = String.format(Locale.getDefault(), "£%.2f", origPrice);
                String feeStr = String.format(Locale.getDefault(), "£%.2f", fee);
                String netStr = String.format(Locale.getDefault(), "£%.2f", netRefund);
                String ptsStr = String.format(Locale.getDefault(), "%,d", rewardPoints);

                binding.tvRefundBreakdown.setText("Original Price: " + origStr + "\nFare Class: " + cls + " • " + trip + "\nCancellation Fee: " + feeStr);
                binding.tvEstimatedRefundTotal.setText(netStr);
                binding.rbRefundRewards.setText("🎁 Convert to SWIFTTRACK Rewards (+" + ptsStr + " pts)");
                binding.rbRefundVoucher.setText("🎫 Issue SWIFTTRACK Travel Voucher (" + generatedVoucherCode + " • " + netStr + ")");
            }
        });
    }

    private void handleConfirmRefund(View view) {
        if (targetTicket != null && "CANCELLED".equalsIgnoreCase(targetTicket.status)) {
            Toast.makeText(requireContext(), "This ticket has already been cancelled", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(view).navigateUp();
            return;
        }

        String targetIdToUpdate = targetTicket != null ? targetTicket.ticketId : ticketId;
        String activeUserId = rewardsRepository.resolveActiveUserId();
        String netStr = String.format(Locale.getDefault(), "£%.2f", passedNetRefund);
        String ptsStr = String.format(Locale.getDefault(), "%,d", passedRewardPoints);

        if (binding.rbRefundRewards.isChecked()) {
            rewardsRepository.refundAsPoints(activeUserId, passedRewardPoints, "Ref: " + bookingRef);
            Toast.makeText(requireContext(), "✓ +" + ptsStr + " SWIFTTRACK Reward Points (" + netStr + " equivalent) credited to your account!", Toast.LENGTH_LONG).show();
        } else if (binding.rbRefundVoucher.isChecked()) {
            Toast.makeText(requireContext(), "✓ Travel Voucher " + generatedVoucherCode + " (" + netStr + ") generated & saved to wallet!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), "✓ " + netStr + " Original Payment Method refund initiated!", Toast.LENGTH_LONG).show();
        }

        if (!targetIdToUpdate.isEmpty()) {
            ticketRepository.updateTicketStatus(targetIdToUpdate, "CANCELLED");
            com.swifttrack.app.data.repository.BookingFirestoreRepository.getInstance(requireContext())
                    .updateTicketStatusInFirestore(activeUserId, ticketCode, "CANCELLED");
        }

        // Post cancellation notification
        SimpleDateFormat tsFmt = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
        long now = System.currentTimeMillis();
        NotificationRepository.getInstance().addNotification(
                requireContext(),
                new NotificationItem(
                        "notif_cnl_" + now,
                        "⛔ Ticket Cancelled: " + ticketCode,
                        "Refund processed for " + netStr + " (+" + ptsStr + " pts). Ticket marked as CANCELLED.",
                        "RED",
                        "REFUND",
                        tsFmt.format(new Date(now)),
                        now,
                        false,
                        true,
                        ticketCode
                )
        );

        Navigation.findNavController(view).navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
