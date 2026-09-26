package com.swifttrack.app.core.util;

import android.graphics.Color;
import java.util.regex.Pattern;

public class PasswordStrengthUtil {

    public enum StrengthLevel {
        EMPTY("", 0, Color.TRANSPARENT, 0),
        VERY_WEAK("Very Weak", 1, Color.parseColor("#EF4444"), 20),
        WEAK("Weak", 2, Color.parseColor("#F97316"), 40),
        MEDIUM("Medium", 3, Color.parseColor("#F59E0B"), 60),
        STRONG("Strong", 4, Color.parseColor("#10B981"), 80),
        VERY_STRONG("Very Strong", 5, Color.parseColor("#059669"), 100);

        private final String label;
        private final int score;
        private final int color;
        private final int progressPercentage;

        StrengthLevel(String label, int score, int color, int progressPercentage) {
            this.label = label;
            this.score = score;
            this.color = color;
            this.progressPercentage = progressPercentage;
        }

        public String getLabel() {
            return label;
        }

        public int getScore() {
            return score;
        }

        public int getColor() {
            return color;
        }

        public int getProgressPercentage() {
            return progressPercentage;
        }
    }

    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_NUMBER = Pattern.compile("[0-9]");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    public static StrengthLevel calculateStrength(String password) {
        if (password == null || password.isEmpty()) {
            return StrengthLevel.EMPTY;
        }

        int score = 0;

        if (password.length() >= 8) {
            score++;
        }
        if (HAS_UPPERCASE.matcher(password).find()) {
            score++;
        }
        if (HAS_LOWERCASE.matcher(password).find()) {
            score++;
        }
        if (HAS_NUMBER.matcher(password).find()) {
            score++;
        }
        if (HAS_SPECIAL.matcher(password).find()) {
            score++;
        }

        switch (score) {
            case 0:
            case 1:
                return StrengthLevel.VERY_WEAK;
            case 2:
                return StrengthLevel.WEAK;
            case 3:
                return StrengthLevel.MEDIUM;
            case 4:
                return StrengthLevel.STRONG;
            case 5:
                return StrengthLevel.VERY_STRONG;
            default:
                return StrengthLevel.VERY_WEAK;
        }
    }

    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.length() >= 8 &&
                HAS_UPPERCASE.matcher(password).find() &&
                HAS_LOWERCASE.matcher(password).find() &&
                HAS_NUMBER.matcher(password).find() &&
                HAS_SPECIAL.matcher(password).find();
    }
}
