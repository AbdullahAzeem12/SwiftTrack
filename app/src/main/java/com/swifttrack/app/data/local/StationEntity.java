package com.swifttrack.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "stations")
public class StationEntity {

    @PrimaryKey
    @NonNull
    public String id;
    public String code;
    public String name;
    public String city;
    public boolean isAirportTerminal;
    public String terminalCode;
    public Double latitude;
    public Double longitude;

    public StationEntity(@NonNull String id, String code, String name, String city, boolean isAirportTerminal, String terminalCode, Double latitude, Double longitude) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.city = city;
        this.isAirportTerminal = isAirportTerminal;
        this.terminalCode = terminalCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
