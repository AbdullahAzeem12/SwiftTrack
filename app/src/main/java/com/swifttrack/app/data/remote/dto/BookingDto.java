package com.swifttrack.app.data.remote.dto;

import java.util.List;
import java.util.UUID;

public class BookingDto {

    public static class CreateBookingRequest {
        private UUID quoteId;
        private String contactEmail;
        private List<PassengerDetailDto> passengers;

        public CreateBookingRequest() {}

        public UUID getQuoteId() { return quoteId; }
        public void setQuoteId(UUID quoteId) { this.quoteId = quoteId; }

        public String getContactEmail() { return contactEmail; }
        public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

        public List<PassengerDetailDto> getPassengers() { return passengers; }
        public void setPassengers(List<PassengerDetailDto> passengers) { this.passengers = passengers; }
    }

    public static class PassengerDetailDto {
        private String passengerType;
        private String fullName;

        public PassengerDetailDto() {}

        public String getPassengerType() { return passengerType; }
        public void setPassengerType(String passengerType) { this.passengerType = passengerType; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
    }

    public static class BookingResponse {
        private UUID bookingId;
        private String bookingReference;
        private String status;
        private Integer totalAmountMinor;
        private String currency;
        private String contactEmail;
        private JourneyDto journey;
        private String createdAt;

        public BookingResponse() {}

        public UUID getBookingId() { return bookingId; }
        public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

        public String getBookingReference() { return bookingReference; }
        public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Integer getTotalAmountMinor() { return totalAmountMinor; }
        public void setTotalAmountMinor(Integer totalAmountMinor) { this.totalAmountMinor = totalAmountMinor; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getContactEmail() { return contactEmail; }
        public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

        public JourneyDto getJourney() { return journey; }
        public void setJourney(JourneyDto journey) { this.journey = journey; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}
