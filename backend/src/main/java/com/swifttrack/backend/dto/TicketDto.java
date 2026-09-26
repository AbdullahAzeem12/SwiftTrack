package com.swifttrack.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private ZonedDateTime validFrom;
    private ZonedDateTime validUntil;
    private String pdfUrl;
    private ZonedDateTime createdAt;
}
