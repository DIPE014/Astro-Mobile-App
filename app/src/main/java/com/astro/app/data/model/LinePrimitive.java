package com.astro.app.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A rendering primitive representing a line or polyline in the sky.
 *
 * <p>LinePrimitives are used to render constellation lines, coordinate grids,
 * horizon lines, and other linear features in the sky map. A line consists of
 * a series of vertices (points) that are connected to form a path.</p>
 *
 * <h3>Rendering Properties:</h3>
 * <ul>
 *   <li><b>vertices</b> - List of celestial coordinates forming the line path</li>
 *   <li><b>color</b> - ARGB color for the line</li>
 *   <li><b>lineWidth</b> - Width of the line in pixels</li>
 * </ul>
 *
 * <h3>Vertex Ordering:</h3>
 * <p>Vertices are connected in order: vertex[0] to vertex[1], vertex[1] to vertex[2],
 * and so on. For a line segment (two points), use exactly 2 vertices.</p>
 *
 * @see PointPrimitive
 * @see LabelPrimitive
 * @see GeocentricCoords
 */
public class LinePrimitive {

    /** Default white color */
    private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    /** Default line width in pixels */
    private static final float DEFAULT_LINE_WIDTH = 1.5f;

    /** ARGB color of the line */
    private final int color;

    /** Vertices in geocentric coordinates */
    @NonNull
    private final List<GeocentricCoords> vertices;

    /** Width of the line in pixels */
    private final float lineWidth;

    /**
     * Creates an empty line primitive with default color and line width.
     */
    public LinePrimitive() {
        this(DEFAULT_COLOR, new ArrayList<>(), DEFAULT_LINE_WIDTH);
    }

    /**
     * Creates an empty line primitive with specified color.
     *
     * @param color ARGB color
     */
    public LinePrimitive(int color) {
        this(color, new ArrayList<>(), DEFAULT_LINE_WIDTH);
    }

    /**
     * Creates an empty line primitive with specified color and line width.
     *
     * @param color     ARGB color
     * @param lineWidth Width in pixels
     */
    public LinePrimitive(int color, float lineWidth) {
        this(color, new ArrayList<>(), lineWidth);
    }

    /**
     * Creates a line primitive with all parameters.
     *
     * @param color     ARGB color
     * @param vertices  List of vertices in geocentric coordinates
     * @param lineWidth Width in pixels
     */
    public LinePrimitive(int color, @NonNull List<GeocentricCoords> vertices, float lineWidth) {
        this.color = color;
        this.vertices = new ArrayList<>(vertices);
        this.lineWidth = lineWidth;
    }

    /**
     * Creates a line segment between two points.
     *
     * @param start     Start point coordinates
     * @param end       End point coordinates
     * @param color     ARGB color
     * @param lineWidth Width in pixels
     * @return A new LinePrimitive with two vertices
     */
    @NonNull
    public static LinePrimitive createSegment(@NonNull GeocentricCoords start,
                                               @NonNull GeocentricCoords end,
                                               int color, float lineWidth) {
        List<GeocentricCoords> vertices = new ArrayList<>();
        vertices.add(start);
        vertices.add(end);
        return new LinePrimitive(color, vertices, lineWidth);
    }

    /**
     * Creates a line segment between two points using RA/Dec.
     *
     * @param ra1       Start Right Ascension in degrees
     * @param dec1      Start Declination in degrees
     * @param ra2       End Right Ascension in degrees
     * @param dec2      End Declination in degrees
     * @param color     ARGB color
     * @param lineWidth Width in pixels
     * @return A new LinePrimitive with two vertices
     */
    @NonNull
    public static LinePrimitive createSegment(float ra1, float dec1,
                                               float ra2, float dec2,
                                               int color, float lineWidth) {
        return createSegment(
                GeocentricCoords.fromDegrees(ra1, dec1),
                GeocentricCoords.fromDegrees(ra2, dec2),
                color, lineWidth);
    }

    /**
     * Adds a vertex to this line from geocentric coordinates.
     *
     * @param coords The coordinates to add
     */
    public void addVertex(@NonNull GeocentricCoords coords) {
        vertices.add(coords);
    }

