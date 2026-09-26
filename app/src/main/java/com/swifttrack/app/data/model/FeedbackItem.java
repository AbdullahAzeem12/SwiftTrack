package com.swifttrack.app.data.model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class FeedbackItem {
    private String feedbackId;
    private String userId;
    private String userEmail;
    private String userName;
    private float rating;
    private String category;
    private String message;
    private String recommend;
    private String appVersion;
    private String platform;
    @ServerTimestamp
    private Date createdAt;
    private long createdAtTimestamp;
    private String status; // NEW, REVIEWED, RESPONDED, CLOSED
    private String emailNotificationStatus; // PENDING, SENT, FAILED

    public FeedbackItem() {}

    public FeedbackItem(String feedbackId, String userId, String userEmail, String userName,
                        float rating, String category, String message, String recommend,
                        String appVersion, String platform, long createdAtTimestamp) {
        this.feedbackId = feedbackId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
        this.rating = rating;
        this.category = category;
        this.message = message;
        this.recommend = recommend;
        this.appVersion = appVersion;
        this.platform = platform;
        this.createdAtTimestamp = createdAtTimestamp;
        this.status = "NEW";
        this.emailNotificationStatus = "PENDING";
    }

    // Getters and Setters
    public String getFeedbackId() { return feedbackId; }
    public void setFeedbackId(String feedbackId) { this.feedbackId = feedbackId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRecommend() { return recommend; }
    public void setRecommend(String recommend) { this.recommend = recommend; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public long getCreatedAtTimestamp() { return createdAtTimestamp; }
    public void setCreatedAtTimestamp(long createdAtTimestamp) { this.createdAtTimestamp = createdAtTimestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEmailNotificationStatus() { return emailNotificationStatus; }
    public void setEmailNotificationStatus(String emailNotificationStatus) { this.emailNotificationStatus = emailNotificationStatus; }
}
