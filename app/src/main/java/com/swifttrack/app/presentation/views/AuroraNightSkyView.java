package com.swifttrack.app.presentation.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Cinematic Photorealistic Night Sky View featuring:
 * - Solid, high-opacity, brilliantly radiant 3D Moon with clear surface visibility
 * - Seamless feathered lunar terrain (no overlapping circles) with 3D spherical lighting & Tycho ejecta rays
 * - Real-time randomized flowing Aurora Borealis (luminous translucent neon green + pink/magenta ribbons drifting over the moon)
 * - Living multi-tiered stars with natural twinkling & 4-point cross diffraction spikes
 * - Floating atmospheric micro-particles
 */
public class AuroraNightSkyView extends View {

    // --- Star Data Model ---
    private static class Star {
        float x, y;
        float radius;
        int baseAlpha;
        float blinkSpeed;
        float phase;
        int type; // 0: Micro Stardust, 1: Medium Ambient Star, 2: Bright Guide Star, 3: 4-Point Cross Sparkle
        int color;
    }

    // --- Aurora Curtain Data Model ---
    private static class AuroraCurtain {
        float baseHeightRatio;
        float maxCurtainHeight;
        float speed1, speed2, speed3;
        float freq1, freq2, freq3;
        float amp1, amp2, amp3;
        int[] gradientColors;
        float[] gradientStops;
        Path path = new Path();
        List<AuroraRay> rays = new ArrayList<>();
    }

    private static class AuroraRay {
        float xRatio;
        float heightFactor;
        float phase;
        float speed;
        float width;
        int baseAlpha;
        float driftSpeed;
    }

    // --- Micro Atmospheric Particle ---
    private static class AtmosphericParticle {
        float x, y;
        float vx, vy;
        float radius;
        int alpha;
        float phase;
    }

    private final List<Star> stars = new ArrayList<>();
    private final List<AuroraCurtain> auroraCurtains = new ArrayList<>();
    private final List<AtmosphericParticle> atmosphericParticles = new ArrayList<>();
    private final Random random = new Random(42);

    // Background & Atmosphere Paints
    private Paint bgPaint;
    private Paint horizonGlowPaint;

    // Star & Flare Paints
    private Paint starPaint;
    private Paint starHaloPaint;
    private Paint flarePaint;
    private Paint particlePaint;

    // Aurora Paints
    private Paint auroraCurtainPaint;
    private Paint auroraRayPaint;

    // 3D Radiant Solid Moon Paints
    private Paint moonSolidBasePaint;
    private Paint moonGlowOuterPaint;
    private Paint moonGlowMidPaint;
    private Paint moonGlowInnerPaint;
    private Paint moonGlowCorePaint;
    private Paint moonBodyPaint;
    private Paint moonMariaPaint;
    private Paint moonCraterRimPaint;
    private Paint moonCraterPitPaint;
    private Paint moonRayPaint;
    private Paint moonTerminatorPaint;
    private Paint moonLimbHighlightPaint;

    private final RectF ovalRect = new RectF();

    private float animTime = 0f;
    private boolean isAnimating = false;

    public AuroraNightSkyView(Context context) {
        super(context);
        init();
    }

    public AuroraNightSkyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuroraNightSkyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        horizonGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        horizonGlowPaint.setStyle(Paint.Style.FILL);

        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPaint.setStyle(Paint.Style.FILL);

        starHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starHaloPaint.setStyle(Paint.Style.FILL);

        flarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        flarePaint.setStyle(Paint.Style.STROKE);
        flarePaint.setStrokeCap(Paint.Cap.ROUND);

        particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particlePaint.setStyle(Paint.Style.FILL);

        // Aurora Paints (Translucent, luminous shaders)
        auroraCurtainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        auroraCurtainPaint.setStyle(Paint.Style.FILL);

        auroraRayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        auroraRayPaint.setStyle(Paint.Style.STROKE);
        auroraRayPaint.setStrokeCap(Paint.Cap.ROUND);

        // 3D Radiant High-Opacity Moon Paints
        moonSolidBasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonSolidBasePaint.setStyle(Paint.Style.FILL);
        moonSolidBasePaint.setColor(0xFFFFFFFF); // Pure solid white base disc for 100% opacity

        moonGlowOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonGlowOuterPaint.setStyle(Paint.Style.FILL);

        moonGlowMidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonGlowMidPaint.setStyle(Paint.Style.FILL);

        moonGlowInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonGlowInnerPaint.setStyle(Paint.Style.FILL);

        moonGlowCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonGlowCorePaint.setStyle(Paint.Style.FILL);

        moonBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonBodyPaint.setStyle(Paint.Style.FILL);

        moonMariaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonMariaPaint.setStyle(Paint.Style.FILL);

        moonCraterRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonCraterRimPaint.setStyle(Paint.Style.STROKE);

        moonCraterPitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonCraterPitPaint.setStyle(Paint.Style.FILL);

        moonRayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonRayPaint.setStyle(Paint.Style.STROKE);

        moonTerminatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonTerminatorPaint.setStyle(Paint.Style.FILL);

        moonLimbHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        moonLimbHighlightPaint.setStyle(Paint.Style.STROKE);
        moonLimbHighlightPaint.setStrokeWidth(1.8f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;

        float density = getContext().getResources().getDisplayMetrics().density;

        // 1. Physically Believable Deep Night Sky Gradient (Zenith to Nadir)
        LinearGradient bgShader = new LinearGradient(
                0, 0, 0, h,
                new int[]{
                        0xFF040208, // Zenith near-black
                        0xFF070D1E, // Midnight navy
                        0xFF0B1432, // Deep space atmospheric blue
                        0xFF141030, // Indigo violet
                        0xFF1A0E2E, // Soft horizon transition
                        0xFF100720  // Horizon baseline
                },
                new float[]{0f, 0.20f, 0.45f, 0.72f, 0.90f, 1.0f},
                Shader.TileMode.CLAMP
        );
        bgPaint.setShader(bgShader);

        // 2. Horizon Environmental Airglow
        RadialGradient horizonShader = new RadialGradient(
                w * 0.5f, h * 1.05f, w * 0.95f,
                new int[]{0x281C2652, 0x12140D2A, 0x00000000},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        );
        horizonGlowPaint.setShader(horizonShader);

        // 3. Multi-Tiered Living Stars
        stars.clear();
        int totalStars = 210;
        for (int i = 0; i < totalStars; i++) {
            Star star = new Star();
            star.x = random.nextFloat() * w;
            star.y = random.nextFloat() * h;
            star.phase = random.nextFloat() * (float) (Math.PI * 2);
            star.blinkSpeed = 0.015f + random.nextFloat() * 0.035f;

            float p = random.nextFloat();
            if (p < 0.62f) {
                // Tier 0: Micro Stardust
                star.type = 0;
                star.radius = (0.45f + random.nextFloat() * 0.45f) * density;
                star.baseAlpha = 45 + random.nextInt(65);
                star.color = Color.WHITE;
            } else if (p < 0.85f) {
                // Tier 1: Medium Ambient Stars
                star.type = 1;
                star.radius = (0.85f + random.nextFloat() * 0.65f) * density;
                star.baseAlpha = 95 + random.nextInt(90);
                float c = random.nextFloat();
                if (c < 0.25f) star.color = 0xFFE0F2FE; // Diamond blue-white
                else if (c < 0.45f) star.color = 0xFFFFF8E7; // Warm amber-white
                else star.color = Color.WHITE;
            } else if (p < 0.95f) {
                // Tier 2: Bright Guide Stars with soft halo
                star.type = 2;
                star.radius = (1.6f + random.nextFloat() * 0.7f) * density;
                star.baseAlpha = 175 + random.nextInt(75);
                star.color = 0xFFF8FAFC;
            } else {
                // Tier 3: Brilliant Focal Stars with 4-Point Diffraction Sparkles
                star.type = 3;
                star.radius = (2.2f + random.nextFloat() * 0.8f) * density;
                star.baseAlpha = 210 + random.nextInt(45);
                star.color = 0xFFFFFFFF;
            }
            stars.add(star);
        }

        // 4. Multilayered Aurora Borealis Setup (Translucent & Ethereal for Moon Visibility)
        initAuroraCurtains(w, h, density);

        // 5. Floating Atmospheric Micro-Particles
        atmosphericParticles.clear();
        for (int i = 0; i < 18; i++) {
            AtmosphericParticle p = new AtmosphericParticle();
            p.x = random.nextFloat() * w;
            p.y = random.nextFloat() * h * 0.85f;
            p.vx = (random.nextFloat() - 0.5f) * 0.18f * density;
            p.vy = -0.10f - random.nextFloat() * 0.22f * density;
            p.radius = (0.6f + random.nextFloat() * 0.8f) * density;
            p.alpha = 30 + random.nextInt(45);
            p.phase = random.nextFloat() * (float) (Math.PI * 2);
            atmosphericParticles.add(p);
        }
    }

