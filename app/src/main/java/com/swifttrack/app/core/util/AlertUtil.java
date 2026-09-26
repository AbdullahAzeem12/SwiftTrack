package com.swifttrack.app.core.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.android.material.snackbar.Snackbar;

public class AlertUtil {

    public enum AlertType {
        SUCCESS, ERROR, WARNING, INFO
    }

    public static void showSuccessSnackbar(View view, String message) {
        showAlertSnackbar(view, message, AlertType.SUCCESS);
    }

    public static void showErrorSnackbar(View view, String message) {
        showAlertSnackbar(view, message, AlertType.ERROR);
    }

    public static void showWarningSnackbar(View view, String message) {
        showAlertSnackbar(view, message, AlertType.WARNING);
    }

    public static void showInfoSnackbar(View view, String message) {
        showAlertSnackbar(view, message, AlertType.INFO);
    }

    private static void showAlertSnackbar(View view, String message, AlertType type) {
        if (view == null || view.getContext() == null) return;
        Context context = view.getContext();
        boolean isDark = isDarkMode(context);

        String startBg, endBg, strokeColor, textColor;

        switch (type) {
            case SUCCESS:
                if (isDark) {
                    startBg = "#0F172A";
                    endBg = "#1E293B";
                    strokeColor = "#10B981";
                    textColor = "#FFFFFF"; // Pure White in Dark Mode
                } else {
                    startBg = "#FFFFFF";
                    endBg = "#F0FDF4";
                    strokeColor = "#059669";
                    textColor = "#0F172A"; // Deep Charcoal Black in Light Mode
                }
                break;
            case ERROR:
                if (isDark) {
                    startBg = "#0F172A";
                    endBg = "#1E293B";
                    strokeColor = "#F43F5E";
                    textColor = "#FFFFFF";
                } else {
                    startBg = "#FFFFFF";
                    endBg = "#FFF1F2";
                    strokeColor = "#E11D48";
                    textColor = "#0F172A";
                }
                break;
            case WARNING:
                if (isDark) {
                    startBg = "#0F172A";
                    endBg = "#1E293B";
                    strokeColor = "#F59E0B";
                    textColor = "#FFFFFF";
                } else {
                    startBg = "#FFFFFF";
                    endBg = "#FFFBEB";
                    strokeColor = "#D97706";
                    textColor = "#0F172A";
                }
                break;
            case INFO:
            default:
                if (isDark) {
                    startBg = "#0F172A";
                    endBg = "#1E293B";
                    strokeColor = "#6366F1";
                    textColor = "#FFFFFF";
                } else {
                    startBg = "#FFFFFF";
                    endBg = "#EEF2FF";
                    strokeColor = "#4F46E5";
                    textColor = "#0F172A";
                }
                break;
        }

        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();

        // Strip default Material background tinting so custom gradient shape renders 100% pure
        snackbarView.setBackgroundTintList(null);

        // 1. Floating Margin Adjustment
        ViewGroup.LayoutParams layoutParams = snackbarView.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
            int marginHoriz = dpToPx(context, 16);
            int marginBottom = dpToPx(context, 24);
            marginParams.setMargins(marginHoriz, 0, marginHoriz, marginBottom);
            snackbarView.setLayoutParams(marginParams);
        }

        // 2. Dual-Mode Responsive Floating Card Background with Gradient & Border
        GradientDrawable shape = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor(startBg), Color.parseColor(endBg)}
        );
        shape.setCornerRadius(dpToPx(context, 16));
        shape.setStroke(dpToPx(context, 2.0f), Color.parseColor(strokeColor));
        snackbarView.setBackground(shape);

        // 3. Elevation & Padding
        snackbarView.setElevation(dpToPx(context, 10));
        snackbarView.setPadding(
                dpToPx(context, 16),
                dpToPx(context, 12),
                dpToPx(context, 16),
                dpToPx(context, 12)
        );

        // 4. High-Contrast Text Alignment, Size & Weight
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            textView.setTextColor(Color.parseColor(textColor));
            textView.setGravity(Gravity.CENTER_VERTICAL);
            textView.setMaxLines(3);
            textView.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            textView.setCompoundDrawablePadding(dpToPx(context, 10));
        }

        snackbar.show();
    }

    private static boolean isDarkMode(Context context) {
        try {
            com.swifttrack.app.core.security.KeystoreManager km = new com.swifttrack.app.core.security.KeystoreManager(context);
            int mode = km.getNightMode();
            if (mode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) return true;
            if (mode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) return false;
        } catch (Exception ignored) {}

        int appCompatMode = androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
        if (appCompatMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) return true;
        if (appCompatMode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) return false;

        int currentNightMode = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private static int dpToPx(Context context, float dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
