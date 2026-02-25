package com.astro.app;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Immersive night-sky canvas:
 *
 *  ┌─────────────────────────────────┐
 *  │  Deep space + Milky Way glow    │  ← top 75%: sky
 *  │  630 twinkling stars            │
 *  │  Up to 8 simultaneous meteors   │
 *  ├──────────────────────────────── │
 *  │  Far mountain silhouette (navy) │  ← bottom 28%
 *  │  Near mountain silhouette (blk) │
 *  └─────────────────────────────────┘
 *
 *  On startWarp(): stars accelerate radially outward → warp-speed effect.
 */
public class StarFieldView extends View {

    // ── Counts ───────────────────────────────────────────────────────────
    private static final int FIELD_STARS      = 450;
    private static final int MILKY_WAY_STARS  = 180;
    private static final int MAX_METEORS      = 9;
    // New meteor every ~450 ms once below cap
    private static final long METEOR_INTERVAL_MS = 450;

    // ── Reusable paints (zero allocations in onDraw except meteor shaders) ──
    private final Paint skyPaint       = new Paint();          // sky gradient
    private final Paint atmoGlowPaint  = new Paint();          // horizon glow
    private final Paint mwGlowPaint    = new Paint();          // Milky Way band
    private final Paint farMtnPaint    = new Paint();          // far mountain fill
    private final Paint nearMtnPaint   = new Paint();          // near mountain fill
    private final Paint ridgePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint starPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trailPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint meteorPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Scene geometry (built once in onSizeChanged) ─────────────────────
    private Path  farMountainPath;
    private Path  nearMountainPath;
    private RectF mwRect;   // bounding rect for Milky Way strip (rotated)

    // ── Scene data ───────────────────────────────────────────────────────
    private final List<Star>   stars   = new ArrayList<>(FIELD_STARS + MILKY_WAY_STARS);
    private final List<Meteor> meteors = new ArrayList<>(MAX_METEORS);
    private final Random rng = new Random(0xDEADBEEF);

    // ── State ────────────────────────────────────────────────────────────
    private boolean sizeReady    = false;
    private long    lastFrameMs  = 0;
    private long    lastMeteorMs = 0;
    private float   screenW, screenH;

    // ── Warp ─────────────────────────────────────────────────────────────
    private boolean warpMode    = false;
    private long    warpStartMs = 0;

    // =====================================================================
    //  Constructors
    // =====================================================================

    public StarFieldView(Context c)                         { super(c);      init(); }
    public StarFieldView(Context c, AttributeSet a)         { super(c, a);   init(); }
    public StarFieldView(Context c, AttributeSet a, int s)  { super(c, a, s); init(); }

    private void init() {
        starPaint.setStyle(Paint.Style.FILL);
        trailPaint.setStyle(Paint.Style.STROKE);
        trailPaint.setStrokeCap(Paint.Cap.ROUND);
        meteorPaint.setStyle(Paint.Style.STROKE);
        meteorPaint.setStrokeCap(Paint.Cap.BUTT);
        glowPaint.setStyle(Paint.Style.FILL);
        ridgePaint.setStyle(Paint.Style.STROKE);
        ridgePaint.setStrokeWidth(1.2f);
    }

    // =====================================================================
    //  Public API
    // =====================================================================

    public void startWarp() {
        warpMode    = true;
        warpStartMs = System.currentTimeMillis();
    }

    // =====================================================================
    //  Size change — build the entire scene
    // =====================================================================

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        if (w <= 0 || h <= 0) return;
        screenW = w;
        screenH = h;

