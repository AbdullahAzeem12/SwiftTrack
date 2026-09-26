package com.swifttrack.app.data.remote.dto;

import java.util.HashMap;
import java.util.Map;

public class UserProfileDto {

    private String uid;
    private String fullName;
    private String email;
    private String phone;
    private long createdDate;
    private long updatedAt;
    private long lastLogin;
    private String profileImage;
    private String role;
    private String accountStatus; // e.g. "ACTIVE", "DISABLED"
    private boolean rememberMe;
    private String deviceToken;
    private boolean notificationEnabled;
    private String verificationStatus; // e.g. "VERIFIED", "UNVERIFIED"

    public UserProfileDto() {
        // Default constructor required for Firestore calls
    }

    public UserProfileDto(String uid, String fullName, String email, String phone,
                          long createdDate, long updatedAt, long lastLogin,
                          String profileImage, String role, String accountStatus,
                          boolean rememberMe, String deviceToken,
                          boolean notificationEnabled, String verificationStatus) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.createdDate = createdDate;
        this.updatedAt = updatedAt;
        this.lastLogin = lastLogin;
        this.profileImage = profileImage;
        this.role = role != null ? role : "CUSTOMER";
        this.accountStatus = accountStatus != null ? accountStatus : "ACTIVE";
        this.rememberMe = rememberMe;
        this.deviceToken = deviceToken != null ? deviceToken : "";
        this.notificationEnabled = notificationEnabled;
        this.verificationStatus = verificationStatus != null ? verificationStatus : "UNVERIFIED";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("fullName", fullName);
        map.put("email", email);
        map.put("phone", phone);
        map.put("createdDate", createdDate);
        map.put("createdAt", createdDate);
        map.put("updatedAt", updatedAt);
        map.put("lastLogin", lastLogin);
        map.put("profileImage", profileImage);
        map.put("role", role);
        map.put("accountStatus", accountStatus);
        map.put("status", accountStatus);
        map.put("rememberMe", rememberMe);
        map.put("deviceToken", deviceToken);
        map.put("notificationEnabled", notificationEnabled);
        map.put("verificationStatus", verificationStatus);
        return map;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public long getCreatedDate() { return createdDate; }
    public void setCreatedDate(long createdDate) { this.createdDate = createdDate; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getLastLogin() { return lastLogin; }
    public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public boolean isRememberMe() { return rememberMe; }
    public void setRememberMe(boolean rememberMe) { this.rememberMe = rememberMe; }

    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }

    public boolean isNotificationEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
}
