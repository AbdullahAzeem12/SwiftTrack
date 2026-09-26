package com.swifttrack.app.core.util;

import android.util.Patterns;
import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s'\\.-]{3,50}$");

    public static boolean isValidFullName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String trimmed = name.trim();
        return trimmed.length() >= 3 && NAME_PATTERN.matcher(trimmed).matches();
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return PasswordStrengthUtil.isValidPassword(password);
    }

    public static boolean doPasswordsMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) return false;
        return password.equals(confirmPassword);
    }

    public static String sanitizeInput(String input) {
        if (input == null) return "";
        return input.trim()
                .replaceAll("<[^>]*>", "") // Prevent basic XSS HTML tags
                .replaceAll("['\"]", ""); // Sanitize raw quotes
    }
}
