package com.swifttrack.app.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.swifttrack.app.data.local.SwiftTrackDatabase;
import com.swifttrack.app.data.local.TicketDao;
import com.swifttrack.app.data.local.TicketEntity;
import com.swifttrack.app.data.remote.ApiClient;
import com.swifttrack.app.data.remote.SwiftTrackApi;
import com.swifttrack.app.data.remote.dto.TicketDto;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class TicketRepository {

    private final Context context;
    private final SwiftTrackApi api;
    private final TicketDao ticketDao;

    public TicketRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = ApiClient.getInstance(context);
        this.ticketDao = SwiftTrackDatabase.getInstance(context).ticketDao();
    }

    public LiveData<List<TicketEntity>> getLocalTicketsLiveData() {
        return ticketDao.getAllTicketsLiveData();
    }

    public LiveData<TicketEntity> getTicketByIdLiveData(String ticketId) {
        return ticketDao.getTicketByIdLiveData(ticketId);
    }

    public void insertTicket(TicketEntity ticket, Runnable onComplete) {
        Executors.newSingleThreadExecutor().execute(() -> {
            ticketDao.insert(ticket);
            if (onComplete != null) onComplete.run();
        });
    }

    public void updateTicketStatus(String ticketId, String status, Runnable onComplete) {
        Executors.newSingleThreadExecutor().execute(() -> {
            ticketDao.updateTicketStatus(ticketId, status);
            if (onComplete != null) onComplete.run();
        });
    }

    public void updateTicketStatus(String ticketId, String status) {
        updateTicketStatus(ticketId, status, null);
    }

    public void refreshTickets() {
        String activeUserId = BookingFirestoreRepository.getInstance(context).resolveActiveUserId();
        if (!activeUserId.isEmpty()) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(activeUserId)
                    .collection("tickets")
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null || snapshots == null) return;
                        List<TicketEntity> entities = new ArrayList<>();
                        long now = System.currentTimeMillis();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            String ticketId = doc.getString("ticketId");
                            String ticketCode = doc.getString("ticketCode");
                            if (ticketId == null && ticketCode == null) continue;

                            TicketEntity entity = new TicketEntity(
                                    ticketId != null ? ticketId : doc.getId(),
                                    ticketCode != null ? ticketCode : doc.getId(),
                                    doc.getString("bookingReference"),
                                    doc.getString("originStationName"),
                                    doc.getString("destStationName"),
                                    doc.getString("departureTime"),
                                    doc.getString("arrivalTime"),
                                    doc.getString("fareProductName"),
                                    doc.getString("travelClass"),
                                    doc.getString("passengerCategory"),
                                    doc.getString("signedQrPayload"),
                                    doc.getString("status"),
                                    doc.getString("validUntil"),
                                    doc.getString("pdfUrl"),
                                    now,
                                    doc.getString("tripType"),
                                    doc.getString("returnDate"),
                                    doc.getString("returnDepartureTime"),
                                    doc.getString("returnArrivalTime")
                            );
                            Double priceVal = doc.getDouble("pricePaid");
                            if (priceVal != null && priceVal > 0) {
                                entity.pricePaid = priceVal;
                            }
                            entities.add(entity);
                        }
                        if (!entities.isEmpty()) {
                            Executors.newSingleThreadExecutor().execute(() -> ticketDao.insertAll(entities));
                        }
                    });
        }

        api.getUserTickets().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<TicketDto>> call, Response<List<TicketDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TicketEntity> entities = new ArrayList<>();
                    long now = System.currentTimeMillis();
                    for (TicketDto dto : response.body()) {
                        entities.add(new TicketEntity(
                                dto.getTicketId().toString(),
                                dto.getTicketCode(),
                                dto.getBookingReference(),
                                dto.getJourney() != null ? dto.getJourney().getDepartureStation().getName() : "Paddington",
                                dto.getJourney() != null ? dto.getJourney().getArrivalStation().getName() : "Heathrow T5",
                                dto.getJourney() != null ? dto.getJourney().getScheduledDeparture() : "",
                                dto.getJourney() != null ? dto.getJourney().getScheduledArrival() : "",
                                dto.getFareProductName(),
                                dto.getTravelClass(),
                                dto.getPassengerCategory(),
                                dto.getSignedQrPayload(),
                                dto.getStatus(),
                                dto.getValidUntil(),
                                dto.getPdfUrl(),
                                now
                        ));
                    }
                    Executors.newSingleThreadExecutor().execute(() -> ticketDao.insertAll(entities));
                }
            }

            @Override
            public void onFailure(Call<List<TicketDto>> call, Throwable t) {
                // Offline mode: Room DB remains source of truth
            }
        });
    }

    public void fetchTicketByCode(String code, TicketCallback callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            TicketEntity local = ticketDao.getTicketByCode(code);
            if (local != null) {
                callback.onSuccess(local);
            }
            api.getTicketByCode(code).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<TicketDto> call, Response<TicketDto> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        TicketDto dto = response.body();
                        TicketEntity updated = new TicketEntity(
                                dto.getTicketId().toString(),
                                dto.getTicketCode(),
                                dto.getBookingReference(),
                                dto.getJourney() != null ? dto.getJourney().getDepartureStation().getName() : "Paddington",
                                dto.getJourney() != null ? dto.getJourney().getArrivalStation().getName() : "Heathrow T5",
                                dto.getJourney() != null ? dto.getJourney().getScheduledDeparture() : "",
                                dto.getJourney() != null ? dto.getJourney().getScheduledArrival() : "",
                                dto.getFareProductName(),
                                dto.getTravelClass(),
                                dto.getPassengerCategory(),
                                dto.getSignedQrPayload(),
                                dto.getStatus(),
                                dto.getValidUntil(),
                                dto.getPdfUrl(),
                                System.currentTimeMillis()
                        );
                        Executors.newSingleThreadExecutor().execute(() -> ticketDao.insert(updated));
                        callback.onSuccess(updated);
                    }
                }

                @Override
                public void onFailure(Call<TicketDto> call, Throwable t) {
                    if (local == null) {
                        callback.onError("Failed to load ticket offline");
                    }
                }
            });
        });
    }

    public interface TicketCallback {
        void onSuccess(TicketEntity ticket);
        void onError(String error);
    }
}
