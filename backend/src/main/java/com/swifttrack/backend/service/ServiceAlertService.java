package com.swifttrack.backend.service;

import com.swifttrack.backend.domain.entity.ServiceAlert;
import com.swifttrack.backend.dto.ServiceAlertDto;
import com.swifttrack.backend.repository.ServiceAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceAlertService {

    private final ServiceAlertRepository serviceAlertRepository;

    public List<ServiceAlertDto> getActiveAlerts() {
        return serviceAlertRepository.findByIsActiveTrue().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ServiceAlertDto mapToDto(ServiceAlert a) {
        return ServiceAlertDto.builder()
                .id(a.getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .severity(a.getSeverity())
                .affectedRouteId(a.getAffectedRouteId())
                .isActive(a.isActive())
                .startsAt(a.getStartsAt())
                .endsAt(a.getEndsAt())
                .build();
    }
}
