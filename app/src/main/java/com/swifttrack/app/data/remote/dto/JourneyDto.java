package com.swifttrack.app.data.remote.dto;

import java.util.List;
import java.util.UUID;

public class JourneyDto {
    private UUID id;
    private String serviceCode;
    private StationDto departureStation;
    private StationDto arrivalStation;
    private String scheduledDeparture;
    private String scheduledArrival;
    private String estimatedDeparture;
    private String estimatedArrival;
    private String status;
    private String platform;
    private String capacityLevel;
    private List<FareOptionDto> fareOptions;

    public JourneyDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }

    public StationDto getDepartureStation() { return departureStation; }
    public void setDepartureStation(StationDto departureStation) { this.departureStation = departureStation; }

    public StationDto getArrivalStation() { return arrivalStation; }
    public void setArrivalStation(StationDto arrivalStation) { this.arrivalStation = arrivalStation; }

    public String getScheduledDeparture() { return scheduledDeparture; }
    public void setScheduledDeparture(String scheduledDeparture) { this.scheduledDeparture = scheduledDeparture; }

    public String getScheduledArrival() { return scheduledArrival; }
    public void setScheduledArrival(String scheduledArrival) { this.scheduledArrival = scheduledArrival; }

    public String getEstimatedDeparture() { return estimatedDeparture; }
    public void setEstimatedDeparture(String estimatedDeparture) { this.estimatedDeparture = estimatedDeparture; }

    public String getEstimatedArrival() { return estimatedArrival; }
    public void setEstimatedArrival(String estimatedArrival) { this.estimatedArrival = estimatedArrival; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getCapacityLevel() { return capacityLevel; }
    public void setCapacityLevel(String capacityLevel) { this.capacityLevel = capacityLevel; }

    public List<FareOptionDto> getFareOptions() { return fareOptions; }
    public void setFareOptions(List<FareOptionDto> fareOptions) { this.fareOptions = fareOptions; }

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

        public FareOptionDto() {}

        public UUID getFareProductId() { return fareProductId; }
        public void setFareProductId(UUID fareProductId) { this.fareProductId = fareProductId; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getTravelClass() { return travelClass; }
        public void setTravelClass(String travelClass) { this.travelClass = travelClass; }

        public String getValidityType() { return validityType; }
        public void setValidityType(String validityType) { this.validityType = validityType; }

        public Integer getPriceMinor() { return priceMinor; }
        public void setPriceMinor(Integer priceMinor) { this.priceMinor = priceMinor; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public boolean isRefundable() { return isRefundable; }
        public void setRefundable(boolean refundable) { isRefundable = refundable; }

        public boolean isChangeable() { return isChangeable; }
        public void setChangeable(boolean changeable) { isChangeable = changeable; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
