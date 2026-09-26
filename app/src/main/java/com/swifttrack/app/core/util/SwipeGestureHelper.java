package com.swifttrack.app.core.util;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class SwipeGestureHelper implements View.OnTouchListener {

    public interface OnSwipeListener {
        void onSwipeRight(); // Swipe from left to right (Navigate Back)
        void onSwipeLeft();  // Swipe from right to left (Navigate Forward)
    }

    private final GestureDetector gestureDetector;
    private final OnSwipeListener listener;

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 180;

    public SwipeGestureHelper(Context context, @NonNull OnSwipeListener listener) {
        this.listener = listener;
        this.gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            listener.onSwipeRight();
                        } else {
                            listener.onSwipeLeft();
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    public static void attachSwipeBack(View view, Runnable onSwipeBack) {
        if (view == null || onSwipeBack == null) return;
        SwipeGestureHelper helper = new SwipeGestureHelper(view.getContext(), new OnSwipeListener() {
            @Override
            public void onSwipeRight() {
                onSwipeBack.run();
            }

            @Override
            public void onSwipeLeft() {}
        });
        view.setOnTouchListener((v, event) -> {
            boolean handled = helper.onTouch(v, event);
            return handled;
        });
    }
}
