package com.astro.app.core.highlights;

import com.astro.app.core.control.SolarSystemBody;
import com.astro.app.core.control.space.Universe;
import com.astro.app.core.math.RaDec;
import com.astro.app.core.math.TimeUtilsKt;
import com.astro.app.data.model.ConstellationData;
import com.astro.app.data.model.MessierObjectData;
import com.astro.app.data.model.StarData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Computes tonight's visible celestial highlights based on observer location and time.
 */
public class TonightsHighlights {

    public static class Highlight {
        public final String name;
        public final String type;     // "planet", "star", "constellation", "dso"
        public final float ra;
        public final float dec;
        public final double altitude;
        public final String direction;
        public final String extra;

        public Highlight(String name, String type, float ra, float dec,
                         double altitude, String direction, String extra) {
            this.name = name;
            this.type = type;
            this.ra = ra;
            this.dec = dec;
            this.altitude = altitude;
            this.direction = direction;
            this.extra = extra;
        }
    }

    public static List<Highlight> compute(
            Universe universe,
            List<StarData> stars,
            List<ConstellationData> constellations,
            List<MessierObjectData> dsos,
            double observerLat, double observerLon, long timeMillis) {

        List<Highlight> highlights = new ArrayList<>();
        Date date = new Date(timeMillis);
        float lst = TimeUtilsKt.meanSiderealTime(date, (float) observerLon);

        // Planets
        if (universe != null) {
            for (SolarSystemBody body : SolarSystemBody.values()) {
                if (body == SolarSystemBody.Sun || body == SolarSystemBody.Earth) continue;
                try {
                    RaDec raDec = universe.getRaDec(body, date);
                    double[] altAz = computeAltAz(raDec.getRa(), raDec.getDec(),
                            observerLat, lst);
                    if (altAz[0] > 5.0) {
                        highlights.add(new Highlight(
                                body.name().charAt(0) + body.name().substring(1).toLowerCase(),
                                "planet", raDec.getRa(), raDec.getDec(),
                                altAz[0], azimuthToCardinal(altAz[1]),
                                String.format("Alt %.0f\u00b0", altAz[0])));
                    }
                } catch (Exception ignored) {}
            }
        }

        // Bright stars (magnitude < 1.5)
        if (stars != null) {
            for (StarData star : stars) {
                if (star.getMagnitude() > 1.5f) continue;
                double[] altAz = computeAltAz(star.getRa(), star.getDec(),
                        observerLat, lst);
                if (altAz[0] > 5.0) {
                    highlights.add(new Highlight(
                            star.getName(), "star", star.getRa(), star.getDec(),
                            altAz[0], azimuthToCardinal(altAz[1]),
                            String.format("Mag %.1f", star.getMagnitude())));
                }
            }
        }

        // Constellations
        if (constellations != null) {
            for (ConstellationData c : constellations) {
                if (!c.hasCenterPoint()) continue;
                double[] altAz = computeAltAz(c.getCenterRa(), c.getCenterDec(),
                        observerLat, lst);
                if (altAz[0] > 20.0) {
                    highlights.add(new Highlight(
                            c.getName(), "constellation",
                            c.getCenterRa(), c.getCenterDec(),
                            altAz[0], azimuthToCardinal(altAz[1]),
                            String.format("Alt %.0f\u00b0", altAz[0])));
                }
            }
        }

        // Deep sky objects
        if (dsos != null) {
            for (MessierObjectData dso : dsos) {
                double[] altAz = computeAltAz(dso.getRa(), dso.getDec(),
                        observerLat, lst);
                if (altAz[0] > 10.0) {
                    highlights.add(new Highlight(
                            dso.getName(), "dso", dso.getRa(), dso.getDec(),
                            altAz[0], azimuthToCardinal(altAz[1]),
                            dso.getTypeString()));
                }
            }
        }

        // Sort: planets first, then by altitude descending
        Collections.sort(highlights, (a, b) -> {
            int typeOrder = typeOrdinal(a.type) - typeOrdinal(b.type);
            if (typeOrder != 0) return typeOrder;
            return Double.compare(b.altitude, a.altitude);
        });

        return highlights;
    }

    private static int typeOrdinal(String type) {
        switch (type) {
            case "planet": return 0;
            case "star": return 1;
            case "constellation": return 2;
            case "dso": return 3;
            default: return 4;
        }
    }

    private static double[] computeAltAz(float ra, float dec,
                                          double observerLat, float lst) {
        double raRad = Math.toRadians(ra);
        double decRad = Math.toRadians(dec);
        double latRad = Math.toRadians(observerLat);
        double lstRad = Math.toRadians(lst);
        double ha = lstRad - raRad;

        double sinAlt = Math.sin(decRad) * Math.sin(latRad) +
                Math.cos(decRad) * Math.cos(latRad) * Math.cos(ha);
        double altitude = Math.toDegrees(Math.asin(sinAlt));

        double cosA = (Math.sin(decRad) - Math.sin(latRad) * sinAlt) /
                (Math.cos(latRad) * Math.cos(Math.asin(sinAlt)));
        cosA = Math.max(-1.0, Math.min(1.0, cosA));
        double azimuth = Math.toDegrees(Math.acos(cosA));
        if (Math.sin(ha) > 0) {
            azimuth = 360.0 - azimuth;
        }

        return new double[]{altitude, azimuth};
    }

    private static String azimuthToCardinal(double azimuth) {
        azimuth = ((azimuth % 360) + 360) % 360;
        if (azimuth < 22.5 || azimuth >= 337.5) return "N";
        if (azimuth < 67.5) return "NE";
        if (azimuth < 112.5) return "E";
        if (azimuth < 157.5) return "SE";
        if (azimuth < 202.5) return "S";
        if (azimuth < 247.5) return "SW";
        if (azimuth < 292.5) return "W";
        return "NW";
    }
}
