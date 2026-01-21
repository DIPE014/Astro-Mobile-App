package com.astro.app.ui.skymap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import com.astro.app.common.model.StarData;
import java.util.ArrayList;
import java.util.List;

/**
 * FRONTEND - Person A
 *
 * Custom view that draws stars on top of camera preview.
 */
public class SkyOverlayView extends View {

    private Paint starPaint;
    private Paint labelPaint;
    private List<StarData> visibleStars = new ArrayList<>();

    public SkyOverlayView(Context context) {
        super(context);
        init();
    }

    public SkyOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Star rendering paint
        starPaint = new Paint();
        starPaint.setColor(Color.WHITE);
        starPaint.setStyle(Paint.Style.FILL);
        starPaint.setAntiAlias(true);

        // Label rendering paint
        labelPaint = new Paint();
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(32f);
        labelPaint.setAntiAlias(true);
    }

    /**
     * Called by Activity to update visible stars.
     * Backend calculates stars, Frontend displays them.
     */
    public void updateStars(List<StarData> stars) {
        this.visibleStars = stars;
        invalidate(); // Trigger redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (StarData star : visibleStars) {
            if (!star.isVisible) continue;

            // Calculate star size based on magnitude (brighter = bigger)
            float size = Math.max(2f, 8f - star.magnitude);

            // Draw star
            starPaint.setColor(star.color != 0 ? star.color : Color.WHITE);
            canvas.drawCircle(star.screenX, star.screenY, size, starPaint);

            // Draw label for bright stars
            if (star.magnitude < 2.0f && star.name != null) {
                canvas.drawText(star.name, star.screenX + size + 5, star.screenY, labelPaint);
            }
        }
    }
}