    private void initAuroraCurtains(int w, int h, float density) {
        auroraCurtains.clear();

        // --- Curtain 1: High-Altitude Translucent Veil ---
        AuroraCurtain c1 = new AuroraCurtain();
        c1.baseHeightRatio = 0.13f;
        c1.maxCurtainHeight = h * 0.24f;
        c1.speed1 = 0.28f; c1.speed2 = 0.18f; c1.speed3 = 0.10f;
        c1.freq1 = 0.0030f; c1.freq2 = 0.0070f; c1.freq3 = 0.011f;
        c1.amp1 = 28f * density; c1.amp2 = 16f * density; c1.amp3 = 9f * density;
        c1.gradientColors = new int[]{
                0x0045FFB0, // Bottom edge transparent fade
                0x4845FFB0, // Luminous Neon Green
                0x3500D889, // Emerald mid curtain
                0x28FF4FA3, // Soft Magenta/Pink peak
                0x10FF4FA3, // Upper pink fade
                0x00000000  // Apex transparent
        };
        c1.gradientStops = new float[]{0f, 0.14f, 0.40f, 0.70f, 0.88f, 1.0f};
        populateCurtainRays(c1, 28, density);
        auroraCurtains.add(c1);

        // --- Curtain 2: Main Mid-Altitude Aurora Drapery ---
        AuroraCurtain c2 = new AuroraCurtain();
        c2.baseHeightRatio = 0.21f;
        c2.maxCurtainHeight = h * 0.28f;
        c2.speed1 = 0.36f; c2.speed2 = 0.24f; c2.speed3 = 0.14f;
        c2.freq1 = 0.0038f; c2.freq2 = 0.0085f; c2.freq3 = 0.014f;
        c2.amp1 = 34f * density; c2.amp2 = 20f * density; c2.amp3 = 11f * density;
        c2.gradientColors = new int[]{
                0x0075FFE0, // Soft cyan base fade
                0x5545FFB0, // Neon Green
                0x4000E5A3, // Emerald green body
                0x32FF4FA3, // Pink / Magenta crest
                0x15E040FB, // Delicate violet-pink shimmer
                0x00000000  // Transparent apex
        };
        c2.gradientStops = new float[]{0f, 0.12f, 0.38f, 0.68f, 0.86f, 1.0f};
        populateCurtainRays(c2, 32, density);
        auroraCurtains.add(c2);

        // --- Curtain 3: Foreground Shimmering Wave ---
        AuroraCurtain c3 = new AuroraCurtain();
        c3.baseHeightRatio = 0.29f;
        c3.maxCurtainHeight = h * 0.22f;
        c3.speed1 = 0.22f; c3.speed2 = 0.32f; c3.speed3 = 0.16f;
        c3.freq1 = 0.0034f; c3.freq2 = 0.0078f; c3.freq3 = 0.013f;
        c3.amp1 = 22f * density; c3.amp2 = 14f * density; c3.amp3 = 8f * density;
        c3.gradientColors = new int[]{
                0x0000D889,
                0x4500D889,
                0x3045FFB0,
                0x20FF4FA3,
                0x00000000
        };
        c3.gradientStops = new float[]{0f, 0.15f, 0.45f, 0.76f, 1.0f};
        populateCurtainRays(c3, 24, density);
        auroraCurtains.add(c3);
    }

