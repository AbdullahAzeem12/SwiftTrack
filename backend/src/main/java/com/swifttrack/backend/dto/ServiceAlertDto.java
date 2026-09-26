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
public class ServiceAlertDto {
    private UUID id;
    private String title;
    private String description;
    private String severity;
    private UUID affectedRouteId;
    private boolean isActive;
    private ZonedDateTime startsAt;
    private ZonedDateTime endsAt;
}
