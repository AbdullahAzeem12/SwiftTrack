package com.swifttrack.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "tickets")
public class TicketEntity {

    @PrimaryKey
    @NonNull
    public String ticketId;
    public String ticketCode;
    public String bookingReference;
    public String originStationName;
    public String destStationName;
    public String departureTime;
    public String arrivalTime;
    public String fareProductName;
    public String travelClass;
    public String passengerCategory;
    public String signedQrPayload;
    public String status;
    public String validUntil;
    public String pdfUrl;
    public long cachedAt;

    // Return Journey Fields
    public String tripType; // "ONE-WAY" or "RETURN"
    public String returnDate;
    public String returnDepartureTime;
    public String returnArrivalTime;

    // Price Paid
    public double pricePaid = 0.0;

    @Ignore
    public TicketEntity() {
        this.ticketId = "tkt_" + System.currentTimeMillis();
        this.tripType = "ONE-WAY";
    }

    @Ignore
    public TicketEntity(@NonNull String ticketId, String ticketCode, String bookingReference, String originStationName, String destStationName, String departureTime, String arrivalTime, String fareProductName, String travelClass, String passengerCategory, String signedQrPayload, String status, String validUntil, String pdfUrl, long cachedAt) {
        this(ticketId, ticketCode, bookingReference, originStationName, destStationName, departureTime, arrivalTime, fareProductName, travelClass, passengerCategory, signedQrPayload, status, validUntil, pdfUrl, cachedAt, "ONE-WAY", null, null, null);
    }

    public TicketEntity(@NonNull String ticketId, String ticketCode, String bookingReference, String originStationName, String destStationName, String departureTime, String arrivalTime, String fareProductName, String travelClass, String passengerCategory, String signedQrPayload, String status, String validUntil, String pdfUrl, long cachedAt, String tripType, String returnDate, String returnDepartureTime, String returnArrivalTime) {
        this.ticketId = ticketId;
        this.ticketCode = ticketCode;
        this.bookingReference = bookingReference;
        this.originStationName = originStationName;
        this.destStationName = destStationName;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.fareProductName = fareProductName;
        this.travelClass = travelClass;
        this.passengerCategory = passengerCategory;
        this.signedQrPayload = signedQrPayload;
        this.status = status;
        this.validUntil = validUntil;
        this.pdfUrl = pdfUrl;
        this.cachedAt = cachedAt;
        this.tripType = tripType != null ? tripType : "ONE-WAY";
        this.returnDate = returnDate;
        this.returnDepartureTime = returnDepartureTime;
        this.returnArrivalTime = returnArrivalTime;
    }

    public double getCalculatedOrStoredPrice() {
        if (pricePaid > 0) return pricePaid;

        int adults = 1;
        int children = 0;
        if (passengerCategory != null) {
            String p = passengerCategory.toLowerCase();
            if (p.contains("3 adult")) adults = 3;
            else if (p.contains("2 adult")) adults = 2;
            else if (p.contains("4 adult")) adults = 4;
            else if (p.contains("5 adult")) adults = 5;

            if (p.contains("2 child")) children = 2;
            else if (p.contains("1 child")) children = 1;
            else if (p.contains("3 child")) children = 3;
            else if (p.contains("4 child")) children = 4;
        }

        boolean isReturn = "RETURN".equalsIgnoreCase(tripType) || (passengerCategory != null && passengerCategory.toLowerCase().contains("return"));
        boolean isPremier = (travelClass != null && (travelClass.toLowerCase().contains("business") || travelClass.toLowerCase().contains("premier") || travelClass.toLowerCase().contains("first")));

        double passMult = Math.max(1, adults) + (children * 0.5);
        double journeyMult = isReturn ? 1.8 : 1.0;
        double unitRate = isPremier ? 34.00 : 25.00;

        return Math.round(unitRate * passMult * journeyMult * 100.0) / 100.0;
    }
}
