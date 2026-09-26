package com.swifttrack.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alerts")
public class AlertEntity {

    @PrimaryKey
    @NonNull
    public String id;
    public String title;
    public String description;
    public String severity;
    public boolean isActive;

    public AlertEntity(@NonNull String id, String title, String description, String severity, boolean isActive) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.isActive = isActive;
    }
}
