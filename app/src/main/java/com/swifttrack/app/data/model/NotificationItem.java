package com.swifttrack.app.data.model;

public class NotificationItem {
    public String id;
    public String title;
    public String description;
    public String severity; // "GREEN", "AMBER", "RED", "BLUE", "PURPLE"
    public String category; // "TICKET", "TRAIN_ARRIVAL", "DISRUPTION", "SYSTEM"
    public String timestamp;
    public long timeMillis;
    public boolean isRead;
    public boolean isPinned;
    public String actionTarget; // Ticket code or screen route

    public NotificationItem(String id, String title, String description, String severity, String category, String timestamp, long timeMillis, boolean isRead, boolean isPinned, String actionTarget) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.category = category;
        this.timestamp = timestamp;
        this.timeMillis = timeMillis;
        this.isRead = isRead;
        this.isPinned = isPinned;
        this.actionTarget = actionTarget;
    }
}
