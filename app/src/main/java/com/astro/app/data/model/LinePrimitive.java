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
     * Constructs a LinePrimitive with the specified color, vertices, and line width.
     *
     * The provided vertex list is copied; subsequent mutations to the original list do not affect this instance.
     *
     * @param color     ARGB color value
     * @param vertices  list of vertices in geocentric coordinates (copied)
     * @param lineWidth line width in pixels
     */
    public LinePrimitive(int color, @NonNull List<GeocentricCoords> vertices, float lineWidth) {
        this.color = color;
        this.vertices = new ArrayList<>(vertices);
        this.lineWidth = lineWidth;
    }

    /**
     * Constructs a LinePrimitive representing a single segment from the given start point to the given end point.
     *
     * @param start     start vertex of the segment
     * @param end       end vertex of the segment
     * @param color     ARGB color applied to the line
     * @param lineWidth width of the line in pixels
     * @return          a LinePrimitive containing exactly two vertices (start followed by end) with the specified color and width
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
     * Appends the given geocentric coordinate as a vertex at the end of this line.
     *
     * @param coords the non-null geocentric coordinates to append
     */
    public void addVertex(@NonNull GeocentricCoords coords) {
        vertices.add(coords);
    }

    /**
     * Add a vertex to the line using equatorial coordinates.
     *
     * @param ra  Right Ascension in degrees.
     * @param dec Declination in degrees.
     */
    public void addVertex(float ra, float dec) {
        vertices.add(GeocentricCoords.fromDegrees(ra, dec));
    }

    /**
     * Appends the given vertices to the end of this line's vertex list in order.
     *
     * @param coords the coordinates to append (non-null)
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
     * The primitive's ARGB color.
     *
     * @return the ARGB color as an int.
     */
    public int getColor() {
        return color;
    }

    /**
     * Get the alpha component of the ARGB color.
     *
     * @return the alpha component as an integer in the range 0-255
     */
    public int getAlpha() {
        return (color >> 24) & 0xFF;
    }

    /**
         * Get an unmodifiable view of the vertices in geocentric coordinates.
         *
         * @return an unmodifiable list of vertices in geocentric coordinates
         */
    @NonNull
    public List<GeocentricCoords> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    /**
     * Provides direct mutable access to the internal vertex list for adding or modifying points.
     *
     * Modifying the returned list changes this LinePrimitive's vertices.
     *
     * @return the internal mutable list of GeocentricCoords vertices
     */
    @NonNull
    public List<GeocentricCoords> getVerticesMutable() {
        return vertices;
    }

    /**
         * Get the vertex at the given zero-based index.
         *
         * @param index the zero-based position of the vertex to retrieve
         * @return the vertex coordinates at the specified index
         */
    @NonNull
    public GeocentricCoords getVertex(int index) {
        return vertices.get(index);
    }

    /**
         * Get the first vertex of this line.
         *
         * @return the first vertex, or {@code null} if the line contains no vertices
         */
    @Nullable
    public GeocentricCoords getFirstVertex() {
        return vertices.isEmpty() ? null : vertices.get(0);
    }

    /**
     * Retrieve the last vertex of the line.
     *
     * @return the last vertex, or `null` if there are no vertices
     */
    @Nullable
    public GeocentricCoords getLastVertex() {
        return vertices.isEmpty() ? null : vertices.get(vertices.size() - 1);
    }

    /**
     * Provides the line width in pixels.
     *
     * @return the line width in pixels
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * The number of vertices in this line.
     *
     * @return the number of vertices contained in this LinePrimitive
     */
    public int getVertexCount() {
        return vertices.size();
    }

    /**
     * Compute the number of straight segments implied by the stored vertices.
     *
     * @return the number of segments; 0 if the primitive has fewer than 2 vertices
     */
    public int getSegmentCount() {
        return Math.max(0, vertices.size() - 1);
    }

    /**
     * Determines whether the line contains enough vertices to be rendered.
     *
     * @return `true` if the line has at least 2 vertices, `false` otherwise.
     */
    public boolean isDrawable() {
        return vertices.size() >= 2;
    }

    /**
     * Determines whether the line contains no vertices.
     *
     * @return `true` if the line has no vertices, `false` otherwise.
     */
    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    /**
     * Convert the primitive's vertices into 3-element 3D unit vectors.
     *
     * Each vertex is mapped to a float[3] array containing [x, y, z], in the same order as the vertices.
     *
     * @return a list of 3-element float arrays `[x, y, z]`, one per vertex
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
     * Create a copy of this LinePrimitive with its color's alpha channel set to the given value.
     *
     * @param alpha the alpha component (0–255). Only the lowest 8 bits are applied to the resulting color.
     * @return a new LinePrimitive with the same vertices and line width, and the color adjusted to the specified alpha (RGB preserved)
     */
    @NonNull
    public LinePrimitive withAlpha(int alpha) {
        int newColor = (alpha << 24) | (color & 0x00FFFFFF);
        return new LinePrimitive(newColor, new ArrayList<>(vertices), lineWidth);
    }

    /**
     * Determine whether another object represents the same LinePrimitive.
     *
     * @param o the object to compare with
     * @return `true` if {@code o} is a {@code LinePrimitive} with the same color, the same line width, and equal vertex sequence; `false` otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LinePrimitive that = (LinePrimitive) o;
        return color == that.color &&
               Float.compare(that.lineWidth, lineWidth) == 0 &&
               vertices.equals(that.vertices);
    }

    /**
     * Computes a hash code for this LinePrimitive using its color, line width, and vertex list.
     *
     * @return an integer hash code derived from the color, lineWidth, and vertices
     */
    @Override
    public int hashCode() {
        int result = color;
        result = 31 * result + (lineWidth != +0.0f ? Float.floatToIntBits(lineWidth) : 0);
        result = 31 * result + vertices.hashCode();
        return result;
    }

    /**
     * Short string describing the LinePrimitive's color, vertex count, and line width.
     *
     * @return a string in the form LinePrimitive{color=0xAARRGGBB, vertices=N, lineWidth=W.W}
     */
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
         * Set the ARGB color used by the builder.
         *
         * @param color ARGB color value in 0xAARRGGBB format
         * @return this Builder instance for method chaining
         */
        @NonNull
        public Builder setColor(int color) {
            this.color = color;
            return this;
        }

        /**
         * Set the line width used by the builder.
         *
         * @param lineWidth the line width in pixels
         * @return this Builder for method chaining
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
         * Appends the provided vertices to the builder's internal vertex list, preserving their order.
         *
         * @param coords list of vertices to append; elements must be non-null and the list may be empty
         * @return this Builder instance for method chaining
         */
        @NonNull
        public Builder addVertices(@NonNull List<GeocentricCoords> coords) {
            this.vertices.addAll(coords);
            return this;
        }

        /**
                 * Replace the builder's vertex list with a copy of the given coordinates.
                 *
                 * @param coords the vertices to set on the builder (copied into the builder)
                 * @return this builder for method chaining
                 */
        @NonNull
        public Builder setVertices(@NonNull List<GeocentricCoords> coords) {
            this.vertices.clear();
            this.vertices.addAll(coords);
            return this;
        }

        /**
                 * Builds a LinePrimitive configured with this builder's color, vertices, and line width.
                 *
                 * @return the constructed LinePrimitive
                 */
        @NonNull
        public LinePrimitive build() {
            return new LinePrimitive(color, vertices, lineWidth);
        }
    }
}