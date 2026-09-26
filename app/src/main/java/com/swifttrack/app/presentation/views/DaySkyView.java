package com.swifttrack.app.presentation.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DaySkyView extends View {

    // Cloud Puff Node with dynamic wind morphing
    private static class CloudNode {
        float relX, relY;
        float radiusX, radiusY;
        float morphPhase;
        float morphSpeed;
    }

    // Layered Parallax Cloud
    private static class CinematicCloud {
        int layer; // 1: Far, 2: Mid, 3: Near
        float x;
        float y;
        float scale;
        float speed;
        int baseAlpha;
        List<CloudNode> nodes = new ArrayList<>();
    }

    private final List<CinematicCloud> clouds = new ArrayList<>();
    private final Random random = new Random();

    // Layer 5: Atmospheric Lighting & Sun Bloom Paints
    private Paint skyPaint;
    private Paint horizonHazePaint;
    private Paint sunCorePaint;
    private Paint sunInnerCoronaPaint;
    private Paint sunMidCoronaPaint;
    private Paint sunOuterCoronaPaint;
    private Paint sunRayPaint;

    // Layer 4: Atmosphere Mist / Fog Paint
    private Paint mistFogPaint;

    // Volumetric Layered Cloud Paints (Soft Feathered Washes)
    private Paint cloudDeepShadowPaint;
    private Paint cloudMidShadowPaint;
    private Paint cloudBodyPaint;
    private Paint cloudSilverLiningPaint;

    private float animTime = 0f;
    private boolean isAnimating = false;

    public DaySkyView(Context context) {
        super(context);
        init();
    }

    public DaySkyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DaySkyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        skyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        horizonHazePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        horizonHazePaint.setStyle(Paint.Style.FILL);

        sunCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunCorePaint.setStyle(Paint.Style.FILL);

        sunInnerCoronaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunInnerCoronaPaint.setStyle(Paint.Style.FILL);

        sunMidCoronaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunMidCoronaPaint.setStyle(Paint.Style.FILL);

        sunOuterCoronaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunOuterCoronaPaint.setStyle(Paint.Style.FILL);

        sunRayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunRayPaint.setStyle(Paint.Style.STROKE);

        mistFogPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mistFogPaint.setStyle(Paint.Style.FILL);

        cloudDeepShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloudDeepShadowPaint.setStyle(Paint.Style.FILL);

        cloudMidShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloudMidShadowPaint.setStyle(Paint.Style.FILL);

        cloudBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloudBodyPaint.setStyle(Paint.Style.FILL);

        cloudSilverLiningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloudSilverLiningPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;

        float density = getContext().getResources().getDisplayMetrics().density;

        // Build 5-Layer Cinematic Cloud Architecture:
        // Layer 1: Far Clouds (4 clouds, small scale, blurred soft opacity, slowest speed)
        // Layer 2: Mid Clouds (5 clouds, medium scale, moderate opacity, medium speed)
        // Layer 3: Near Clouds (3 clouds, large scale, high detail, fastest speed)
        clouds.clear();

        // --- Layer 1: Far Background Clouds ---
        for (int i = 0; i < 4; i++) {
            CinematicCloud c = new CinematicCloud();
            c.layer = 1;
            c.x = (random.nextFloat() * (w + density * 260f)) - density * 130f;
            c.y = h * (0.08f + (i * 0.07f));
            c.scale = 0.45f + random.nextFloat() * 0.25f;
            c.speed = density * (0.12f + random.nextFloat() * 0.08f); // Slowest parallax
            c.baseAlpha = 110 + random.nextInt(40);
            generateCloudNodes(c, density, 6 + random.nextInt(4));
            clouds.add(c);
        }

        // --- Layer 2: Mid Altitude Clouds ---
        for (int i = 0; i < 5; i++) {
            CinematicCloud c = new CinematicCloud();
            c.layer = 2;
            c.x = (random.nextFloat() * (w + density * 300f)) - density * 150f;
            c.y = h * (0.14f + (i * 0.09f));
            c.scale = 0.8f + random.nextFloat() * 0.35f;
            c.speed = density * (0.28f + random.nextFloat() * 0.14f); // Medium parallax
            c.baseAlpha = 160 + random.nextInt(50);
            generateCloudNodes(c, density, 9 + random.nextInt(4));
            clouds.add(c);
        }

        // --- Layer 3: Near Foreground Clouds ---
        for (int i = 0; i < 3; i++) {
            CinematicCloud c = new CinematicCloud();
            c.layer = 3;
            c.x = (random.nextFloat() * (w + density * 360f)) - density * 180f;
            c.y = h * (0.22f + (i * 0.12f));
            c.scale = 1.25f + random.nextFloat() * 0.45f;
            c.speed = density * (0.48f + random.nextFloat() * 0.22f); // Fastest parallax
            c.baseAlpha = 210 + random.nextInt(45);
            generateCloudNodes(c, density, 12 + random.nextInt(5));
            clouds.add(c);
        }

        // Layer 5: Atmospheric Sky & Haze Shaders
        LinearGradient bgShader = new LinearGradient(
                0, 0, 0, h,
                new int[]{0xFF0B46A3, 0xFF175CC4, 0xFF3581EC, 0xFF76AFFA, 0xFFC2DFFF},
                new float[]{0f, 0.25f, 0.55f, 0.82f, 1f},
                Shader.TileMode.CLAMP
        );
        skyPaint.setShader(bgShader);

        LinearGradient hazeShader = new LinearGradient(
                0, h * 0.65f, 0, h,
                new int[]{0x00C2DFFF, 0x55E3F0FF, 0x99F5F9FF},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );
        horizonHazePaint.setShader(hazeShader);

        // Layer 4: Low-Altitude Mist / Fog Shader
        LinearGradient mistShader = new LinearGradient(
                0, 0, w, 0,
                new int[]{0x00FFFFFF, 0x35E6F2FF, 0x50FFFFFF, 0x35E6F2FF, 0x00FFFFFF},
                new float[]{0f, 0.25f, 0.5f, 0.75f, 1f},
                Shader.TileMode.CLAMP
        );
        mistFogPaint.setShader(mistShader);
    }

    private void generateCloudNodes(CinematicCloud c, float density, int count) {
        c.nodes.clear();
        for (int n = 0; n < count; n++) {
            CloudNode node = new CloudNode();
            node.relX = (random.nextFloat() - 0.5f) * (density * 150f * c.scale);
            node.relY = (random.nextFloat() - 0.4f) * (density * 40f * c.scale);
            node.radiusX = (density * (26f + random.nextFloat() * 32f)) * c.scale;
            node.radiusY = node.radiusX * (0.6f + random.nextFloat() * 0.35f);
            node.morphPhase = random.nextFloat() * (float) (Math.PI * 2);
            node.morphSpeed = 0.4f + random.nextFloat() * 0.8f;
            c.nodes.add(node);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        animTime += 0.016f;

        // --- Layer 5: Base Sky & Atmospheric Haze ---
        canvas.drawRect(0, 0, w, h, skyPaint);
        canvas.drawRect(0, h * 0.65f, w, h, horizonHazePaint);

        // --- Layer 5: Sun Core, Bloom & 360-Degree Corona ---
        drawAtmosphericSunAndBloom(canvas, w, h);

        // --- Layer 1: Far Background Clouds (Slow Parallax, Blurred Soft Opacity) ---
        drawCloudLayer(canvas, w, h, 1);

        // --- Layer 4: Low-Altitude Atmosphere Mist / Fog ---
        drawAtmosphereMistFog(canvas, w, h);

        // --- Layer 2: Mid-Altitude Clouds (Medium Parallax & Volumetric Depth) ---
        drawCloudLayer(canvas, w, h, 2);

        // --- Layer 3: Near Foreground Clouds (Fast Parallax, High Detail, Silver Lining) ---
        drawCloudLayer(canvas, w, h, 3);

        if (isAnimating) {
            postInvalidateOnAnimation();
        }
    }

    private void drawAtmosphericSunAndBloom(Canvas canvas, int w, int h) {
        float density = getContext().getResources().getDisplayMetrics().density;

        float cx = w * 0.5f;
        float cy = h * 0.22f;
        float sunRadius = density * 40f;

        float pulse = (float) ((Math.sin(animTime * 1.5f) + 1.0) / 2.0);

        // Outer Sky Bloom Halo
        float outerBloomR = sunRadius * (5.2f + 0.6f * pulse);
        RadialGradient outerBloomShader = new RadialGradient(
                cx, cy, outerBloomR,
                new int[]{0x77FFFFFF, 0x44FFF0B5, 0x1A3581EC, 0x00000000},
                new float[]{0f, 0.4f, 0.75f, 1f},
                Shader.TileMode.CLAMP
        );
        sunOuterCoronaPaint.setShader(outerBloomShader);
        canvas.drawCircle(cx, cy, outerBloomR, sunOuterCoronaPaint);

        // Mid Golden Solar Corona
        float midCoronaR = sunRadius * (2.9f + 0.3f * pulse);
        RadialGradient midCoronaShader = new RadialGradient(
                cx, cy, midCoronaR,
                new int[]{0xBBFFFFFF, 0x88FFF5BD, 0x33FFD26F, 0x00000000},
                new float[]{0f, 0.45f, 0.8f, 1f},
                Shader.TileMode.CLAMP
        );
        sunMidCoronaPaint.setShader(midCoronaShader);
        canvas.drawCircle(cx, cy, midCoronaR, sunMidCoronaPaint);

        // Inner Core Glow
        float innerCoronaR = sunRadius * 1.7f;
        RadialGradient innerCoronaShader = new RadialGradient(
                cx, cy, innerCoronaR,
                new int[]{0xFFFFFFFF, 0xEEFFFDF0, 0x00000000},
                new float[]{0f, 0.65f, 1f},
                Shader.TileMode.CLAMP
        );
        sunInnerCoronaPaint.setShader(innerCoronaShader);
        canvas.drawCircle(cx, cy, innerCoronaR, sunInnerCoronaPaint);

        // 360-Degree Radial Sunbeams (16 rays)
        int numRays = 16;
        for (int i = 0; i < numRays; i++) {
            float angle = (float) (i * (Math.PI * 2.0 / numRays) + animTime * 0.08f);
            float rayLen = sunRadius * (2.5f + 0.6f * (float) Math.sin(animTime * 1.8f + i));

            float startX = cx + (float) Math.cos(angle) * (sunRadius * 0.95f);
            float startY = cy + (float) Math.sin(angle) * (sunRadius * 0.95f);
            float endX = cx + (float) Math.cos(angle) * rayLen;
            float endY = cy + (float) Math.sin(angle) * rayLen;

            sunRayPaint.setColor(0x38FFFDF0);
            sunRayPaint.setStrokeWidth(density * (2.2f + (i % 2) * 1.4f));
            canvas.drawLine(startX, startY, endX, endY, sunRayPaint);
        }

        // Core Specular Sun Disc
        RadialGradient sunCoreShader = new RadialGradient(
                cx - sunRadius * 0.2f, cy - sunRadius * 0.2f, sunRadius * 1.25f,
                new int[]{0xFFFFFFFF, 0xFFFFFFF5, 0xFFFFF6DF, 0xFFFFE899},
                new float[]{0f, 0.4f, 0.75f, 1f},
                Shader.TileMode.CLAMP
        );
        sunCorePaint.setShader(sunCoreShader);
        canvas.drawCircle(cx, cy, sunRadius, sunCorePaint);
    }

    private void drawAtmosphereMistFog(Canvas canvas, int w, int h) {
        float density = getContext().getResources().getDisplayMetrics().density;
        float mistShift = (float) Math.sin(animTime * 0.4f) * (density * 30f);

        canvas.save();
        canvas.translate(mistShift, h * 0.38f);
        canvas.drawRect(-density * 60f, 0, w + density * 60f, density * 70f, mistFogPaint);
        canvas.restore();
    }

    private void drawCloudLayer(Canvas canvas, int w, int h, int targetLayer) {
        float density = getContext().getResources().getDisplayMetrics().density;

        for (CinematicCloud c : clouds) {
            if (c.layer != targetLayer) continue;

            // Parallax Scroll Wind Drift
            c.x += c.speed;
            if (c.x > w + density * 220f * c.scale) {
                c.x = -density * 220f * c.scale;
            }

            int alphaMultiplier = c.baseAlpha;

            // Render Cloud Nodes with Multi-Pass Feathered Volumetric Washes
            // Pass 1: Deep Atmosphere Shadow Wash
            for (CloudNode n : c.nodes) {
                float morph = (float) Math.sin(animTime * n.morphSpeed + n.morphPhase) * (density * 3f);
                float nx = c.x + n.relX + morph;
                float ny = c.y + n.relY + n.radiusY * 0.32f;
                float r = n.radiusX * 1.15f;

                int a1 = (int) (0x50 * (alphaMultiplier / 255.0f));
                int a2 = (int) (0x25 * (alphaMultiplier / 255.0f));

                RadialGradient shadowWash = new RadialGradient(
                        nx, ny, r,
                        new int[]{Color.argb(a1, 75, 110, 145), Color.argb(a2, 75, 110, 145), Color.TRANSPARENT},
                        new float[]{0f, 0.62f, 1f},
                        Shader.TileMode.CLAMP
                );
                cloudDeepShadowPaint.setShader(shadowWash);
                canvas.drawCircle(nx, ny, r, cloudDeepShadowPaint);
            }

            // Pass 2: Mid Soft Lavender Shadow Wash
            for (CloudNode n : c.nodes) {
                float morph = (float) Math.sin(animTime * n.morphSpeed + n.morphPhase) * (density * 3f);
                float nx = c.x + n.relX + morph;
                float ny = c.y + n.relY + n.radiusY * 0.16f;
                float r = n.radiusX * 1.06f;

                int a1 = (int) (0x88 * (alphaMultiplier / 255.0f));
                int a2 = (int) (0x38 * (alphaMultiplier / 255.0f));

                RadialGradient midWash = new RadialGradient(
                        nx, ny, r,
                        new int[]{Color.argb(a1, 140, 175, 210), Color.argb(a2, 140, 175, 210), Color.TRANSPARENT},
                        new float[]{0f, 0.65f, 1f},
                        Shader.TileMode.CLAMP
                );
                cloudMidShadowPaint.setShader(midWash);
                canvas.drawCircle(nx, ny, r, cloudMidShadowPaint);
            }

            // Pass 3: Main Fluff Body Wash
            for (CloudNode n : c.nodes) {
                float morph = (float) Math.sin(animTime * n.morphSpeed + n.morphPhase) * (density * 3f);
                float nx = c.x + n.relX + morph;
                float ny = c.y + n.relY;
                float r = n.radiusX;

                int a1 = (int) (0xEA * (alphaMultiplier / 255.0f));
                int a2 = (int) (0x95 * (alphaMultiplier / 255.0f));

                RadialGradient bodyWash = new RadialGradient(
                        nx, ny, r,
                        new int[]{Color.argb(a1, 248, 252, 255), Color.argb(a2, 248, 252, 255), Color.TRANSPARENT},
                        new float[]{0f, 0.7f, 1f},
                        Shader.TileMode.CLAMP
                );
                cloudBodyPaint.setShader(bodyWash);
                canvas.drawCircle(nx, ny, r, cloudBodyPaint);
            }

            // Pass 4: Sunlit Top Silver Lining Rim Wash (Highest intensity on Layer 3)
            for (CloudNode n : c.nodes) {
                float morph = (float) Math.sin(animTime * n.morphSpeed + n.morphPhase) * (density * 3f);
                float nx = c.x + n.relX + morph - n.radiusX * 0.1f;
                float ny = c.y + n.relY - n.radiusY * 0.22f;
                float r = n.radiusX * 0.82f;

                int silverAlpha = (targetLayer == 3) ? 0xFF : 0xDD;
                int a1 = (int) (silverAlpha * (alphaMultiplier / 255.0f));
                int a2 = (int) (0xBB * (alphaMultiplier / 255.0f));

                RadialGradient silverWash = new RadialGradient(
                        nx, ny, r,
                        new int[]{Color.argb(a1, 255, 255, 255), Color.argb(a2, 255, 255, 255), Color.TRANSPARENT},
                        new float[]{0f, 0.6f, 1f},
                        Shader.TileMode.CLAMP
                );
                cloudSilverLiningPaint.setShader(silverWash);
                canvas.drawCircle(nx, ny, r, cloudSilverLiningPaint);
            }
        }
    }

    public void startAnimation() {
        if (!isAnimating) {
            isAnimating = true;
            invalidate();
        }
    }

    public void stopAnimation() {
        isAnimating = false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}