        buildSkyPaints(w, h);
        buildMountains(w, h);
        buildStars(w, h);
        prewarmMeteors(w, h);
        sizeReady = true;
    }

    // ─── Sky / atmosphere paints ─────────────────────────────────────────

    private void buildSkyPaints(int w, int h) {
        // Deep-space gradient: near-black at zenith → very dark indigo at horizon
        skyPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{
                        Color.argb(255,  0,  0,  3),   // zenith: near-black
                        Color.argb(255,  1,  4, 14),   // mid-sky
                        Color.argb(255,  3,  9, 22)    // horizon: deep indigo
                },
                new float[]{0f, 0.60f, 1.0f},
                Shader.TileMode.CLAMP));

        // Atmospheric glow centred on the mountain horizon line
        float horizonY = h * 0.74f;
        atmoGlowPaint.setShader(new RadialGradient(
                w * 0.5f, horizonY, w * 0.80f,
                Color.argb(40, 18, 40, 90), Color.TRANSPARENT,
                Shader.TileMode.CLAMP));

        // Milky Way band: a narrow diagonal soft strip
        // We pre-compute a small paint; the rect is drawn rotated in onDraw
        mwRect = new RectF(-w * 0.6f, -h * 0.08f, w * 1.6f, h * 0.08f);
        mwGlowPaint.setShader(new LinearGradient(
                0, -h * 0.08f, 0, h * 0.08f,
                new int[]{Color.TRANSPARENT,
                        Color.argb(18, 195, 210, 255),
                        Color.argb(26, 210, 220, 255),
                        Color.argb(18, 195, 210, 255),
                        Color.TRANSPARENT},
                new float[]{0f, 0.25f, 0.5f, 0.75f, 1.0f},
                Shader.TileMode.CLAMP));
    }

    // ─── Mountain silhouettes ─────────────────────────────────────────────

    private void buildMountains(int w, int h) {
        // Far mountains: smooth gentle peaks, dark blue-grey fill
        farMountainPath  = waveMountainPath(w, h,
                h * 0.74f,                      // base (mean ridge height)
                h * 0.065f, 2.20f, 0.90f,       // primary wave
                h * 0.032f, 5.10f, 1.70f,       // secondary wave
                h * 0.015f, 10.8f, 0.40f);      // fine detail

        farMtnPaint.setStyle(Paint.Style.FILL);
        farMtnPaint.setShader(new LinearGradient(0, h * 0.66f, 0, h,
                new int[]{Color.argb(255, 10, 18, 32),
                        Color.argb(255,  2,  4,  9)},
                null, Shader.TileMode.CLAMP));

        // Near mountains: jagged prominent peaks, near-black fill
        nearMountainPath = waveMountainPath(w, h,
                h * 0.82f,                      // lower base (closer)
                h * 0.085f, 2.90f, 1.50f,       // primary wave
                h * 0.042f, 6.40f, 0.30f,       // secondary
                h * 0.022f, 14.0f, 2.20f);      // jagged detail

        nearMtnPaint.setStyle(Paint.Style.FILL);
        nearMtnPaint.setShader(new LinearGradient(0, h * 0.72f, 0, h,
                new int[]{Color.argb(255, 3, 6, 12),
                        Color.argb(255, 0, 0,  0)},
                null, Shader.TileMode.CLAMP));
    }

    /**
     * Builds a closed Path for a mountain silhouette using summed abs-sine waves.
     * Each wave adds distinct bumps/peaks for a natural-looking ridgeline.
     */
    private static Path waveMountainPath(int w, int h,
                                          float baseY,
                                          float a1, float f1, float p1,
                                          float a2, float f2, float p2,
                                          float a3, float f3, float p3) {
        Path path = new Path();
        int step = 3;

        // Left edge
        path.moveTo(-step, h);

        for (int x = -step; x <= w + step; x += step) {
            float nx = (float) x / w;
            float y  = baseY
                    - a1 * (float) Math.abs(Math.sin(Math.PI * f1 * nx + p1))
                    - a2 * (float) Math.abs(Math.sin(Math.PI * f2 * nx + p2))
                    - a3 * (float) Math.abs(Math.sin(Math.PI * f3 * nx + p3));
            if (x == -step) {
                path.lineTo(-step, y);
            } else {
                path.lineTo(x, y);
            }
        }

        path.lineTo(w + step, h);
        path.close();
        return path;
    }

    // ─── Stars ────────────────────────────────────────────────────────────

    private void buildStars(int w, int h) {
        stars.clear();
        float cx = w * 0.5f, cy = h * 0.5f;

        // Regular field stars distributed across the whole sky
        for (int i = 0; i < FIELD_STARS; i++) {
            float x = rng.nextFloat() * w;
            float y = rng.nextFloat() * h * 0.92f; // keep out of deep mountain zone
            stars.add(makeStar(x, y, cx, cy, false));
        }

        // Milky Way band: diagonal strip of faint, tiny stars
        for (int i = 0; i < MILKY_WAY_STARS; i++) {
            float t       = rng.nextFloat();
            float scatter = (rng.nextFloat() - 0.5f) * w * 0.26f;
            float x       = clamp(w * 0.05f + t * w * 0.90f + scatter * 0.35f, 0, w);
            float y       = clamp(h * 0.06f + t * h * 0.70f + scatter,         0, h * 0.90f);
            stars.add(makeStar(x, y, cx, cy, true));
        }
    }

    private Star makeStar(float x, float y, float cx, float cy, boolean milkyWay) {
        Star s = new Star();
        s.x = x;  s.y = y;
        float dx = x - cx, dy = y - cy;
        float d  = (float) Math.sqrt(dx * dx + dy * dy);
        s.nx = d > 0 ? dx / d : 0f;
        s.ny = d > 0 ? dy / d : 1f;

        if (milkyWay) {
            s.radius    = 0.3f + rng.nextFloat() * 0.85f;
            s.baseAlpha = 22 + rng.nextInt(60);
            s.color     = Color.rgb(195, 210, 255);
        } else {
            s.radius    = 0.5f + rng.nextFloat() * 2.7f;
            s.baseAlpha = 85 + rng.nextInt(170);
            float cr = rng.nextFloat();
            if      (cr < 0.22f) s.color = Color.rgb(150, 180, 255); // blue-white
            else if (cr < 0.38f) s.color = Color.rgb(255, 225, 165); // warm yellow
            else if (cr < 0.48f) s.color = Color.rgb(200, 215, 255); // pale blue
            else if (cr < 0.54f) s.color = Color.rgb(255, 195, 175); // red giant
            else                  s.color = Color.WHITE;
        }
        s.twinklePhase = rng.nextFloat() * 6.2832f;
        s.twinkleSpeed = 0.35f + rng.nextFloat() * 1.6f;
        return s;
    }

    // ─── Pre-warm meteor shower ───────────────────────────────────────────

    /** Spawn several meteors immediately so the scene starts with action. */
    private void prewarmMeteors(int w, int h) {
        meteors.clear();
        for (int i = 0; i < 5; i++) {
            // Stagger their spawn time so they appear at different positions
            Meteor m = buildMeteor(w, h);
            m.spawnMs -= rng.nextInt(1400); // artificially age some meteors
            meteors.add(m);
        }
        lastMeteorMs = System.currentTimeMillis();
    }

    // =====================================================================
    //  onDraw — runs at ~60 fps
    // =====================================================================

    @Override
    protected void onDraw(Canvas canvas) {
        if (!sizeReady) { invalidate(); return; }

        long  now     = System.currentTimeMillis();
        float dt      = lastFrameMs == 0 ? 0.016f : Math.min((now - lastFrameMs) / 1000f, 0.05f);
        lastFrameMs   = now;
        float elapsed = now / 1000f;

        float warpFactor = 0f;
        if (warpMode) {
            warpFactor = Math.min(1f, (now - warpStartMs) / 1900f);
        }

        int w = (int) screenW, h = (int) screenH;

        // ── 1. Sky gradient ───────────────────────────────────────────────
        canvas.drawRect(0, 0, w, h, skyPaint);

        // ── 2. Milky Way glow (rotated diagonal band) ─────────────────────
        canvas.save();
        canvas.translate(w * 0.5f, h * 0.40f);
        canvas.rotate(-52f);
        canvas.drawRect(mwRect, mwGlowPaint);
        canvas.restore();

        // ── 3. Atmospheric horizon glow ───────────────────────────────────
        canvas.drawRect(0, 0, w, h, atmoGlowPaint);

        // ── 4. Stars ──────────────────────────────────────────────────────
        for (Star s : stars) {
            float twinkle = (float)(0.55 + 0.45 * Math.sin(elapsed * s.twinkleSpeed + s.twinklePhase));
            int   alpha   = (int)(s.baseAlpha * twinkle);

            if (warpMode && warpFactor > 0f) {
                // Quadratic acceleration — stars near centre move slowly,
                // stars at edges fly fast (perspective-correct depth feel)
                float speed = warpFactor * warpFactor * 3600f;
                s.x += s.nx * speed * dt;
                s.y += s.ny * speed * dt;

                // Simple opaque trail — no per-star gradient allocation
                float trailLen = warpFactor * warpFactor * 70f;
                trailPaint.setColor(Color.argb(alpha / 3, 255, 255, 255));
                trailPaint.setStrokeWidth(Math.max(0.5f, s.radius * 0.65f));
                canvas.drawLine(
                        s.x - s.nx * trailLen, s.y - s.ny * trailLen,
                        s.x, s.y, trailPaint);

                starPaint.setColor(s.color);
                starPaint.setAlpha(alpha);
                canvas.drawCircle(s.x, s.y, s.radius * (1f + warpFactor * 0.6f), starPaint);

            } else {
                starPaint.setColor(s.color);
                starPaint.setAlpha(alpha);
                canvas.drawCircle(s.x, s.y, s.radius, starPaint);

                // Four-pointed cross flare on the brightest stars
                if (s.radius > 1.85f) {
                    starPaint.setAlpha(alpha / 6);
                    starPaint.setStyle(Paint.Style.STROKE);
                    starPaint.setStrokeWidth(0.7f);
                    float fl = s.radius * 5.5f;
                    canvas.drawLine(s.x - fl, s.y,      s.x + fl, s.y,      starPaint);
                    canvas.drawLine(s.x,      s.y - fl, s.x,      s.y + fl, starPaint);
                    starPaint.setStyle(Paint.Style.FILL);
                }
            }
        }

        // ── 5. Meteors (disabled once warp is well underway) ─────────────
        if (!warpMode || warpFactor < 0.15f) {
            // Spawn
            if (meteors.size() < MAX_METEORS && now - lastMeteorMs > METEOR_INTERVAL_MS) {
                meteors.add(buildMeteor(w, h));
                // Random extra jitter so they don't all appear at once
                lastMeteorMs = now + (long)(rng.nextFloat() * 300);
            }
            drawMeteors(canvas, now, w, h, dt);
        }

        // ── 6. Mountain silhouettes ───────────────────────────────────────
        // Far range — dark navy gradient
        canvas.drawPath(farMountainPath, farMtnPaint);
        // Subtle starlight on the far ridgeline
        ridgePaint.setColor(Color.argb(55, 35, 65, 105));
        canvas.drawPath(farMountainPath, ridgePaint);

        // Near range — near-black gradient
        canvas.drawPath(nearMountainPath, nearMtnPaint);
        // Very faint blue on near ridgeline (moonlight/starlight effect)
        ridgePaint.setColor(Color.argb(40, 20, 40, 70));
        canvas.drawPath(nearMountainPath, ridgePaint);

        invalidate();
    }

    // =====================================================================
    //  Meteor helpers
    // =====================================================================

    private Meteor buildMeteor(int w, int h) {
        Meteor m  = new Meteor();
        // Angle: 25°–65° below horizontal (varies so not all look the same)
        double ang = Math.toRadians(25 + rng.nextFloat() * 40);
        m.speed    = 750f + rng.nextFloat() * 950f;     // 750–1700 px/s
        m.vx       = (float)(Math.cos(ang) * m.speed);
        m.vy       = (float)(Math.sin(ang) * m.speed);

        // Start above the visible sky, spread across the full width
        m.headX     = w * 0.05f + rng.nextFloat() * w * 0.90f;
        m.headY     = -50f - rng.nextFloat() * 150f;
        m.tailLength = 260f + rng.nextFloat() * 340f;   // very long tails: 260–600 px
        m.width      = 1.4f + rng.nextFloat() * 2.4f;
        m.lifetime   = 0.8f + rng.nextFloat() * 1.4f;  // 0.8–2.2 s → slow enough to admire
        m.spawnMs    = System.currentTimeMillis();
        return m;
    }

    private void drawMeteors(Canvas canvas, long now, int w, int h, float dt) {
        Iterator<Meteor> it = meteors.iterator();
        while (it.hasNext()) {
            Meteor m   = it.next();
            float  age = (now - m.spawnMs) / 1000f;

            if (age > m.lifetime || m.headX > w + 600 || m.headY > h + 600) {
                it.remove();
                continue;
            }

            m.headX += m.vx * dt;
            m.headY += m.vy * dt;

            float dirX  = m.vx / m.speed;
            float dirY  = m.vy / m.speed;
            float tailX = m.headX - dirX * m.tailLength;
            float tailY = m.headY - dirY * m.tailLength;

            // Envelope: fast fade-in (0.06 s), hold, fade-out last 30%
            float fadeIn  = Math.min(1f, age / 0.06f);
            float fadeOut = age > m.lifetime * 0.70f
                    ? 1f - (age - m.lifetime * 0.70f) / (m.lifetime * 0.30f)
                    : 1f;
            float a01  = fadeIn * fadeOut;
            int   aInt = (int)(a01 * 250);

            // ── Gradient tail (LinearGradient — only 4–9 meteors max) ──
            meteorPaint.setShader(new LinearGradient(
                    tailX, tailY, m.headX, m.headY,
                    Color.TRANSPARENT, Color.argb(aInt, 255, 255, 255),
                    Shader.TileMode.CLAMP));
            meteorPaint.setStrokeWidth(m.width);
            canvas.drawLine(tailX, tailY, m.headX, m.headY, meteorPaint);
            meteorPaint.setShader(null);

            // Outer halo (blue-white glow around the head)
            glowPaint.setColor(Color.argb((int)(a01 * 80), 160, 200, 255));
            canvas.drawCircle(m.headX, m.headY, m.width * 6f, glowPaint);
            // Bright inner core
            glowPaint.setColor(Color.argb(aInt, 255, 255, 248));
            canvas.drawCircle(m.headX, m.headY, m.width * 2f, glowPaint);
        }
    }

    // =====================================================================
    //  Utilities
    // =====================================================================

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    // =====================================================================
    //  Data
    // =====================================================================

    private static class Star {
        float x, y;
        float nx, ny;   // warp direction (unit vector from screen centre)
        float radius, baseAlpha, twinklePhase, twinkleSpeed;
        int   color;
    }

    private static class Meteor {
        float headX, headY;
        float vx, vy, speed;
        float tailLength, width, lifetime;
        long  spawnMs;
    }
}
