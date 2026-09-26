package com.swifttrack.app.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class SupportTicket {
    private String ticketId;
    private String userId;
    private String userEmail;
    private String userName;
    private String category;
    private String subject;
    private String description;
    private String bookingId;
    private String journeyId;
    private String priority; // NORMAL, HIGH, URGENT
    private String status;   // OPEN, IN_PROGRESS, WAITING_FOR_USER, RESOLVED, CLOSED
    @ServerTimestamp
    private Date createdAt;
    private long createdAtTimestamp;
    private long updatedAtTimestamp;
    private String appVersion;
    private String platform;
    private String emailNotificationStatus; // PENDING, SENT, FAILED

    public SupportTicket() {}

    public SupportTicket(String ticketId, String userId, String userEmail, String userName,
                         String category, String subject, String description, String bookingId,
                         String appVersion, String platform, long createdAtTimestamp) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.category = category;
        this.subject = subject;
        this.description = description;
        this.bookingId = bookingId;
        this.priority = "NORMAL";
        this.status = "OPEN";
        this.appVersion = appVersion;
        this.platform = platform;
        this.createdAtTimestamp = createdAtTimestamp;
        this.updatedAtTimestamp = createdAtTimestamp;
        this.emailNotificationStatus = "PENDING";
    }

    // Getters and Setters
    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getJourneyId() { return journeyId; }
    public void setJourneyId(String journeyId) { this.journeyId = journeyId; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public long getCreatedAtTimestamp() { return createdAtTimestamp; }
    public void setCreatedAtTimestamp(long createdAtTimestamp) { this.createdAtTimestamp = createdAtTimestamp; }

    public long getUpdatedAtTimestamp() { return updatedAtTimestamp; }
    public void setUpdatedAtTimestamp(long updatedAtTimestamp) { this.updatedAtTimestamp = updatedAtTimestamp; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getEmailNotificationStatus() { return emailNotificationStatus; }
    public void setEmailNotificationStatus(String emailNotificationStatus) { this.emailNotificationStatus = emailNotificationStatus; }
}
