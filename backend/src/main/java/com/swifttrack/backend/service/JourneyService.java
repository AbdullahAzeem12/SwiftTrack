package com.swifttrack.backend.service;

import com.swifttrack.backend.domain.entity.FareProduct;
import com.swifttrack.backend.domain.entity.Journey;
import com.swifttrack.backend.dto.JourneyDto;
import com.swifttrack.backend.exception.ApiException;
import com.swifttrack.backend.repository.FareProductRepository;
import com.swifttrack.backend.repository.JourneyRepository;
import com.swifttrack.backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JourneyService {

    private final JourneyRepository journeyRepository;
    private final StationRepository stationRepository;
    private final FareProductRepository fareProductRepository;
    private final StationService stationService;

    public List<JourneyDto> searchJourneys(UUID originId, UUID destinationId, ZonedDateTime departureTime) {
        if (!stationRepository.existsById(originId) || !stationRepository.existsById(destinationId)) {
            throw new ApiException("STATION_NOT_FOUND", "Origin or Destination station not found", HttpStatus.NOT_FOUND);
        }

        ZonedDateTime searchAfter = departureTime != null ? departureTime : ZonedDateTime.now().minusHours(1);
        List<Journey> journeys = journeyRepository.findAvailableJourneys(originId, destinationId, searchAfter);

        List<FareProduct> fares = fareProductRepository.findAll();

        return journeys.stream()
                .map(j -> mapToDto(j, fares))
                .collect(Collectors.toList());
    }

    public JourneyDto getJourneyDetails(UUID journeyId) {
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ApiException("JOURNEY_NOT_FOUND", "Journey not found", HttpStatus.NOT_FOUND));

        List<FareProduct> fares = fareProductRepository.findAll();
        return mapToDto(journey, fares);
    }

    public JourneyDto mapToDto(Journey j, List<FareProduct> fares) {
        List<JourneyDto.FareOptionDto> fareDtos = fares.stream().map(f ->
                JourneyDto.FareOptionDto.builder()
                        .fareProductId(f.getId())
                        .code(f.getCode())
                        .name(f.getName())
                        .travelClass(f.getTravelClass().name())
                        .validityType(f.getValidityType())
                        .priceMinor(f.getBasePriceMinor())
                        .currency(f.getCurrency())
                        .isRefundable(f.isRefundable())
                        .isChangeable(f.isChangeable())
                        .description(f.getDescription())
                        .build()
        ).collect(Collectors.toList());

        return JourneyDto.builder()
                .id(j.getId())
                .serviceCode(j.getServiceCode())
                .departureStation(stationService.mapToDto(j.getDepartureStation()))
                .arrivalStation(stationService.mapToDto(j.getArrivalStation()))
                .scheduledDeparture(j.getScheduledDeparture())
                .scheduledArrival(j.getScheduledArrival())
                .estimatedDeparture(j.getEstimatedDeparture() != null ? j.getEstimatedDeparture() : j.getScheduledDeparture())
                .estimatedArrival(j.getEstimatedArrival() != null ? j.getEstimatedArrival() : j.getScheduledArrival())
                .status(j.getStatus())
                .platform(j.getPlatform() != null ? j.getPlatform() : "Platform TBA")
                .capacityLevel(j.getCapacityLevel())
                .fareOptions(fareDtos)
                .build();
    }
}
