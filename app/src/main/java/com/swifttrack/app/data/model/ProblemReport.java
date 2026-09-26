package com.swifttrack.app.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class ProblemReport {
    private String reportId;
    private String userId;
    private String userEmail;
    private String userName;
    private String category;
    private String subject;
    private String description;
    private String screenName;
    private String bookingId;
    private String appVersion;
    private String androidVersion;
    private String deviceModel;
    @ServerTimestamp
    private Date createdAt;
    private long createdAtTimestamp;
    private String status; // NEW, INVESTIGATING, RESOLVED, CLOSED
    private String emailNotificationStatus; // PENDING, SENT, FAILED

    public ProblemReport() {}

    public ProblemReport(String reportId, String userId, String userEmail, String userName,
                         String category, String subject, String description, String screenName,
                         String bookingId, String appVersion, String androidVersion,
                         String deviceModel, long createdAtTimestamp) {
        this.reportId = reportId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.category = category;
        this.subject = subject;
        this.description = description;
        this.screenName = screenName;
        this.bookingId = bookingId;
        this.appVersion = appVersion;
        this.androidVersion = androidVersion;
        this.deviceModel = deviceModel;
        this.createdAtTimestamp = createdAtTimestamp;
        this.status = "NEW";
        this.emailNotificationStatus = "PENDING";
    }

    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

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

    public String getScreenName() { return screenName; }
    public void setScreenName(String screenName) { this.screenName = screenName; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getAndroidVersion() { return androidVersion; }
    public void setAndroidVersion(String androidVersion) { this.androidVersion = androidVersion; }

    public String getDeviceModel() { return deviceModel; }
    public void setDeviceModel(String deviceModel) { this.deviceModel = deviceModel; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public long getCreatedAtTimestamp() { return createdAtTimestamp; }
    public void setCreatedAtTimestamp(long createdAtTimestamp) { this.createdAtTimestamp = createdAtTimestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEmailNotificationStatus() { return emailNotificationStatus; }
    public void setEmailNotificationStatus(String emailNotificationStatus) { this.emailNotificationStatus = emailNotificationStatus; }
}
