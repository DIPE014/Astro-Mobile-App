package com.astro.app.data.model;

/**
 * Data model for Messier / Deep Sky Objects (DSOs).
 */
public class MessierObjectData {
    private final String id;
    private final String name;
    private final float ra;       // Right ascension in degrees
    private final float dec;      // Declination in degrees
    private final int color;      // ARGB color
    private final int size;       // Display size
    private final int shapeValue; // Proto shape ordinal
    private final float magnitude;

    public MessierObjectData(String id, String name, float ra, float dec,
                             int color, int size, int shapeValue) {
        this.id = id;
        this.name = name;
        this.ra = ra;
        this.dec = dec;
        this.color = color;
        this.size = size;
        this.shapeValue = shapeValue;
        // Derive approximate magnitude from size (larger objects tend to be brighter)
        this.magnitude = Math.max(4.0f, 10.0f - size * 0.5f);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public float getRa() { return ra; }
    public float getDec() { return dec; }
    public int getColor() { return color; }
    public int getSize() { return size; }
    public int getShapeValue() { return shapeValue; }
    public float getMagnitude() { return magnitude; }

    /** Returns a human-readable type string based on the shape value. */
    public String getTypeString() {
        switch (shapeValue) {
            case 2: return "Elliptical Galaxy";
            case 3: return "Spiral Galaxy";
            case 4: return "Irregular Galaxy";
            case 5: return "Lenticular Galaxy";
            case 6: return "Globular Cluster";
            case 7: return "Open Cluster";
            case 8: return "Nebula";
            case 9: return "Hubble Deep Field";
            default: return "Deep Sky Object";
        }
    }

    public boolean isGalaxy() {
        return shapeValue >= 2 && shapeValue <= 5;
    }

    public boolean isCluster() {
        return shapeValue == 6 || shapeValue == 7;
    }

    public boolean isNebula() {
        return shapeValue == 8;
    }
}
