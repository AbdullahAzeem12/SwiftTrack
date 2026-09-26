package com.swifttrack.app.data.remote.dto;

import java.util.UUID;

public class ServiceAlertDto {
    private UUID id;
    private String title;
    private String description;
    private String severity;
    private UUID affectedRouteId;
    private boolean isActive;
    private String startsAt;
    private String endsAt;

    public ServiceAlertDto() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public UUID getAffectedRouteId() { return affectedRouteId; }
    public void setAffectedRouteId(UUID affectedRouteId) { this.affectedRouteId = affectedRouteId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getStartsAt() { return startsAt; }
    public void setStartsAt(String startsAt) { this.startsAt = startsAt; }

    public String getEndsAt() { return endsAt; }
    public void setEndsAt(String endsAt) { this.endsAt = endsAt; }
}