    private void populateCurtainRays(AuroraCurtain c, int count, float density) {
        c.rays.clear();
        for (int i = 0; i < count; i++) {
            AuroraRay ray = new AuroraRay();
            ray.xRatio = (float) i / (float) count + (random.nextFloat() - 0.5f) * 0.03f;
            ray.heightFactor = 0.60f + random.nextFloat() * 0.40f;
            ray.phase = random.nextFloat() * (float) (Math.PI * 2);
            ray.speed = 1.2f + random.nextFloat() * 1.8f;
            ray.width = (1.4f + random.nextFloat() * 2.2f) * density;
            ray.baseAlpha = 30 + random.nextInt(40);
            ray.driftSpeed = (random.nextFloat() - 0.5f) * 0.0008f;
            c.rays.add(ray);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        animTime += 0.016f;

        // 1. Deep Space Atmospheric Gradient Background
        canvas.drawRect(0, 0, w, h, bgPaint);

        // 2. Horizon Environmental Airglow
        canvas.drawRect(0, h * 0.50f, w, h, horizonGlowPaint);

        // 3. Background Living Stars
        drawLivingStars(canvas, false);

        // 4. SOLID, HIGH-OPACITY 3D RADIANT MOON (Rendered before aurora so aurora passes over it)
        drawPhotorealisticMoon(canvas, w, h);

        // 5. REAL-TIME FLOWING AURORA BOREALIS (Translucent layers drift in front of the Moon)
        drawRealtimeAurora(canvas, w, h);

        // 6. Forefront Sparkling Guide Stars
        drawLivingStars(canvas, true);

        // 7. Floating Atmospheric Micro-Particles
        drawAtmosphericParticles(canvas, w, h);

        if (isAnimating) {
            postInvalidateOnAnimation();
        }
    }

    // ==========================================
    // SOLID, HIGH-OPACITY 3D RADIANT MOON
    // ==========================================
    private void drawPhotorealisticMoon(Canvas canvas, int w, int h) {
        float density = getContext().getResources().getDisplayMetrics().density;

        float cx = w * 0.50f;
        float cy = h * 0.18f;
        float radius = density * 40f; // Prominent, crisp 80dp diameter

        // Dynamic Breathing Pulse for Lunar Atmosphere
        float pulse = (float) ((Math.sin(animTime * 1.5f) + 1.0) / 2.0);

        // A) Vast Ambient Volumetric Nebula Bloom
        float outerGlowRadius = radius * (5.5f + 0.6f * pulse);
        RadialGradient outerGlowShader = new RadialGradient(
                cx, cy, outerGlowRadius,
                new int[]{0x6690D0FF, 0x354080E0, 0x14183880, 0x00000000},
                new float[]{0f, 0.28f, 0.62f, 1.0f},
                Shader.TileMode.CLAMP
        );
        moonGlowOuterPaint.setShader(outerGlowShader);
        canvas.drawCircle(cx, cy, outerGlowRadius, moonGlowOuterPaint);

        // B) Mid Atmospheric Corona (Luminous white with soft aurora cyan/blue scatter)
        float midGlowRadius = radius * (3.0f + 0.3f * pulse);
        RadialGradient midGlowShader = new RadialGradient(
                cx, cy, midGlowRadius,
                new int[]{0x95EAF3FF, 0x4575FFE0, 0x00000000},
                new float[]{0f, 0.48f, 1.0f},
                Shader.TileMode.CLAMP
        );
        moonGlowMidPaint.setShader(midGlowShader);
        canvas.drawCircle(cx, cy, midGlowRadius, moonGlowMidPaint);

        // C) Inner Specular Core Bloom
        float innerGlowRadius = radius * 1.75f;
        RadialGradient innerGlowShader = new RadialGradient(
                cx, cy, innerGlowRadius,
                new int[]{0xD0FFFFFF, 0x75EAF3FF, 0x00000000},
                new float[]{0f, 0.55f, 1.0f},
                Shader.TileMode.CLAMP
        );
        moonGlowInnerPaint.setShader(innerGlowShader);
        canvas.drawCircle(cx, cy, innerGlowRadius, moonGlowInnerPaint);

        // D) Specular Halo Rim Core
        float coreGlowRadius = radius * 1.25f;
        RadialGradient coreGlowShader = new RadialGradient(
                cx, cy, coreGlowRadius,
                new int[]{0xF8FFFFFF, 0x95FFFFFF, 0x00000000},
                new float[]{0f, 0.70f, 1.0f},
                Shader.TileMode.CLAMP
        );
        moonGlowCorePaint.setShader(coreGlowShader);
        canvas.drawCircle(cx, cy, coreGlowRadius, moonGlowCorePaint);

        // E) Solid Opaque Lunar Foundation (Guarantees 100% Solid Opacity)
        canvas.drawCircle(cx, cy, radius, moonSolidBasePaint);

        // F) Physically Realistic 3D Spherical Moon Body (Directional Sunlight from Top-Left)
        RadialGradient moonBodyShader = new RadialGradient(
                cx - radius * 0.30f, cy - radius * 0.30f, radius * 1.40f,
                new int[]{
                        0xFFFFFFFF, // Specular solar apex
                        0xFFF8FAFC, // Sunlit anorthosite lunar highlands
                        0xFFE8EEF5, // Highlands regolith
                        0xFFD5DFEC, // Midtone transition
                        0xFFB0BFD0, // Terminator approach
                        0xFF8595A8, // Limb darkening
                        0xFF607285  // Dark limb base
                },
                new float[]{0f, 0.22f, 0.45f, 0.68f, 0.85f, 0.95f, 1.0f},
                Shader.TileMode.CLAMP
        );
        moonBodyPaint.setShader(moonBodyShader);
        canvas.drawCircle(cx, cy, radius, moonBodyPaint);

        // G) Seamless Feathered Lunar Maria (Soft, Realistic Terrain without Harsh Patches)
        drawSeamlessLunarMaria(canvas, cx, cy, radius);

        // H) Impact Craters, High-Albedo Peaks & Tycho Ejecta Ray Systems
        drawLunarCratersAndRays(canvas, cx, cy, radius);

        // I) Subtle 3D Orbital Crescent Limb Shadow
        RadialGradient shadowShader = new RadialGradient(
                cx + radius * 0.44f, cy + radius * 0.44f, radius * 1.30f,
                new int[]{0x450B1020, 0x180B1020, 0x00000000},
                new float[]{0f, 0.60f, 1.0f},
                Shader.TileMode.CLAMP
        );
        moonTerminatorPaint.setShader(shadowShader);
        canvas.drawCircle(cx, cy, radius, moonTerminatorPaint);

        // J) Sunlit Limb Highlight Ring (Crisp Edge Reflection)
        moonLimbHighlightPaint.setColor(0x80FFFFFF);
        ovalRect.set(cx - radius + 0.8f, cy - radius + 0.8f, cx + radius - 0.8f, cy + radius - 0.8f);
        canvas.drawArc(ovalRect, 135f, 180f, false, moonLimbHighlightPaint);
    }

    /**
     * Renders seamless, feathered, photorealistic lunar maria with soft radial gradients.
     */
    private void drawSeamlessLunarMaria(Canvas canvas, float cx, float cy, float r) {
        // 1. Oceanus Procellarum (Broad western soft terrain gradient)
        float opX = cx - r * 0.30f;
        float opY = cy - r * 0.05f;
        float opR = r * 0.45f;
        RadialGradient opShader = new RadialGradient(
                opX, opY, opR,
                new int[]{0x221E293B, 0x121E293B, 0x001E293B},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        );
        moonMariaPaint.setShader(opShader);
        canvas.drawCircle(opX, opY, opR, moonMariaPaint);

        // 2. Mare Imbrium (Northwest circular impact basin with feathered edge)
        float miX = cx - r * 0.18f;
        float miY = cy - r * 0.32f;
        float miR = r * 0.32f;
        RadialGradient miShader = new RadialGradient(
                miX, miY, miR,
                new int[]{0x281E293B, 0x141E293B, 0x001E293B},
                new float[]{0f, 0.60f, 1f},
                Shader.TileMode.CLAMP
        );
        moonMariaPaint.setShader(miShader);
        canvas.drawCircle(miX, miY, miR, moonMariaPaint);

        // 3. Mare Serenitatis & Tranquillitatis (Eastern plains soft wash)
        float msX = cx + r * 0.22f;
        float msY = cy - r * 0.20f;
        float msR = r * 0.36f;
        RadialGradient msShader = new RadialGradient(
                msX, msY, msR,
                new int[]{0x241E293B, 0x101E293B, 0x001E293B},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        );
        moonMariaPaint.setShader(msShader);
        canvas.drawCircle(msX, msY, msR, moonMariaPaint);

        // 4. Mare Crisium (Distinct isolated eastern soft oval)
        float mcX = cx + r * 0.52f;
        float mcY = cy - r * 0.18f;
        float mcR = r * 0.18f;
        RadialGradient mcShader = new RadialGradient(
                mcX, mcY, mcR,
                new int[]{0x2A1E293B, 0x141E293B, 0x001E293B},
                new float[]{0f, 0.60f, 1f},
                Shader.TileMode.CLAMP
        );
        moonMariaPaint.setShader(mcShader);
        canvas.drawCircle(mcX, mcY, mcR, moonMariaPaint);

        // 5. Mare Nubium (Southwestern plains soft wash)
        float mnX = cx - r * 0.20f;
        float mnY = cy + r * 0.26f;
        float mnR = r * 0.26f;
        RadialGradient mnShader = new RadialGradient(
                mnX, mnY, mnR,
                new int[]{0x1E1E293B, 0x0E1E293B, 0x001E293B},
                new float[]{0f, 0.55f, 1f},
                Shader.TileMode.CLAMP
        );
        moonMariaPaint.setShader(mnShader);
        canvas.drawCircle(mnX, mnY, mnR, moonMariaPaint);
    }

    private void drawLunarCratersAndRays(Canvas canvas, float cx, float cy, float r) {
        // --- Tycho Crater (Prominent southern impact crater with vast ejecta ray system) ---
        float tychoX = cx + r * 0.16f;
        float tychoY = cy + r * 0.48f;

        // Tycho Ejecta Rays radiating across the southern disc
        moonRayPaint.setStrokeWidth(0.8f);
        moonRayPaint.setColor(0x28FFFFFF);
        canvas.drawLine(tychoX, tychoY, tychoX - r * 0.70f, tychoY - r * 0.60f, moonRayPaint);
        canvas.drawLine(tychoX, tychoY, tychoX + r * 0.50f, tychoY - r * 0.80f, moonRayPaint);
        canvas.drawLine(tychoX, tychoY, tychoX - r * 0.80f, tychoY + r * 0.16f, moonRayPaint);
        canvas.drawLine(tychoX, tychoY, tychoX + r * 0.60f, tychoY - r * 0.22f, moonRayPaint);
        canvas.drawLine(tychoX, tychoY, tychoX - r * 0.32f, tychoY - r * 0.90f, moonRayPaint);

        // Tycho Bright Impact Peak & Subtle Shadow Rim
        moonCraterPitPaint.setColor(0x95FFFFFF);
        canvas.drawCircle(tychoX, tychoY, r * 0.05f, moonCraterPitPaint);
        moonCraterRimPaint.setColor(0x300F172A);
        moonCraterRimPaint.setStrokeWidth(0.8f);
        canvas.drawCircle(tychoX + 0.6f, tychoY + 0.6f, r * 0.055f, moonCraterRimPaint);

        // --- Copernicus Crater (Northwest bright ray crater) ---
        float copX = cx - r * 0.18f;
        float copY = cy - r * 0.10f;
        moonCraterRimPaint.setColor(0x60FFFFFF);
        moonCraterRimPaint.setStrokeWidth(0.9f);
        canvas.drawCircle(copX, copY, r * 0.065f, moonCraterRimPaint);
        moonCraterPitPaint.setColor(0x250F172A);
        canvas.drawCircle(copX, copY, r * 0.04f, moonCraterPitPaint);

        // Copernicus Ejecta Wisps
        moonRayPaint.setColor(0x1CFFFFFF);
        canvas.drawLine(copX, copY, copX - r * 0.32f, copY - r * 0.28f, moonRayPaint);
        canvas.drawLine(copX, copY, copX + r * 0.28f, copY - r * 0.22f, moonRayPaint);

        // --- Kepler & Aristarchus (Bright high-albedo impact features) ---
        moonCraterPitPaint.setColor(0x75FFFFFF);
        canvas.drawCircle(cx - r * 0.42f, cy - r * 0.12f, r * 0.035f, moonCraterPitPaint);
        canvas.drawCircle(cx - r * 0.48f, cy - r * 0.30f, r * 0.04f, moonCraterPitPaint);
    }

    // ==========================================
    // REAL-TIME RANDOMIZED FLOWING AURORA BOREALIS
    // ==========================================
    private void drawRealtimeAurora(Canvas canvas, int w, int h) {
        for (AuroraCurtain curtain : auroraCurtains) {
            float baseY = h * curtain.baseHeightRatio;
            curtain.path.reset();

            int step = 14; // Ultra smooth 14px horizontal resolution
            int numPoints = (w / step) + 2;

            float[] xPoints = new float[numPoints];
            float[] yBottomPoints = new float[numPoints];
            float[] yTopPoints = new float[numPoints];

            for (int i = 0; i < numPoints; i++) {
                float x = i * step;
                xPoints[i] = x;

                // Organic dynamic harmonic wave with randomized time-varying phase shifts
                float waveOffset = (float) (
                        Math.sin(x * curtain.freq1 + animTime * curtain.speed1 + Math.sin(animTime * 0.4f) * 0.6f) * curtain.amp1 +
                        Math.sin(x * curtain.freq2 - animTime * curtain.speed2 + Math.cos(animTime * 0.3f) * 0.8f + 1.4f) * curtain.amp2 +
                        Math.cos(x * curtain.freq3 + animTime * curtain.speed3 + Math.sin(animTime * 0.2f) * 0.5f + 2.2f) * curtain.amp3
                );

                float yBottom = baseY + waveOffset;
                float heightPulse = (float) (0.82f + 0.18f * Math.sin(x * 0.004f + animTime * 0.38f));
                float yTop = yBottom - curtain.maxCurtainHeight * heightPulse;

                yBottomPoints[i] = yBottom;
                yTopPoints[i] = yTop;
            }

            // Build Ribbon Polygon Path
            curtain.path.moveTo(xPoints[0], yBottomPoints[0]);
            for (int i = 1; i < numPoints; i++) {
                curtain.path.lineTo(xPoints[i], yBottomPoints[i]);
            }
            curtain.path.lineTo(xPoints[numPoints - 1], yTopPoints[numPoints - 1]);
            for (int i = numPoints - 2; i >= 0; i--) {
                curtain.path.lineTo(xPoints[i], yTopPoints[i]);
            }
            curtain.path.close();

            // Set Vertical Linear Gradient
            LinearGradient curtainShader = new LinearGradient(
                    0, baseY + curtain.amp1, 0, baseY - curtain.maxCurtainHeight,
                    curtain.gradientColors,
                    curtain.gradientStops,
                    Shader.TileMode.CLAMP
            );
            auroraCurtainPaint.setShader(curtainShader);
            canvas.drawPath(curtain.path, auroraCurtainPaint);

            // Shimmering Vertical Ionization Rays
            drawCurtainRays(canvas, curtain, w, baseY);
        }
    }

    private void drawCurtainRays(Canvas canvas, AuroraCurtain curtain, int w, float baseY) {
        for (AuroraRay ray : curtain.rays) {
            // Dynamic drifting and breathing of filament rays
            ray.xRatio += ray.driftSpeed;
            if (ray.xRatio > 1.05f) ray.xRatio = -0.05f;
            if (ray.xRatio < -0.05f) ray.xRatio = 1.05f;

            float x = ray.xRatio * w;

            float waveOffset = (float) (
                    Math.sin(x * curtain.freq1 + animTime * curtain.speed1 + Math.sin(animTime * 0.4f) * 0.6f) * curtain.amp1 +
                    Math.sin(x * curtain.freq2 - animTime * curtain.speed2 + 1.4f) * curtain.amp2
            );

            float yBottom = baseY + waveOffset;
            float shimmer = (float) ((Math.sin(ray.phase + animTime * ray.speed) + 1.0) / 2.0);
            float rayHeight = curtain.maxCurtainHeight * ray.heightFactor * (0.70f + 0.30f * shimmer);
            float yTop = yBottom - rayHeight;

            // Natural slight tilt along magnetic field lines
            float xTilt = (x - w * 0.5f) * 0.05f;

            int alpha = (int) (ray.baseAlpha * (0.55f + 0.45f * shimmer));
            alpha = Math.max(0, Math.min(255, alpha));

            LinearGradient rayShader = new LinearGradient(
                    x, yBottom, x + xTilt, yTop,
                    new int[]{
                            Color.argb(0, 69, 255, 176),
                            Color.argb(alpha, 69, 255, 176),               // Neon green base
                            Color.argb((int)(alpha * 0.75f), 255, 79, 163), // Pink/Magenta ray tip
                            Color.TRANSPARENT
                    },
                    new float[]{0f, 0.18f, 0.78f, 1f},
                    Shader.TileMode.CLAMP
            );
            auroraRayPaint.setShader(rayShader);
            auroraRayPaint.setStrokeWidth(ray.width);
            canvas.drawLine(x, yBottom, x + xTilt, yTop, auroraRayPaint);
        }
    }

    // ==========================================
    // LIVING STARS & PARTICLES RENDERING
    // ==========================================
    private void drawLivingStars(Canvas canvas, boolean foregroundOnly) {
        for (Star s : stars) {
            if (foregroundOnly && (s.type == 0 || s.type == 1)) continue;
            if (!foregroundOnly && (s.type == 2 || s.type == 3)) continue;

            s.phase += s.blinkSpeed;
            float blink = (float) ((Math.sin(s.phase) + 1.0) / 2.0); // 0.0 to 1.0

            int alpha = (int) (s.baseAlpha * (0.35f + 0.65f * blink));
            alpha = Math.max(0, Math.min(255, alpha));

            float currentRadius = s.radius * (0.88f + 0.24f * blink);

            // 1. Star Core
            starPaint.setColor(s.color);
            starPaint.setAlpha(alpha);
            canvas.drawCircle(s.x, s.y, currentRadius, starPaint);

            // 2. Soft Gaussian Halo for Guide Stars (Tier 2)
            if (s.type == 2 && alpha > 110) {
                float haloR = currentRadius * 2.8f;
                RadialGradient haloShader = new RadialGradient(
                        s.x, s.y, haloR,
                        new int[]{Color.argb((int) (alpha * 0.35f), 240, 246, 255), Color.TRANSPARENT},
                        new float[]{0f, 1f},
                        Shader.TileMode.CLAMP
                );
                starHaloPaint.setShader(haloShader);
                canvas.drawCircle(s.x, s.y, haloR, starHaloPaint);
            }

            // 3. Delicate 4-Point Diffraction Cross Spikes for Brilliant Stars (Tier 3)
            if (s.type == 3 && alpha > 130) {
                float spikeLen = currentRadius * (3.0f + 1.5f * blink);
                flarePaint.setColor(Color.WHITE);
                flarePaint.setAlpha((int) (alpha * 0.75f));
                flarePaint.setStrokeWidth(1.2f);

                // Primary Horizontal & Vertical Diffraction Beams
                canvas.drawLine(s.x - spikeLen, s.y, s.x + spikeLen, s.y, flarePaint);
                canvas.drawLine(s.x, s.y - spikeLen, s.x, s.y + spikeLen, flarePaint);

                // Subtle Secondary Diagonal Beams
                float diagLen = spikeLen * 0.52f;
                flarePaint.setStrokeWidth(0.8f);
                flarePaint.setAlpha((int) (alpha * 0.40f));
                canvas.drawLine(s.x - diagLen, s.y - diagLen, s.x + diagLen, s.y + diagLen, flarePaint);
                canvas.drawLine(s.x - diagLen, s.y + diagLen, s.x + diagLen, s.y - diagLen, flarePaint);
            }
        }
    }

    private void drawAtmosphericParticles(Canvas canvas, int w, int h) {
        for (AtmosphericParticle p : atmosphericParticles) {
            p.x += p.vx;
            p.y += p.vy;
            p.phase += 0.02f;

            // Wrap around boundaries
            if (p.y < 0) {
                p.y = h * 0.80f;
                p.x = random.nextFloat() * w;
            }
            if (p.x < 0) p.x = w;
            if (p.x > w) p.x = 0;

            float pulse = (float) ((Math.sin(p.phase) + 1.0) / 2.0);
            int alpha = (int) (p.alpha * (0.5f + 0.5f * pulse));

            particlePaint.setColor(0xFF75FFE0);
            particlePaint.setAlpha(alpha);
            canvas.drawCircle(p.x, p.y, p.radius, particlePaint);
        }
    }

    // ==========================================
    // ANIMATION LIFECYCLE CONTROLS
    // ==========================================
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
