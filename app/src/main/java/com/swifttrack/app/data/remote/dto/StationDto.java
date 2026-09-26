package com.swifttrack.app.data.remote.dto;

import java.util.UUID;

public class StationDto {
    private UUID id;
    private String code;
    private String name;
    private String city;
    private boolean isAirportTerminal;
    private String terminalCode;
    private Double latitude;
    private Double longitude;

    public StationDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public boolean isAirportTerminal() { return isAirportTerminal; }
    public void setAirportTerminal(boolean airportTerminal) { isAirportTerminal = airportTerminal; }

    public String getTerminalCode() { return terminalCode; }
    public void setTerminalCode(String terminalCode) { this.terminalCode = terminalCode; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
