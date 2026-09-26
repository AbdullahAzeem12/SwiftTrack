package com.swifttrack.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationDto {
    private UUID id;
    private String code;
    private String name;
    private String city;
    private boolean isAirportTerminal;
    private String terminalCode;
    private Double latitude;
    private Double longitude;
}