    /**
     * Adds a vertex to this line from RA/Dec coordinates.
     *
     * @param ra  Right Ascension in degrees
     * @param dec Declination in degrees
     */
    public void addVertex(float ra, float dec) {
        vertices.add(GeocentricCoords.fromDegrees(ra, dec));
    }

    /**
     * Adds multiple vertices to this line.
     *
     * @param coords List of coordinates to add
     */
    public void addVertices(@NonNull List<GeocentricCoords> coords) {
        vertices.addAll(coords);
    }

    /**
     * Clears all vertices from this line.
     */
    public void clearVertices() {
        vertices.clear();
    }

    /**
     * Returns the ARGB color.
     *
     * @return ARGB color value
     */
    public int getColor() {
        return color;
    }

    /**
     * Returns the alpha component of the color.
     *
     * @return Alpha value (0-255)
     */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
     * Returns the vertices as an unmodifiable list.
     *
     * @return Unmodifiable list of vertices in geocentric coordinates
     */
    @NonNull
    public List<GeocentricCoords> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    /**
     * Returns the mutable vertices list for adding points.
     *
     * <p>Use this method when you need to modify the vertices directly.
     * For read-only access, prefer {@link #getVertices()}.</p>
     *
     * @return Mutable list of vertices
     */
    @NonNull
    public List<GeocentricCoords> getVerticesMutable() {
        return vertices;
    }

    /**
     * Returns the vertex at the specified index.
     *
     * @param index The vertex index
     * @return The vertex coordinates
     * @throws IndexOutOfBoundsException if index is out of range
     */
    @NonNull
    public GeocentricCoords getVertex(int index) {
        return vertices.get(index);
    }

    /**
     * Returns the first vertex of the line.
     *
     * @return The first vertex, or null if the line is empty
     */
    @Nullable
    public GeocentricCoords getFirstVertex() {
        return vertices.isEmpty() ? null : vertices.get(0);
    }

    /**
     * Returns the last vertex of the line.
     *
     * @return The last vertex, or null if the line is empty
     */
    @Nullable
    public GeocentricCoords getLastVertex() {
        return vertices.isEmpty() ? null : vertices.get(vertices.size() - 1);
    }

    /**
     * Returns the line width in pixels.
     *
     * @return Line width
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * Returns the number of vertices in this line.
     *
     * @return Vertex count
     */
    public int getVertexCount() {
        return vertices.size();
    }

    /**
     * Returns the number of line segments.
     *
     * <p>For a polyline with n vertices, there are n-1 segments.</p>
     *
     * @return Segment count (0 if fewer than 2 vertices)
     */
    public int getSegmentCount() {
        return Math.max(0, vertices.size() - 1);
    }

    /**
     * Checks if this line has enough vertices to be drawn.
     *
     * @return true if the line has at least 2 vertices
     */
    public boolean isDrawable() {
        return vertices.size() >= 2;
    }

    /**
     * Checks if this line is empty (has no vertices).
     *
     * @return true if the line has no vertices
     */
    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    /**
     * Converts all vertices to 3D unit vectors.
     *
     * <p>Each vertex is converted to a float[3] array containing [x, y, z].
     * This is useful for OpenGL rendering.</p>
     *
     * @return List of float arrays, each containing [x, y, z]
     */
    @NonNull
    public List<float[]> toVector3List() {
        List<float[]> vectors = new ArrayList<>(vertices.size());
        for (GeocentricCoords coord : vertices) {
            vectors.add(coord.toVector3());
        }
        return vectors;
    }

    /**
     * Converts all vertices to a flat float array for OpenGL.
     *
     * <p>Returns an array of length vertexCount * 3, with coordinates
     * interleaved as [x0, y0, z0, x1, y1, z1, ...].</p>
     *
     * @return Float array of interleaved coordinates
     */
    @NonNull
    public float[] toFlatArray() {
        float[] result = new float[vertices.size() * 3];
        int i = 0;
        for (GeocentricCoords coord : vertices) {
            float[] vec = coord.toVector3();
            result[i++] = vec[0];
            result[i++] = vec[1];
            result[i++] = vec[2];
        }
        return result;
    }

