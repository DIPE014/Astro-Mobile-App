package com.astro.app.ui.intro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class StarFieldView extends View implements Runnable {

    public interface OnWarpCompleteListener {
        void onWarpComplete();
    }

    private static final int STAR_COUNT = 220;
    private static final int METEOR_POOL_SIZE = 7;
    private static final int BACKGROUND_COLOR = Color.parseColor("#000005");
    private static final float METEOR_MIN_LIFESPAN = 0.4f;
    private static final float METEOR_MAX_LIFESPAN = 1.0f;
    private static final float METEOR_MIN_INTERVAL = 0.3f;
    private static final float METEOR_MAX_INTERVAL = 0.8f;
    private static final float METEOR_TAIL_LENGTH = 120f;
    private static final float METEOR_HEAD_RADIUS = 3f;
    private static final float WARP_DURATION = 3.0f;
    private static final float WARP_FLASH_START = 0.85f;

    // Star arrays
    private float[] starX;
    private float[] starY;
    private float[] starRadius;
    private float[] starBaseAlpha;
    private float[] starPhase;
    private float[] starFrequency;
    // Warp-mode per-star direction vectors
    private float[] starDirX;
    private float[] starDirY;
    private float[] starOrigX;
    private float[] starOrigY;

    private final List<Meteor> meteors = new ArrayList<>();
    private final Random random = new Random();

    private final Paint starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint meteorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint warpLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flashPaint = new Paint();

    private long lastNanoTime;
    private float elapsedTime;
    private float meteorSpawnTimer;
    private float nextMeteorInterval;

    private boolean animating;
    private boolean initialized;

    // Warp mode
    private boolean warpMode;
    private float warpProgress;
    private boolean warpCompleteNotified;
    private boolean pendingWarpComplete = false;
    private OnWarpCompleteListener warpCompleteListener;

    private int viewWidth;
    private int viewHeight;

    public StarFieldView(Context context) {
        super(context);
        init();
    }

    public StarFieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StarFieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        starPaint.setStyle(Paint.Style.FILL);
        meteorPaint.setStyle(Paint.Style.FILL);
        warpLinePaint.setStyle(Paint.Style.STROKE);
        warpLinePaint.setStrokeCap(Paint.Cap.ROUND);
        flashPaint.setStyle(Paint.Style.FILL);
        nextMeteorInterval = randomRange(METEOR_MIN_INTERVAL, METEOR_MAX_INTERVAL);
    }

    public void setOnWarpCompleteListener(OnWarpCompleteListener listener) {
        this.warpCompleteListener = listener;
    }

    public void startWarpMode() {
        if (warpMode) return;
        warpMode = true;
        warpProgress = 0f;
        warpCompleteNotified = false;

        if (initialized) {
            float cx = viewWidth / 2f;
            float cy = viewHeight / 2f;
            for (int i = 0; i < STAR_COUNT; i++) {
                starOrigX[i] = starX[i];
                starOrigY[i] = starY[i];
                float dx = starX[i] - cx;
                float dy = starY[i] - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < 1f) dist = 1f;
                starDirX[i] = dx / dist;
                starDirY[i] = dy / dist;
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWidth = w;
        viewHeight = h;
        if (w > 0 && h > 0) {
            initializeStars();
            initialized = true;
            if (!animating) {
                animating = true;
                lastNanoTime = System.nanoTime();
                postOnAnimation(this);
            }
        }
    }

    private void initializeStars() {
        starX = new float[STAR_COUNT];
        starY = new float[STAR_COUNT];
        starRadius = new float[STAR_COUNT];
        starBaseAlpha = new float[STAR_COUNT];
        starPhase = new float[STAR_COUNT];
        starFrequency = new float[STAR_COUNT];
        starDirX = new float[STAR_COUNT];
        starDirY = new float[STAR_COUNT];
        starOrigX = new float[STAR_COUNT];
        starOrigY = new float[STAR_COUNT];

        float cx = viewWidth / 2f;
        float cy = viewHeight / 2f;

        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] = random.nextFloat() * viewWidth;
            starY[i] = random.nextFloat() * viewHeight;
            starRadius[i] = 1f + random.nextFloat() * 3f;
            starBaseAlpha[i] = 0.5f + random.nextFloat() * 0.5f;
            starPhase[i] = random.nextFloat() * (float) (2 * Math.PI);
            starFrequency[i] = 0.5f + random.nextFloat() * 2.0f;

            starOrigX[i] = starX[i];
            starOrigY[i] = starY[i];
            float dx = starX[i] - cx;
            float dy = starY[i] - cy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < 1f) dist = 1f;
            starDirX[i] = dx / dist;
            starDirY[i] = dy / dist;
        }
    }

    @Override
    public void run() {
        if (!animating) return;

        long now = System.nanoTime();
        float dt = (now - lastNanoTime) / 1_000_000_000f;
        lastNanoTime = now;
        // Clamp delta time to avoid huge jumps
        if (dt > 0.1f) dt = 0.1f;

        elapsedTime += dt;

        if (warpMode && warpProgress < 1f) {
            warpProgress += dt / WARP_DURATION;
            if (warpProgress >= 1f) {
                warpProgress = 1f;
                if (!warpCompleteNotified) {
                    if (isAttachedToWindow() && warpCompleteListener != null) {
                        warpCompleteNotified = true;
                        warpCompleteListener.onWarpComplete();
                    } else {
                        pendingWarpComplete = true;
                    }
                }
            }
        }

        updateMeteors(dt);
        if (warpMode) {
            updateWarpStars(dt);
        }

        invalidate();
        postOnAnimation(this);
    }

    private void updateMeteors(float dt) {
        // Update existing meteors
        Iterator<Meteor> it = meteors.iterator();
        while (it.hasNext()) {
            Meteor m = it.next();
            m.age += dt;
            m.x += m.vx * dt;
            m.y += m.vy * dt;
            if (m.age >= m.lifespan) {
                it.remove();
            }
        }

        // Spawn new meteors
        float intervalMultiplier = warpMode ? Math.max(0.1f, 1f - warpProgress * 0.8f) : 1f;
        meteorSpawnTimer += dt;
        if (meteorSpawnTimer >= nextMeteorInterval * intervalMultiplier
                && meteors.size() < METEOR_POOL_SIZE) {
            spawnMeteor();
            meteorSpawnTimer = 0f;
            nextMeteorInterval = randomRange(METEOR_MIN_INTERVAL, METEOR_MAX_INTERVAL);
        }
    }

    private void spawnMeteor() {
        if (viewWidth <= 0 || viewHeight <= 0) return;

        Meteor m = new Meteor();

        // Spawn at top or sides
        int side = random.nextInt(3);
        if (side == 0) {
            // Top edge
            m.x = random.nextFloat() * viewWidth;
            m.y = -10f;
        } else if (side == 1) {
            // Left side (upper half)
            m.x = -10f;
            m.y = random.nextFloat() * viewHeight * 0.4f;
        } else {
            // Right side (upper half)
            m.x = viewWidth + 10f;
            m.y = random.nextFloat() * viewHeight * 0.4f;
        }

        // Angle: 30-60 degrees from vertical, random left or right
        float angleDeg = 30f + random.nextFloat() * 30f;
        boolean goRight = (side == 2) ? false : (side == 1) ? true : random.nextBoolean();
        float angleRad = (float) Math.toRadians(angleDeg);

        float speed = viewHeight * (0.8f + random.nextFloat() * 0.6f);
        m.vx = (goRight ? 1f : -1f) * (float) Math.sin(angleRad) * speed;
        m.vy = (float) Math.cos(angleRad) * speed;

        m.lifespan = randomRange(METEOR_MIN_LIFESPAN, METEOR_MAX_LIFESPAN);
        m.age = 0f;
        m.tailLength = randomRange(80f, 150f);

        meteors.add(m);
    }

    private void updateWarpStars(float dt) {
        float cx = viewWidth / 2f;
        float cy = viewHeight / 2f;
        float accel = warpProgress * warpProgress;
        float speed = 200f + 1200f * accel;

        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] += starDirX[i] * speed * dt;
            starY[i] += starDirY[i] * speed * dt;

            // Recycle stars that exit bounds
            if (starX[i] < -50 || starX[i] > viewWidth + 50
                    || starY[i] < -50 || starY[i] > viewHeight + 50) {
                // Respawn near center with new random direction
                float angle = random.nextFloat() * (float) (2 * Math.PI);
                float offset = 5f + random.nextFloat() * 30f;
                starX[i] = cx + (float) Math.cos(angle) * offset;
                starY[i] = cy + (float) Math.sin(angle) * offset;
                starOrigX[i] = starX[i];
                starOrigY[i] = starY[i];
                starDirX[i] = (float) Math.cos(angle);
                starDirY[i] = (float) Math.sin(angle);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(BACKGROUND_COLOR);

        if (!initialized) return;

        if (warpMode) {
            drawWarpStars(canvas);
        } else {
            drawNormalStars(canvas);
        }

        drawMeteors(canvas);

        // White flash overlay at end of warp
        if (warpMode && warpProgress > WARP_FLASH_START) {
            float flashFraction = (warpProgress - WARP_FLASH_START) / (1f - WARP_FLASH_START);
            int flashAlpha = Math.min(200, (int) (flashFraction * 200));
            flashPaint.setColor(Color.argb(flashAlpha, 255, 255, 255));
            canvas.drawRect(0, 0, viewWidth, viewHeight, flashPaint);
        }
    }

    private void drawNormalStars(Canvas canvas) {
        for (int i = 0; i < STAR_COUNT; i++) {
            float twinkle = 0.5f + 0.5f * (float) Math.sin(
                    elapsedTime * starFrequency[i] * 2 * Math.PI + starPhase[i]);
            float alpha = starBaseAlpha[i] * twinkle;
            int alphaInt = Math.max(0, Math.min(255, (int) (alpha * 255)));

            // Glow for brighter stars
            if (starRadius[i] > 2.5f) {
                drawStarGlow(canvas, starX[i], starY[i], starRadius[i], alphaInt);
            }

            starPaint.setColor(Color.argb(alphaInt, 255, 255, 255));
            canvas.drawCircle(starX[i], starY[i], starRadius[i], starPaint);
        }
    }

    private void drawWarpStars(Canvas canvas) {
        float cx = viewWidth / 2f;
        float cy = viewHeight / 2f;
        float accel = warpProgress * warpProgress;

        for (int i = 0; i < STAR_COUNT; i++) {
            float twinkle = 0.5f + 0.5f * (float) Math.sin(
                    elapsedTime * starFrequency[i] * 2 * Math.PI + starPhase[i]);
            float alpha = starBaseAlpha[i] * twinkle;

            // Stars become brighter and bluer as warp progresses
            float brightBoost = 1f + warpProgress * 1.5f;
            alpha = Math.min(1f, alpha * brightBoost);
            int alphaInt = Math.max(0, Math.min(255, (int) (alpha * 255)));

            int r = (int) (255 * (1f - warpProgress * 0.4f));
            int g = (int) (255 * (1f - warpProgress * 0.2f));
            int b = 255;

            // Trail length increases with warp progress
            float trailLen = 5f + 195f * accel;
            float tailX = starX[i] - starDirX[i] * trailLen;
            float tailY = starY[i] - starDirY[i] * trailLen;

            // Draw streak line
            warpLinePaint.setColor(Color.argb(alphaInt, r, g, b));
            warpLinePaint.setStrokeWidth(Math.max(1f, starRadius[i] * 0.8f));
            canvas.drawLine(tailX, tailY, starX[i], starY[i], warpLinePaint);

            // Draw head
            starPaint.setColor(Color.argb(alphaInt, 255, 255, 255));
            canvas.drawCircle(starX[i], starY[i], starRadius[i] * 0.8f, starPaint);

            // Glow for brighter stars
            if (starRadius[i] > 2.5f) {
                drawStarGlow(canvas, starX[i], starY[i], starRadius[i], alphaInt);
            }
        }
    }

    private void drawStarGlow(Canvas canvas, float x, float y, float radius, int alphaInt) {
        float glowRadius = radius * 3f;
        int coreAlpha = Math.min(255, alphaInt);
        int edgeAlpha = 0;
        int blueAlpha = Math.max(0, Math.min(255, alphaInt / 2));

        RadialGradient gradient = new RadialGradient(
                x, y, glowRadius,
                new int[]{
                        Color.argb(coreAlpha, 255, 255, 255),
                        Color.argb(blueAlpha, 100, 150, 255),
                        Color.argb(edgeAlpha, 100, 150, 255)
                },
                new float[]{0f, 0.4f, 1f},
                Shader.TileMode.CLAMP
        );
        glowPaint.setShader(gradient);
        canvas.drawCircle(x, y, glowRadius, glowPaint);
        glowPaint.setShader(null);
    }

    private void drawMeteors(Canvas canvas) {
        for (Meteor m : meteors) {
            float lifeFraction = m.age / m.lifespan;
            float overallAlpha = 1f - lifeFraction;

            // Head
            int headAlpha = Math.max(0, Math.min(255, (int) (overallAlpha * 255)));
            meteorPaint.setColor(Color.argb(headAlpha, 255, 255, 255));
            canvas.drawCircle(m.x, m.y, METEOR_HEAD_RADIUS, meteorPaint);

            // Tail: series of circles with decreasing alpha
            float tailLen = m.tailLength;
            int segments = 20;
            float dx = -m.vx;
            float dy = -m.vy;
            float vMag = (float) Math.sqrt(dx * dx + dy * dy);
            if (vMag < 1f) continue;
            dx /= vMag;
            dy /= vMag;

            for (int s = 1; s <= segments; s++) {
                float t = s / (float) segments;
                float px = m.x + dx * tailLen * t;
                float py = m.y + dy * tailLen * t;
                float segAlpha = overallAlpha * (1f - t);
                int segAlphaInt = Math.max(0, Math.min(255, (int) (segAlpha * 200)));
                float segRadius = METEOR_HEAD_RADIUS * (1f - t * 0.7f);
                meteorPaint.setColor(Color.argb(segAlphaInt, 255, 255, 255));
                canvas.drawCircle(px, py, segRadius, meteorPaint);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (pendingWarpComplete) {
            pendingWarpComplete = false;
            warpCompleteNotified = true;
            if (warpCompleteListener != null) {
                warpCompleteListener.onWarpComplete();
            }
        }
        if (initialized && !animating) {
            animating = true;
            lastNanoTime = System.nanoTime();
            postOnAnimation(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        animating = false;
        super.onDetachedFromWindow();
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private static class Meteor {
        float x, y;
        float vx, vy;
        float age;
        float lifespan;
        float tailLength;
    }
}
