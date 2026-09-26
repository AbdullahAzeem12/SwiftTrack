package com.swifttrack.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JourneyDto {
    private UUID id;
    private String serviceCode;
    private StationDto departureStation;
    private StationDto arrivalStation;
    private ZonedDateTime scheduledDeparture;
    private ZonedDateTime scheduledArrival;
    private ZonedDateTime estimatedDeparture;
    private ZonedDateTime estimatedArrival;
    private String status;
    private String platform;
    private String capacityLevel;
    private List<FareOptionDto> fareOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FareOptionDto {
        private UUID fareProductId;
        private String code;
        private String name;
        private String travelClass;
        private String validityType;
        private Integer priceMinor;
        private String currency;
        private boolean isRefundable;
        private boolean isChangeable;
        private String description;
    }
}
