package com.swifttrack.app.data.remote.dto;

import java.util.UUID;

public class TicketDto {
    private UUID ticketId;
    private String ticketCode;
    private String bookingReference;
    private JourneyDto journey;
    private String fareProductName;
    private String travelClass;
    private String passengerCategory;
    private String signedQrPayload;
    private String status;
    private String validFrom;
    private String validUntil;
    private String pdfUrl;
    private String createdAt;

    public TicketDto() {}

    public UUID getTicketId() { return ticketId; }
    public void setTicketId(UUID ticketId) { this.ticketId = ticketId; }

    public String getTicketCode() { return ticketCode; }
    public void setTicketCode(String ticketCode) { this.ticketCode = ticketCode; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }

    public JourneyDto getJourney() { return journey; }
    public void setJourney(JourneyDto journey) { this.journey = journey; }

    public String getFareProductName() { return fareProductName; }
    public void setFareProductName(String fareProductName) { this.fareProductName = fareProductName; }

    public String getTravelClass() { return travelClass; }
    public void setTravelClass(String travelClass) { this.travelClass = travelClass; }

    public String getPassengerCategory() { return passengerCategory; }
    public void setPassengerCategory(String passengerCategory) { this.passengerCategory = passengerCategory; }

    public String getSignedQrPayload() { return signedQrPayload; }
    public void setSignedQrPayload(String signedQrPayload) { this.signedQrPayload = signedQrPayload; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getValidFrom() { return validFrom; }
    public void setValidFrom(String validFrom) { this.validFrom = validFrom; }

    public String getValidUntil() { return validUntil; }
    public void setValidUntil(String validUntil) { this.validUntil = validUntil; }

    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
