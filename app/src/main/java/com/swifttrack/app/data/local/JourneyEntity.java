package com.swifttrack.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "journeys")
public class JourneyEntity {

    @PrimaryKey
    @NonNull
    public String id;
    public String serviceCode;
    public String originCode;
    public String originName;
    public String destCode;
    public String destName;
    public String scheduledDeparture;
    public String scheduledArrival;
    public String status;
    public String platform;

    public JourneyEntity(@NonNull String id, String serviceCode, String originCode, String originName, String destCode, String destName, String scheduledDeparture, String scheduledArrival, String status, String platform) {
        this.id = id;
        this.serviceCode = serviceCode;
        this.originCode = originCode;
        this.originName = originName;
        this.destCode = destCode;
        this.destName = destName;
        this.scheduledDeparture = scheduledDeparture;
        this.scheduledArrival = scheduledArrival;
        this.status = status;
        this.platform = platform;
    }
}
