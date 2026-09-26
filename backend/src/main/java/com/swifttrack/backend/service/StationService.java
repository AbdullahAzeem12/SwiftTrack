package com.swifttrack.backend.service;

import com.swifttrack.backend.domain.entity.Station;
import com.swifttrack.backend.dto.StationDto;
import com.swifttrack.backend.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;

    public List<StationDto> getAllActiveStations() {
        return stationRepository.findByIsActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<StationDto> searchStations(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllActiveStations();
        }
        return stationRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(query.trim(), query.trim())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public StationDto mapToDto(Station s) {
        return StationDto.builder()
                .id(s.getId())
                .code(s.getCode())
                .name(s.getName())
                .city(s.getCity())
                .isAirportTerminal(s.isAirportTerminal())
                .terminalCode(s.getTerminalCode())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .build();
    }
}
