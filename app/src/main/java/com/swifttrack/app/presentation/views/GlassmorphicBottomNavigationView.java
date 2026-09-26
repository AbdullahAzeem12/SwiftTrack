package com.swifttrack.app.presentation.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.viewpager2.widget.ViewPager2;
import com.swifttrack.app.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Ultra-Premium Floating Glassmorphism Navigation Bar
 * Features:
 * - Centered 64dp height floating glass dock with rounded corners and neon stroke
 * - Perfect vertical & horizontal centering for icons and glowing selection pill
 * - Continuous realtime breathing animation (pulse) for all tab icons
 * - Morphing outline-to-filled icons with 122%-130% scale animation
 * - Smooth color gradient transitions
 * - Ripple & haptic touch feedback
 * - Notification badges with bounce & pulse animations
 * - Seamless NavController integration and state preservation
 */
public class GlassmorphicBottomNavigationView extends FrameLayout {

    public static class TabItem {
        public final int id;
        public final String title;
        public final int outlineIconRes;
        public final int filledIconRes;

        public TabItem(int id, String title, int outlineIconRes, int filledIconRes) {
            this.id = id;
            this.title = title;
            this.outlineIconRes = outlineIconRes;
            this.filledIconRes = filledIconRes;
        }
    }

    private static final String STATE_SUPER = "state_super";
    private static final String STATE_SELECTED_ID = "state_selected_id";

    private final List<TabItem> tabItems = new ArrayList<>();
    private final List<View> itemViews = new ArrayList<>();
    private final List<ImageView> iconViews = new ArrayList<>();
    private final List<TextView> labelViews = new ArrayList<>();
    private final List<TextView> badgeViews = new ArrayList<>();

    private LinearLayout itemsContainer;
    private View slidingPillIndicator;

    private int selectedPosition = 0;
    private int selectedId = R.id.homeBookFragment;
    private ValueAnimator pillAnimator;
    private ValueAnimator breathingAnimator;
    private float breathingFraction = 0f;

    private NavController navController;
    private boolean isNavigatingInternal = false;
    private boolean isHidden = false;

    public GlassmorphicBottomNavigationView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public GlassmorphicBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GlassmorphicBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setClipToPadding(false);
        setClipChildren(false);



        // Populate default SwiftTrack tabs
        tabItems.add(new TabItem(R.id.homeBookFragment, "Book", R.drawable.ic_nav_book_outline, R.drawable.ic_nav_book_filled));
        tabItems.add(new TabItem(R.id.ticketsListFragment, "Tickets", R.drawable.ic_nav_tickets_outline, R.drawable.ic_nav_tickets_filled));
        tabItems.add(new TabItem(R.id.timetableFragment, "Timetable", R.drawable.ic_nav_timetable_outline, R.drawable.ic_nav_timetable_filled));
        tabItems.add(new TabItem(R.id.alertsFragment, "Alerts", R.drawable.ic_nav_alerts_outline, R.drawable.ic_nav_alerts_filled));
        tabItems.add(new TabItem(R.id.accountFragment, "Account", R.drawable.ic_nav_account_outline, R.drawable.ic_nav_account_filled));

        // Sliding pill selection indicator
        slidingPillIndicator = new View(context);
        slidingPillIndicator.setBackgroundResource(R.drawable.bg_sliding_pill);
        LayoutParams pillParams = new LayoutParams(0, 0);
        pillParams.gravity = Gravity.START | Gravity.TOP;
        addView(slidingPillIndicator, pillParams);

        // Container for item layouts
        itemsContainer = new LinearLayout(context);
        itemsContainer.setOrientation(LinearLayout.HORIZONTAL);
        itemsContainer.setWeightSum(tabItems.size());
        LayoutParams containerParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(itemsContainer, containerParams);

