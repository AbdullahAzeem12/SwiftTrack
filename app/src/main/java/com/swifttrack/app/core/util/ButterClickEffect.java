package com.swifttrack.app.core.util;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class ButterClickEffect {

    /**
     * Applies a smooth micro-interaction scale/jelly bounce effect when the view is pressed/tapped.
     */
    public static void apply(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .setDuration(90)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator(2.2f))
                            .start();
                    break;
            }
            return false;
        });
    }

    public static void applyAll(View... views) {
        if (views == null) return;
        for (View v : views) {
            if (v != null) {
                apply(v);
            }
        }
    }
}
