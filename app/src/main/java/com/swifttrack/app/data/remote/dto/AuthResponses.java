package com.swifttrack.app.data.remote.dto;

import java.util.UUID;

public class AuthResponses {

    public static class JwtResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private UserDto user;

        public JwtResponse() {}
        public JwtResponse(String accessToken, String refreshToken, String tokenType, UserDto user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.tokenType = tokenType;
            this.user = user;
        }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }

        public UserDto getUser() { return user; }
        public void setUser(UserDto user) { this.user = user; }
    }

    public static class UserDto {
        private UUID id;
        private String email;
        private String fullName;
        private String phoneNumber;
        private String role;
        private boolean emailVerified;

        public UserDto() {}
        public UserDto(UUID id, String email, String fullName, String phoneNumber, String role, boolean emailVerified) {
            this.id = id;
            this.email = email;
            this.fullName = fullName;
            this.phoneNumber = phoneNumber;
            this.role = role;
            this.emailVerified = emailVerified;
        }

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public boolean isEmailVerified() { return emailVerified; }
        public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    }
}