        buildTabViews(context);
        startBreathingAnimation();
    }

    private void buildTabViews(Context context) {
        itemsContainer.removeAllViews();
        itemViews.clear();
        iconViews.clear();
        labelViews.clear();
        badgeViews.clear();

        for (int i = 0; i < tabItems.size(); i++) {
            final int index = i;
            final TabItem item = tabItems.get(i);

            FrameLayout itemView = new FrameLayout(context);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);
            itemView.setLayoutParams(itemParams);
            itemView.setClickable(true);
            itemView.setFocusable(true);

            // Touch ripple effect
            int[] attrs = new int[]{android.R.attr.selectableItemBackgroundBorderless};
            android.content.res.TypedArray ta = context.obtainStyledAttributes(attrs);
            itemView.setBackground(ta.getDrawable(0));
            ta.recycle();

            // Vertical content layout centered perfectly around icon and label
            LinearLayout contentLayout = new LinearLayout(context);
            contentLayout.setOrientation(LinearLayout.VERTICAL);
            contentLayout.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            contentParams.gravity = Gravity.CENTER;

            // Icon ImageView
            ImageView iconView = new ImageView(context);
            int iconSize = dipToPx(context, 22);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            iconParams.gravity = Gravity.CENTER_HORIZONTAL;
            iconView.setLayoutParams(iconParams);
            iconView.setImageResource(item.outlineIconRes);

            // Label TextView
            TextView labelView = new TextView(context);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = dipToPx(context, 3);
            labelParams.gravity = Gravity.CENTER_HORIZONTAL;
            labelView.setLayoutParams(labelParams);
            labelView.setText(item.title);
            labelView.setTextSize(10.5f);
            labelView.setMaxLines(1);
            labelView.setGravity(Gravity.CENTER);

            contentLayout.addView(iconView);
            contentLayout.addView(labelView);
            itemView.addView(contentLayout, contentParams);

            // Badge view overlay
            TextView badgeView = new TextView(context);
            int badgeSize = dipToPx(context, 16);
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, badgeSize);
            badgeParams.gravity = Gravity.TOP | Gravity.END;
            badgeParams.setMarginEnd(dipToPx(context, 6));
            badgeParams.rightMargin = dipToPx(context, 6);
            badgeParams.topMargin = dipToPx(context, 2);
            badgeView.setLayoutParams(badgeParams);
            badgeView.setBackgroundResource(R.drawable.bg_badge_pulse);
            badgeView.setTextColor(ContextCompat.getColor(context, R.color.nav_badge_text));
            badgeView.setTextSize(9.5f);
            badgeView.setPadding(dipToPx(context, 4), 0, dipToPx(context, 4), 0);
            badgeView.setGravity(Gravity.CENTER);
            badgeView.setVisibility(GONE);

            itemView.addView(badgeView);

            // Save references
            itemViews.add(itemView);
            iconViews.add(iconView);
            labelViews.add(labelView);
            badgeViews.add(badgeView);
            itemsContainer.addView(itemView);

            itemView.setOnClickListener(v -> {
                itemView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                itemView.playSoundEffect(SoundEffectConstants.CLICK);
                setSelectedTabPosition(index, true);
            });
        }

        post(() -> updatePillAndItems(false));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultHeight = dipToPx(getContext(), 64);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int finalHeight;
        if (heightMode == MeasureSpec.EXACTLY) {
            finalHeight = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            finalHeight = Math.min(defaultHeight, heightSize);
        } else {
            finalHeight = defaultHeight;
        }

        int exactHeightSpec = MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, exactHeightSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            updatePillAndItems(false);
        }
    }

    /**
     * Continuous Realtime Breathing Animation for All Icons (Faster & More Intense Opacity & Scale Pulse)
     */
    private void startBreathingAnimation() {
        if (breathingAnimator != null && breathingAnimator.isRunning()) return;
        breathingAnimator = ValueAnimator.ofFloat(0f, 1f);
        breathingAnimator.setDuration(850);
        breathingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathingAnimator.setRepeatMode(ValueAnimator.REVERSE);
        breathingAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        breathingAnimator.addUpdateListener(animation -> {
            breathingFraction = (float) animation.getAnimatedValue();
            applyBreathingToIcons();
        });
        breathingAnimator.start();
    }

    private void stopBreathingAnimation() {
        if (breathingAnimator != null) {
            breathingAnimator.cancel();
            breathingAnimator = null;
        }
    }

    private void applyBreathingToIcons() {
        for (int i = 0; i < iconViews.size(); i++) {
            ImageView iconView = iconViews.get(i);
            boolean isSelected = (i == selectedPosition);

            // Dynamic scale breathing
            float baseScale = isSelected ? 1.20f : 1.00f;
            float breathDelta = isSelected ? (0.16f * breathingFraction) : (0.12f * breathingFraction);
            float currentScale = baseScale + breathDelta;
            iconView.setScaleX(currentScale);
            iconView.setScaleY(currentScale);

            // Dynamic opacity / alpha breathing
            float baseAlpha = isSelected ? 0.85f : 0.65f;
            float alphaDelta = isSelected ? (0.15f * breathingFraction) : (0.35f * breathingFraction);
            iconView.setAlpha(baseAlpha + alphaDelta);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startBreathingAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopBreathingAnimation();
    }

    private ViewPager2 viewPager;

    /**
     * Connect to ViewPager2 for 1:1 real-time finger swipe indicator tracking
     */
    public void setupWithViewPager2(@NonNull ViewPager2 vp) {
        this.viewPager = vp;
        vp.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (pillAnimator == null || !pillAnimator.isRunning()) {
                    updatePillPositionForSwipe(position, positionOffset);
                }
            }

            @Override
            public void onPageSelected(int position) {
                setSelectedTabPosition(position, false);
            }
        });
    }

    private void updatePillPositionForSwipe(int position, float positionOffset) {
        if (itemViews.isEmpty() || position < 0 || position >= itemViews.size()) return;

        int pillInsetVert = dipToPx(getContext(), 2);
        int pillInsetHoriz = dipToPx(getContext(), 3);

        View currentView = itemViews.get(position);
        View nextView = (position + 1 < itemViews.size()) ? itemViews.get(position + 1) : currentView;

        int startLeft = currentView.getLeft() + itemsContainer.getLeft() + pillInsetHoriz;
        int endLeft = nextView.getLeft() + itemsContainer.getLeft() + pillInsetHoriz;
        int currentLeft = (int) (startLeft + (endLeft - startLeft) * positionOffset);

        int startWidth = currentView.getWidth() - (pillInsetHoriz * 2);
        int endWidth = nextView.getWidth() - (pillInsetHoriz * 2);
        int currentWidth = (int) (startWidth + (endWidth - startWidth) * positionOffset);

        int containerH = itemsContainer.getHeight() > 0 ? itemsContainer.getHeight() : (getHeight() - getPaddingTop() - getPaddingBottom());
        int targetHeight = Math.max(0, containerH - (pillInsetVert * 2));
        int targetTop = (itemsContainer.getTop() > 0 ? itemsContainer.getTop() : getPaddingTop()) + pillInsetVert;

        if (currentWidth <= 0) return;

        LayoutParams params = (LayoutParams) slidingPillIndicator.getLayoutParams();
        params.leftMargin = currentLeft;
        params.width = currentWidth;
        params.topMargin = targetTop;
        params.height = targetHeight;
        slidingPillIndicator.setLayoutParams(params);
    }

    /**
     * Set active tab by position
     */
    public void setSelectedTabPosition(int position, boolean animate) {
        if (position < 0 || position >= tabItems.size()) return;

        selectedPosition = position;
        selectedId = tabItems.get(position).id;

        updatePillAndItems(animate);

        if (viewPager != null && viewPager.getCurrentItem() != position) {
            viewPager.setCurrentItem(position, animate);
        }

        if (navController != null && !isNavigatingInternal) {
            isNavigatingInternal = true;
            try {
                NavOptions options = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(true)
                        .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                        .build();
                navController.navigate(selectedId, null, options);
            } catch (Exception ignored) {
            }
            isNavigatingInternal = false;
        }
    }

    /**
     * Set active tab by MenuItem ID
     */
    public void setSelectedItemId(int id, boolean animate) {
        for (int i = 0; i < tabItems.size(); i++) {
            if (tabItems.get(i).id == id) {
                setSelectedTabPosition(i, animate);
                break;
            }
        }
    }

    private void updatePillAndItems(boolean animate) {
        if (itemViews.isEmpty() || selectedPosition >= itemViews.size()) return;

        View activeView = itemViews.get(selectedPosition);

        int pillInsetVert = dipToPx(getContext(), 2);
        int pillInsetHoriz = dipToPx(getContext(), 3);

        int targetLeft = activeView.getLeft() + itemsContainer.getLeft() + pillInsetHoriz;
        int targetWidth = activeView.getWidth() - (pillInsetHoriz * 2);
        int containerH = itemsContainer.getHeight() > 0 ? itemsContainer.getHeight() : (getHeight() - getPaddingTop() - getPaddingBottom());
        int targetHeight = Math.max(0, containerH - (pillInsetVert * 2));
        int targetTop = (itemsContainer.getTop() > 0 ? itemsContainer.getTop() : getPaddingTop()) + pillInsetVert;

        if (targetWidth <= 0) return;

        // Ensure pill height & top margin match itemsContainer centered
        LayoutParams pillParams = (LayoutParams) slidingPillIndicator.getLayoutParams();
        pillParams.topMargin = targetTop;
        pillParams.height = targetHeight;
        slidingPillIndicator.setLayoutParams(pillParams);

        // Animate sliding pill indicator horizontally
        if (animate && pillAnimator != null && pillAnimator.isRunning()) {
            pillAnimator.cancel();
        }

        final int startLeft = slidingPillIndicator.getLeft();
        final int startWidth = slidingPillIndicator.getWidth();

        if (animate && (startLeft != targetLeft || startWidth != targetWidth)) {
            pillAnimator = ValueAnimator.ofFloat(0f, 1f);
            pillAnimator.setDuration(280);
            pillAnimator.setInterpolator(new OvershootInterpolator(1.12f));
            pillAnimator.addUpdateListener(animation -> {
                float fraction = animation.getAnimatedFraction();
                int currentLeft = (int) (startLeft + (targetLeft - startLeft) * fraction);
                int currentWidth = (int) (startWidth + (targetWidth - startWidth) * fraction);

                LayoutParams params = (LayoutParams) slidingPillIndicator.getLayoutParams();
                params.leftMargin = currentLeft;
                params.width = currentWidth;
                slidingPillIndicator.setLayoutParams(params);
            });
            pillAnimator.start();
        } else {
            LayoutParams params = (LayoutParams) slidingPillIndicator.getLayoutParams();
            params.leftMargin = targetLeft;
            params.width = targetWidth;
            slidingPillIndicator.setLayoutParams(params);
        }

        // Animate items (morphing icons, gradient colors)
        int activeColor = ContextCompat.getColor(getContext(), R.color.nav_item_active);
        int inactiveColor = ContextCompat.getColor(getContext(), R.color.nav_item_inactive);

        for (int i = 0; i < tabItems.size(); i++) {
            boolean isSelected = (i == selectedPosition);
            ImageView iconView = iconViews.get(i);
            TextView labelView = labelViews.get(i);
            TabItem item = tabItems.get(i);

            // Morph icon drawables
            iconView.setImageResource(isSelected ? item.filledIconRes : item.outlineIconRes);
            int targetColor = isSelected ? activeColor : inactiveColor;

            if (animate) {
                animateViewTextColor(labelView, targetColor);
                ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(targetColor));
            } else {
                labelView.setTextColor(targetColor);
                ImageViewCompat.setImageTintList(iconView, ColorStateList.valueOf(targetColor));
            }
        }

        applyBreathingToIcons();
    }

    private void animateViewTextColor(TextView textView, int targetColor) {
        int currentColor = textView.getCurrentTextColor();
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), currentColor, targetColor);
        colorAnim.setDuration(240);
        colorAnim.addUpdateListener(animator -> textView.setTextColor((int) animator.getAnimatedValue()));
        colorAnim.start();
    }

    /**
     * Connect to Jetpack NavController for synchronized tab selection
     */
    public void setupWithNavController(@NonNull NavController controller) {
        this.navController = controller;
        controller.addOnDestinationChangedListener((cntrl, destination, args) -> {
            if (isNavigatingInternal) return;
            int destId = destination.getId();
            for (int i = 0; i < tabItems.size(); i++) {
                if (tabItems.get(i).id == destId) {
                    setSelectedTabPosition(i, true);
                    break;
                }
            }
        });
    }

    /**
     * Set badge count on a specific tab with bounce & pulse animation
     */
    public void setBadge(int tabId, int count) {
        for (int i = 0; i < tabItems.size(); i++) {
            if (tabItems.get(i).id == tabId) {
                TextView badgeView = badgeViews.get(i);
                if (count > 0) {
                    badgeView.setText(count > 99 ? "99+" : String.valueOf(count));
                    if (badgeView.getVisibility() != VISIBLE) {
                        badgeView.setVisibility(VISIBLE);
                        badgeView.setScaleX(0f);
                        badgeView.setScaleY(0f);
                        badgeView.animate()
                                .scaleX(1.2f)
                                .scaleY(1.2f)
                                .setDuration(200)
                                .withEndAction(() -> badgeView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                                .start();
                    }
                } else {
                    badgeView.setVisibility(GONE);
                }
                break;
            }
        }
    }

    /**
     * Smoothly hide navigation dock on scroll
     */
    public void hide() {
        if (isHidden) return;
        isHidden = true;
        animate()
                .translationY(getHeight() + dipToPx(getContext(), 32))
                .setDuration(260)
                .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                .start();
    }

    /**
     * Smoothly show navigation dock on scroll
     */
    public void show() {
        if (!isHidden) return;
        isHidden = false;
        animate()
                .translationY(0f)
                .setDuration(280)
                .setInterpolator(new OvershootInterpolator(1.0f))
                .start();
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState());
        bundle.putInt(STATE_SELECTED_ID, selectedId);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            selectedId = bundle.getInt(STATE_SELECTED_ID, R.id.homeBookFragment);
            super.onRestoreInstanceState(bundle.getParcelable(STATE_SUPER));
            setSelectedItemId(selectedId, false);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private static int dipToPx(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