    /**
     * Creates a copy of this line with a different color.
     *
     * @param newColor The new ARGB color
     * @return A new LinePrimitive with the specified color
     */
    @NonNull
    public LinePrimitive withColor(int newColor) {
        return new LinePrimitive(newColor, new ArrayList<>(vertices), lineWidth);
    }

    /**
     * Creates a copy of this line with a different line width.
     *
     * @param newWidth The new line width in pixels
     * @return A new LinePrimitive with the specified width
     */
    @NonNull
    public LinePrimitive withLineWidth(float newWidth) {
        return new LinePrimitive(color, new ArrayList<>(vertices), newWidth);
    }

    /**
     * Creates a copy of this line with modified alpha.
     *
     * @param alpha The new alpha value (0-255)
     * @return A new LinePrimitive with the specified alpha
     */
    @NonNull
    public LinePrimitive withAlpha(int alpha) {
        int newColor = (alpha << 24) | (color & 0x00FFFFFF);
        return new LinePrimitive(newColor, new ArrayList<>(vertices), lineWidth);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinePrimitive that = (LinePrimitive) o;
        return color == that.color &&
               Float.compare(that.lineWidth, lineWidth) == 0 &&
               vertices.equals(that.vertices);
    }

    @Override
    public int hashCode() {
        int result = color;
        result = 31 * result + (lineWidth != +0.0f ? Float.floatToIntBits(lineWidth) : 0);
        result = 31 * result + vertices.hashCode();
        return result;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("LinePrimitive{color=0x%08X, vertices=%d, lineWidth=%.1f}",
                color, vertices.size(), lineWidth);
    }

    /**
     * Builder for creating LinePrimitive instances.
     *
     * <p>Example usage:
     * <pre>{@code
     * LinePrimitive line = new LinePrimitive.Builder()
     *     .setColor(0xFF00FF00)
     *     .setLineWidth(2.0f)
     *     .addVertex(0.0f, 0.0f)
     *     .addVertex(45.0f, 30.0f)
     *     .addVertex(90.0f, 0.0f)
     *     .build();
     * }</pre>
     * </p>
     */
    public static class Builder {

        private int color = DEFAULT_COLOR;
        private float lineWidth = DEFAULT_LINE_WIDTH;
        private final List<GeocentricCoords> vertices = new ArrayList<>();

        /**
         * Creates a new Builder instance.
         */
        public Builder() {
        }

        /**
         * Sets the ARGB color.
         *
         * @param color ARGB color value
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setColor(int color) {
            this.color = color;
            return this;
        }

        /**
         * Sets the line width.
         *
         * @param lineWidth Width in pixels
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setLineWidth(float lineWidth) {
            this.lineWidth = lineWidth;
            return this;
        }

        /**
         * Adds a vertex to the line.
         *
         * @param coords The vertex coordinates
         * @return This builder for method chaining
         */
        @NonNull
        public Builder addVertex(@NonNull GeocentricCoords coords) {
            this.vertices.add(coords);
            return this;
        }

        /**
         * Adds a vertex to the line using RA/Dec.
         *
         * @param ra  Right Ascension in degrees
         * @param dec Declination in degrees
         * @return This builder for method chaining
         */
        @NonNull
        public Builder addVertex(float ra, float dec) {
            this.vertices.add(GeocentricCoords.fromDegrees(ra, dec));
            return this;
        }

        /**
         * Adds multiple vertices to the line.
         *
         * @param coords List of vertex coordinates
         * @return This builder for method chaining
         */
        @NonNull
        public Builder addVertices(@NonNull List<GeocentricCoords> coords) {
            this.vertices.addAll(coords);
            return this;
        }

        /**
         * Sets all vertices, replacing any existing vertices.
         *
         * @param coords List of vertex coordinates
         * @return This builder for method chaining
         */
        @NonNull
        public Builder setVertices(@NonNull List<GeocentricCoords> coords) {
            this.vertices.clear();
            this.vertices.addAll(coords);
            return this;
        }

        /**
         * Builds the LinePrimitive instance.
         *
         * @return A new LinePrimitive
         */
        @NonNull
        public LinePrimitive build() {
            return new LinePrimitive(color, vertices, lineWidth);
        }
    }
}
