package com.swifttrack.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class BookingDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateBookingRequest {
        @NotNull
        private UUID quoteId;
        @NotBlank
        @Email
        private String contactEmail;
        private List<PassengerDetailDto> passengers;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PassengerDetailDto {
        private String passengerType; // ADULT, CHILD, SENIOR, STUDENT
        private String fullName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingResponse {
        private UUID bookingId;
        private String bookingReference;
        private String status;
        private Integer totalAmountMinor;
        private String currency;
        private String contactEmail;
        private JourneyDto journey;
        private ZonedDateTime createdAt;
    }
}
