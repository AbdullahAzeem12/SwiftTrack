package com.swifttrack.app.data.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.swifttrack.app.data.local.SwiftTrackDatabase;
import com.swifttrack.app.data.local.TicketEntity;
import com.swifttrack.app.data.model.NotificationItem;
import com.swifttrack.app.data.remote.ApiClient;
import com.swifttrack.app.data.remote.SwiftTrackApi;
import com.swifttrack.app.data.remote.dto.TicketDto;
import com.swifttrack.app.data.repository.NotificationRepository;
import com.swifttrack.app.data.repository.RewardsRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Response;

public class PaymentRecoveryWorker extends Worker {

    public PaymentRecoveryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SwiftTrackApi api = ApiClient.getInstance(context);
        SwiftTrackDatabase db = SwiftTrackDatabase.getInstance(context);
        String activeUserId = RewardsRepository.getInstance(context).resolveActiveUserId();

        try {
            Response<List<TicketDto>> response = api.getUserTickets().execute();
            if (response.isSuccessful() && response.body() != null) {
                List<TicketDto> serverTickets = response.body();
                for (TicketDto dto : serverTickets) {
                    TicketEntity existing = db.ticketDao().getTicketByCode(dto.getTicketCode());
                    if (existing == null) {
                        TicketEntity newTicket = new TicketEntity(
                                dto.getTicketId() != null ? dto.getTicketId().toString() : "tkt_" + dto.getTicketCode(),
                                dto.getTicketCode(),
                                dto.getBookingReference(),
                                dto.getJourney() != null && dto.getJourney().getDepartureStation() != null
                                        ? dto.getJourney().getDepartureStation().getName()
                                        : "London Paddington",
                                dto.getJourney() != null && dto.getJourney().getArrivalStation() != null
                                        ? dto.getJourney().getArrivalStation().getName()
                                        : "Heathrow Airport",
                                "Scheduled Departure",
                                "Scheduled Arrival",
                                dto.getFareProductName(),
                                dto.getTravelClass(),
                                dto.getPassengerCategory(),
                                dto.getSignedQrPayload() != null ? dto.getSignedQrPayload() : dto.getTicketCode(),
                                dto.getStatus(),
                                dto.getValidUntil(),
                                dto.getPdfUrl(),
                                System.currentTimeMillis(),
                                "ONE-WAY",
                                "",
                                "",
                                ""
                        );
                        db.ticketDao().insert(newTicket);

                        com.swifttrack.app.data.repository.BookingFirestoreRepository.getInstance(context)
                                .createPermanentTicketInFirestore(activeUserId, newTicket);

                        SimpleDateFormat tsFmt = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
                        long now = System.currentTimeMillis();
                        NotificationRepository.getInstance().addNotification(
                                context,
                                new NotificationItem(
                                        "notif_recov_" + now,
                                        "🎟️ Ticket Recovered: " + dto.getTicketCode(),
                                        "Your booking was confirmed and your digital ticket is ready.",
                                        "PURPLE",
                                        "TICKET",
                                        tsFmt.format(new Date(now)),
                                        now,
                                        false,
                                        true,
                                        dto.getTicketCode()
                                )
                        );
                    }
                }
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
