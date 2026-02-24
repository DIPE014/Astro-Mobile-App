package com.astro.app.ui.skymap;

import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves constellation names by testing whether a star RA/Dec point falls inside
 * polygons from bound.json (IAU boundaries).
 */
public final class ConstellationBoundaryResolver {
    private static final String TAG = "ConstBoundaryResolver";
    private static final String BOUND_ASSET_PRIMARY = "bound_edges_18.json";
    private static final String BOUND_ASSET_FALLBACK = "bound.json";
    private static final String NAMES_ASSET = "constellations.json";

    private final List<BoundaryPolygon> polygons;

    private ConstellationBoundaryResolver(@NonNull List<BoundaryPolygon> polygons) {
        this.polygons = polygons;
    }

    @Nullable
    public static ConstellationBoundaryResolver fromAssets(@NonNull AssetManager assets) {
        try {
            String boundJson = readBoundaryAsset(assets);
            String namesJson = readAsset(assets, NAMES_ASSET);
            JSONObject boundRoot = new JSONObject(boundJson);
            JSONObject namesRoot = new JSONObject(namesJson);
            Map<String, String> codeToName = parseCodeToName(namesRoot);

            List<BoundaryPolygon> parsed = parsePolygons(boundRoot, codeToName);
            if (parsed.isEmpty()) {
                return null;
            }

            Log.d(TAG, "Loaded " + parsed.size() + " boundary polygons");
            return new ConstellationBoundaryResolver(parsed);
        } catch (Exception e) {
            Log.w(TAG, "Failed to load boundary assets", e);
            return null;
        }
    }

    @Nullable
    public String findConstellationName(double raDeg, double decDeg) {
        for (BoundaryPolygon polygon : polygons) {
            if (pointInPolygon(raDeg, decDeg, polygon.points)) {
                return polygon.name;
            }
        }
        return null;
    }

