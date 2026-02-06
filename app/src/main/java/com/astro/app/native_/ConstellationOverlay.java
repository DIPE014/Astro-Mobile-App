package com.astro.app.native_;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws constellation lines and labels on a solved image.
 */
public class ConstellationOverlay {
    private static final String TAG = "ConstellationOverlay";

    private double[][] stars;           // [i] = {ra, dec}
    private List<Constellation> constellations;
    private Map<String, String> nameMap; // abbreviation -> full name

    private static class Constellation {
        String name;
        int[] lines; // pairs: [start0, end0, start1, end1, ...]
    }

    /**
     * Load constellation line data from assets.
     */
    public void loadConstellations(Context context) {
        try {
            // Load line data
            String json = readAsset(context, "constellation_lines.json");
            JSONObject root = new JSONObject(json);

            // Parse stars
            JSONArray starsArray = root.getJSONArray("stars");
            stars = new double[starsArray.length()][2];
            for (int i = 0; i < starsArray.length(); i++) {
                JSONArray pos = starsArray.getJSONArray(i);
                stars[i][0] = pos.getDouble(0); // RA degrees
                stars[i][1] = pos.getDouble(1); // Dec degrees
            }

            // Parse constellations
            JSONArray constArray = root.getJSONArray("constellations");
            constellations = new ArrayList<>();
            for (int i = 0; i < constArray.length(); i++) {
                JSONObject cObj = constArray.getJSONObject(i);
                Constellation c = new Constellation();
                c.name = cObj.getString("name");
                JSONArray linesArr = cObj.getJSONArray("lines");
                c.lines = new int[linesArr.length()];
                for (int j = 0; j < linesArr.length(); j++) {
                    c.lines[j] = linesArr.getInt(j);
                }
                constellations.add(c);
            }

            // Load name map (abbreviation -> full name)
            nameMap = new HashMap<>();
            String namesJson = readAsset(context, "constellations.json");
            JSONObject namesObj = new JSONObject(namesJson);
            Iterator<String> keys = namesObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                nameMap.put(key, namesObj.getString(key));
            }

            Log.i(TAG, "Loaded " + stars.length + " stars, " +
                    constellations.size() + " constellations");

        } catch (Exception e) {
            Log.e(TAG, "Failed to load constellation data", e);
        }
    }

    private String readAsset(Context context, String path) throws Exception {
        InputStream is = context.getAssets().open(path);
        byte[] data = new byte[is.available()];
        is.read(data);
        is.close();
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Draw constellation overlay on image.
     * Returns a new bitmap with overlay drawn on top.
     */
    public Bitmap drawOverlay(Bitmap original, AstrometryNative.SolveResult result) {
        if (stars == null || constellations == null) {
            Log.w(TAG, "Constellation data not loaded");
            return original;
        }

        Bitmap overlay = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(overlay);
        int w = overlay.getWidth();
        int h = overlay.getHeight();

        WcsProjection wcs = new WcsProjection(result);

        // Line paint
        Paint linePaint = new Paint();
        linePaint.setColor(Color.argb(180, 0, 220, 100)); // green, semi-transparent
        linePaint.setStrokeWidth(3f);
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.STROKE);

        // Label paint
        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.argb(220, 255, 255, 100)); // yellow
        labelPaint.setTextSize(36f);
        labelPaint.setAntiAlias(true);
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // Star dot paint
        Paint dotPaint = new Paint();
        dotPaint.setColor(Color.argb(200, 0, 220, 100));
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setAntiAlias(true);

        // Project all stars to pixel coords (cache)
        double[][] projected = new double[stars.length][];
        for (int i = 0; i < stars.length; i++) {
            projected[i] = wcs.radecToPixel(stars[i][0], stars[i][1]);
        }

        for (Constellation c : constellations) {
            double sumX = 0, sumY = 0;
            int visCount = 0;

            // Draw lines
            for (int j = 0; j + 1 < c.lines.length; j += 2) {
                int idx1 = c.lines[j];
                int idx2 = c.lines[j + 1];

                if (idx1 < 0 || idx1 >= stars.length || idx2 < 0 || idx2 >= stars.length)
                    continue;

                double[] p1 = projected[idx1];
                double[] p2 = projected[idx2];

                if (p1 == null || p2 == null) continue;

                boolean p1on = WcsProjection.isOnImage(p1[0], p1[1], w, h);
                boolean p2on = WcsProjection.isOnImage(p2[0], p2[1], w, h);

                if (!p1on && !p2on) continue;

                canvas.drawLine((float) p1[0], (float) p1[1],
                        (float) p2[0], (float) p2[1], linePaint);

                // Draw small dots at star positions
                if (p1on) {
                    canvas.drawCircle((float) p1[0], (float) p1[1], 5f, dotPaint);
                    sumX += p1[0];
                    sumY += p1[1];
                    visCount++;
                }
                if (p2on) {
                    canvas.drawCircle((float) p2[0], (float) p2[1], 5f, dotPaint);
                    sumX += p2[0];
                    sumY += p2[1];
                    visCount++;
                }
            }

            // Draw label at centroid of visible stars
            if (visCount >= 2) {
                float cx = (float) (sumX / visCount);
                float cy = (float) (sumY / visCount);
                // Only label if centroid is on image
                if (cx >= 0 && cx < w && cy >= 0 && cy < h) {
                    String label = nameMap != null ?
                            nameMap.getOrDefault(c.name, c.name) : c.name;
                    canvas.drawText(label, cx + 10, cy - 10, labelPaint);
                }
            }
        }

        return overlay;
    }
}
