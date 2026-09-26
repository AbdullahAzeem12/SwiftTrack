package com.swifttrack.app.core.util;

import android.view.View;
import android.widget.TextView;
import com.swifttrack.app.R;

public class LoadingOverlayManager {

    private final View overlayView;
    private final TextView tvLoadingText;

    public LoadingOverlayManager(View overlayView) {
        this.overlayView = overlayView;
        if (overlayView != null) {
            this.tvLoadingText = overlayView.findViewById(R.id.tv_loading_text);
        } else {
            this.tvLoadingText = null;
        }
    }

    public void show(String message) {
        if (overlayView == null) return;
        if (tvLoadingText != null && message != null) {
            tvLoadingText.setText(message);
        }
        overlayView.setVisibility(View.VISIBLE);
        overlayView.bringToFront();
    }

    public void hide() {
        if (overlayView == null) return;
        overlayView.setVisibility(View.GONE);
    }

    public boolean isShowing() {
        return overlayView != null && overlayView.getVisibility() == View.VISIBLE;
    }
}