    @NonNull
    private static Map<String, String> parseCodeToName(@NonNull JSONObject namesRoot) {
        Map<String, String> codeToName = new HashMap<>();
        Iterator<String> keys = namesRoot.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = namesRoot.optString(key, "").trim();
            if (value.isEmpty()) continue;
            codeToName.put(key.toUpperCase(Locale.ROOT), value);
        }
        return codeToName;
    }

    @NonNull
    private static String resolveName(@NonNull String rawCode, @NonNull Map<String, String> codeToName) {
        String normalizedCode = rawCode;
        // Handle Serpens split segments in boundary data.
        if ("SER1".equals(rawCode) || "SER2".equals(rawCode)) {
            normalizedCode = "SER";
        }

        String name = codeToName.get(normalizedCode);
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return rawCode;
    }

    @NonNull
    private static List<BoundaryPolygon> parsePolygons(@NonNull JSONObject boundRoot,
                                                       @NonNull Map<String, String> codeToName) {
        // Newest schema: bound_edges_18.json
        JSONArray edgesArray = boundRoot.optJSONArray("edges");
        if (edgesArray != null && edgesArray.length() > 0) {
            return parseFromEdges(edgesArray, codeToName);
        }

        // New schema: bound_in_18.json
        JSONArray constellations = boundRoot.optJSONArray("constellations");
        if (constellations != null && constellations.length() > 0) {
            List<BoundaryPolygon> result = new ArrayList<>(constellations.length());
            for (int i = 0; i < constellations.length(); i++) {
                JSONObject cObj = constellations.optJSONObject(i);
                if (cObj == null) continue;

                String rawCode = cObj.optString("id", "").trim().toUpperCase(Locale.ROOT);
                if (rawCode.isEmpty()) continue;

                JSONArray segments = cObj.optJSONArray("segments");
                if (segments == null || segments.length() == 0) continue;

                List<Point> points = new ArrayList<>();
                for (int s = 0; s < segments.length(); s++) {
                    JSONObject segObj = segments.optJSONObject(s);
                    if (segObj == null) continue;
                    JSONArray pts = segObj.optJSONArray("points");
                    if (pts == null) continue;
                    for (int j = 0; j < pts.length(); j++) {
                        JSONObject p = pts.optJSONObject(j);
                        if (p == null) continue;
                        double raDeg = p.optDouble("ra_deg", Double.NaN);
                        if (Double.isNaN(raDeg)) {
                            double raH = p.optDouble("ra_hours", Double.NaN);
                            if (Double.isNaN(raH)) {
                                raH = p.optDouble("ra_h", Double.NaN);
                            }
                            if (!Double.isNaN(raH)) {
                                raDeg = raH * 15.0;
                            }
                        }
                        double decDeg = p.optDouble("dec_deg", Double.NaN);
                        if (Double.isNaN(raDeg) || Double.isNaN(decDeg)) continue;
                        points.add(new Point(raDeg, decDeg));
                    }
                }

                if (points.size() < 3) continue;
                // Ensure ring is closed for stable point-in-polygon behavior.
                Point first = points.get(0);
                Point last = points.get(points.size() - 1);
                if (Math.abs(first.raDeg - last.raDeg) > 1e-6 || Math.abs(first.decDeg - last.decDeg) > 1e-6) {
                    points.add(new Point(first.raDeg, first.decDeg));
                }

                String name = resolveName(rawCode, codeToName);
                result.add(new BoundaryPolygon(rawCode, name, points));
            }
            return result;
        }

        // Legacy schema: bound.json
        JSONArray polygonsArray = boundRoot.optJSONArray("polygons");
        if (polygonsArray == null || polygonsArray.length() == 0) {
            return new ArrayList<>();
        }
        List<BoundaryPolygon> result = new ArrayList<>(polygonsArray.length());
        for (int i = 0; i < polygonsArray.length(); i++) {
            JSONObject polygonObj = polygonsArray.optJSONObject(i);
            if (polygonObj == null) continue;

            String rawCode = polygonObj.optString("constellation", "").trim().toUpperCase(Locale.ROOT);
            if (rawCode.isEmpty()) continue;

            JSONArray pointsArray = polygonObj.optJSONArray("points");
            if (pointsArray == null || pointsArray.length() < 3) continue;

            List<Point> points = new ArrayList<>(pointsArray.length());
            for (int j = 0; j < pointsArray.length(); j++) {
                JSONObject p = pointsArray.optJSONObject(j);
                if (p == null) continue;
                // bound.json stores RA in hours.
                double raH = p.optDouble("ra_h", Double.NaN);
                double raDeg = Double.isNaN(raH) ? p.optDouble("ra_deg", Double.NaN) : raH * 15.0;
                double decDeg = p.optDouble("dec_deg", Double.NaN);
                if (Double.isNaN(raDeg) || Double.isNaN(decDeg)) continue;
                points.add(new Point(raDeg, decDeg));
            }
            if (points.size() < 3) continue;

            String name = resolveName(rawCode, codeToName);
            result.add(new BoundaryPolygon(rawCode, name, points));
        }
        return result;
    }

    @NonNull
    private static List<BoundaryPolygon> parseFromEdges(@NonNull JSONArray edgesArray,
                                                        @NonNull Map<String, String> codeToName) {
        Map<String, List<EdgeSegment>> perConstellation = new HashMap<>();

        for (int i = 0; i < edgesArray.length(); i++) {
            JSONObject e = edgesArray.optJSONObject(i);
            if (e == null) continue;

            int from = e.optInt("from", Integer.MIN_VALUE);
            int to = e.optInt("to", Integer.MIN_VALUE);
            JSONObject p1Obj = e.optJSONObject("p1");
            JSONObject p2Obj = e.optJSONObject("p2");
            if (from == Integer.MIN_VALUE || to == Integer.MIN_VALUE || p1Obj == null || p2Obj == null) {
                continue;
            }

            double p1Ra = p1Obj.optDouble("ra_deg", Double.NaN);
            double p1Dec = p1Obj.optDouble("dec_deg", Double.NaN);
            double p2Ra = p2Obj.optDouble("ra_deg", Double.NaN);
            double p2Dec = p2Obj.optDouble("dec_deg", Double.NaN);
            if (Double.isNaN(p1Ra) || Double.isNaN(p1Dec) || Double.isNaN(p2Ra) || Double.isNaN(p2Dec)) {
                continue;
            }

            String left = e.optString("left", "").trim().toUpperCase(Locale.ROOT);
            String right = e.optString("right", "").trim().toUpperCase(Locale.ROOT);

            if (!left.isEmpty()) {
                perConstellation.computeIfAbsent(left, k -> new ArrayList<>())
                        .add(new EdgeSegment(from, to, new Point(p1Ra, p1Dec), new Point(p2Ra, p2Dec)));
            }
            if (!right.isEmpty()) {
                // Reverse edge so this constellation is consistently on the left side.
                perConstellation.computeIfAbsent(right, k -> new ArrayList<>())
                        .add(new EdgeSegment(to, from, new Point(p2Ra, p2Dec), new Point(p1Ra, p1Dec)));
            }
        }

        List<BoundaryPolygon> result = new ArrayList<>();
        for (Map.Entry<String, List<EdgeSegment>> entry : perConstellation.entrySet()) {
            String code = entry.getKey();
            List<List<Point>> loops = buildLoops(entry.getValue());
            String name = resolveName(code, codeToName);
            for (List<Point> loop : loops) {
                if (loop.size() < 3) continue;
                Point first = loop.get(0);
                Point last = loop.get(loop.size() - 1);
                if (Math.abs(first.raDeg - last.raDeg) > 1e-6 || Math.abs(first.decDeg - last.decDeg) > 1e-6) {
                    loop.add(new Point(first.raDeg, first.decDeg));
                }
                result.add(new BoundaryPolygon(code, name, loop));
            }
        }

        return result;
    }

    @NonNull
    private static List<List<Point>> buildLoops(@NonNull List<EdgeSegment> segments) {
        List<EdgeSegment> remaining = new ArrayList<>(segments);
        List<List<Point>> loops = new ArrayList<>();

        while (!remaining.isEmpty()) {
            EdgeSegment first = remaining.remove(remaining.size() - 1);
            List<Point> loop = new ArrayList<>();
            loop.add(first.p1);
            loop.add(first.p2);

            int startNode = first.fromNode;
            int currentNode = first.toNode;

            int guard = 0;
            while (guard++ < 20000) {
                if (currentNode == startNode) {
                    break;
                }
                int nextIndex = findNextByFrom(remaining, currentNode);
                if (nextIndex < 0) {
                    break;
                }
                EdgeSegment next = remaining.remove(nextIndex);
                loop.add(next.p2);
                currentNode = next.toNode;
            }

            if (loop.size() >= 3) {
                loops.add(loop);
            }
        }

        return loops;
    }

    private static int findNextByFrom(@NonNull List<EdgeSegment> segments, int fromNode) {
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).fromNode == fromNode) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Ray-casting point-in-polygon on RA/Dec plane, with RA unwrapped around the query RA.
     */
    private static boolean pointInPolygon(double raDeg, double decDeg, @NonNull List<Point> polygon) {
        int n = polygon.size();
        if (n < 3) return false;

        double x = raDeg;
        double y = decDeg;
        boolean inside = false;

        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point pi = polygon.get(i);
            Point pj = polygon.get(j);

            double xi = unwrapAround(pi.raDeg, x);
            double xj = unwrapAround(pj.raDeg, x);
            double yi = pi.decDeg;
            double yj = pj.decDeg;

            boolean intersects = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / ((yj - yi) + 1e-12) + xi);
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static double unwrapAround(double raDeg, double centerDeg) {
        double value = raDeg;
        while (value - centerDeg > 180.0) value -= 360.0;
        while (value - centerDeg < -180.0) value += 360.0;
        return value;
    }

    @NonNull
    private static String readAsset(@NonNull AssetManager assets, @NonNull String assetName) throws Exception {
        try (InputStream in = assets.open(assetName)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    @NonNull
    private static String readBoundaryAsset(@NonNull AssetManager assets) throws Exception {
        try {
            return readAsset(assets, BOUND_ASSET_PRIMARY);
        } catch (Exception primaryError) {
            Log.w(TAG, "Primary boundary asset missing, falling back to " + BOUND_ASSET_FALLBACK, primaryError);
            return readAsset(assets, BOUND_ASSET_FALLBACK);
        }
    }

    private static final class Point {
        final double raDeg;
        final double decDeg;

        Point(double raDeg, double decDeg) {
            this.raDeg = raDeg;
            this.decDeg = decDeg;
        }
    }

    private static final class BoundaryPolygon {
        final String code;
        final String name;
        final List<Point> points;

        BoundaryPolygon(@NonNull String code, @NonNull String name, @NonNull List<Point> points) {
            this.code = code;
            this.name = name;
            this.points = points;
        }
    }

    private static final class EdgeSegment {
        final int fromNode;
        final int toNode;
        final Point p1;
        final Point p2;

        EdgeSegment(int fromNode, int toNode, @NonNull Point p1, @NonNull Point p2) {
            this.fromNode = fromNode;
            this.toNode = toNode;
            this.p1 = p1;
            this.p2 = p2;
        }
    }
}
