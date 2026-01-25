package com.astro.app.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a constellation with its stars and connection lines.
 *
 * <p>A constellation consists of:
 * <ul>
 *   <li>A set of stars identified by their IDs</li>
 *   <li>Line connections between stars that form the constellation pattern</li>
 *   <li>A center point for labeling and identification</li>
 * </ul>
 * </p>
 *
 * <h3>Line Indices:</h3>
 * <p>The {@code lineIndices} list contains pairs of indices that reference
 * positions in the {@code starIds} list. For example, if starIds is
 * ["star1", "star2", "star3"] and lineIndices contains [0, 1], [1, 2],
 * then lines will be drawn from star1 to star2, and from star2 to star3.</p>
 *
 * <h3>Standard Constellation Abbreviations:</h3>
 * <p>Constellation IDs typically use the standard 3-letter IAU abbreviations:
 * <ul>
 *   <li>Ori - Orion</li>
 *   <li>UMa - Ursa Major (Big Dipper)</li>
 *   <li>CMa - Canis Major</li>
 *   <li>etc.</li>
 * </ul>
 * </p>
 *
 * @see StarData
 * @see GeocentricCoords
 */
public class ConstellationData {

    /** Unique identifier (typically 3-letter IAU abbreviation) */
    @NonNull
    private final String id;

    /** Display name of the constellation */
    @NonNull
    private final String name;

    /** List of star IDs that make up this constellation */
    @NonNull
    private final List<String> starIds;

    /**
     * Pairs of indices into starIds defining the constellation lines.
     * Each int[] should have exactly 2 elements: [startIndex, endIndex]
     */
    @NonNull
    private final List<int[]> lineIndices;

    /** Center point of the constellation for labeling */
    @Nullable
    private final GeocentricCoords centerPoint;

    /**
     * Private constructor. Use {@link Builder} to create instances.
     *
     * @param builder The builder containing constellation properties
     */
    private ConstellationData(@NonNull Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.starIds = Collections.unmodifiableList(new ArrayList<>(builder.starIds));
        // Deep copy lineIndices to ensure immutability
        List<int[]> copiedLines = new ArrayList<>();
        for (int[] line : builder.lineIndices) {
            copiedLines.add(new int[] {line[0], line[1]});
        }
        this.lineIndices = Collections.unmodifiableList(copiedLines);
        this.centerPoint = builder.centerPoint;
    }

    /**
         * Gets the constellation's unique identifier.
         *
         * <p>Typically the 3-letter IAU abbreviation (for example, "Ori" for Orion).</p>
         *
         * @return the constellation's unique identifier
         */
    @NonNull
    public String getId() {
        return id;
    }

    /**
     * Gets the display name of this constellation.
     *
     * @return the constellation display name (for example, "Orion")
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Get the star IDs that define this constellation.
     *
     * @return an unmodifiable list of star IDs in declaration order
     */
    @NonNull
    public List<String> getStarIds() {
        return starIds;
    }

    /**
     * Get the number of stars in this constellation.
     *
     * @return the number of star IDs in the constellation
     */
    public int getStarCount() {
        return starIds.size();
    }

    /**
         * Line index pairs used to draw the constellation's lines.
         *
         * <p>Each int[] has two elements: [startIndex, endIndex], which are indices into {@link #getStarIds()}.</p>
         *
         * @return an unmodifiable list of int[] pairs where each array contains `[startIndex, endIndex]` referencing positions in {@link #getStarIds()}
         */
    @NonNull
    public List<int[]> getLineIndices() {
        return lineIndices;
    }

    /**
     * Number of line segments that connect stars in this constellation.
     *
     * @return the number of line index pairs defining the constellation's lines
     */
    public int getLineCount() {
        return lineIndices.size();
    }

    /**
     * The constellation's center point used for label placement and view calculations.
     *
     * @return the center coordinates, or {@code null} if not set
     */
    @Nullable
    public GeocentricCoords getCenterPoint() {
        return centerPoint;
    }

    /**
     * Determines whether a center point is defined for this constellation.
     *
     * @return `true` if a center point is defined, `false` otherwise.
     */
    public boolean hasCenterPoint() {
        return centerPoint != null;
    }

    /**
     * Get the constellation center's right ascension (RA) in degrees.
     *
     * @return the center RA in degrees, or 0 if no center point is defined
     */
    public float getCenterRa() {
        return centerPoint != null ? centerPoint.getRa() : 0f;
    }

    /**
     * Get the constellation center's declination in degrees.
     *
     * @return Center declination in degrees, or 0 if the center point is not set
     */
    public float getCenterDec() {
        return centerPoint != null ? centerPoint.getDec() : 0f;
    }

    /**
     * Determines whether the constellation contains the given star ID.
     *
     * @param starId the star identifier to check
     * @return `true` if the constellation contains `starId`, `false` otherwise
     */
    public boolean containsStar(@NonNull String starId) {
        return starIds.contains(starId);
    }

    /**
         * Finds the index of the given star ID in the constellation's star list.
         *
         * @param starId the star identifier to locate
         * @return the index of `starId` in the constellation's starIds, or -1 if not found
         */
    public int getStarIndex(@NonNull String starId) {
        return starIds.indexOf(starId);
    }

    /**
     * Get the star ID at the specified zero-based index.
     *
     * @param index zero-based position of the star in the constellation's star list
     * @return the star ID at the given index
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    @NonNull
    public String getStarIdAt(int index) {
        return starIds.get(index);
    }

    /**
         * Provide star ID pairs for each valid line in the constellation.
         *
         * <p>Each element in the returned list is a String[] of length 2 containing
         * [startStarId, endStarId]. Lines with index pairs that are out of bounds
         * for the constellation's star list are omitted.</p>
         *
         * @return a list of String[] pairs where each array is [startStarId, endStarId]
         */
    @NonNull
    public List<String[]> getLinePairs() {
        List<String[]> pairs = new ArrayList<>();
        for (int[] indices : lineIndices) {
            if (indices.length >= 2 &&
                indices[0] >= 0 && indices[0] < starIds.size() &&
                indices[1] >= 0 && indices[1] < starIds.size()) {
                pairs.add(new String[] {starIds.get(indices[0]), starIds.get(indices[1])});
            }
        }
        return pairs;
    }

    /**
     * Determine whether another object represents the same constellation by id.
     *
     * @param o the object to compare with
     * @return `true` if {@code o} is a ConstellationData with the same id, `false` otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstellationData that = (ConstellationData) o;
        return id.equals(that.id);
    }

    /**
     * Computes the hash code for this ConstellationData.
     *
     * @return the hash code derived from this constellation's id
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Provides a compact single-line representation of the constellation including its id, name, number of stars, and number of lines.
     *
     * @return a String containing the constellation id, name, star count, and line count in the format:
     *         "ConstellationData{id='...', name='...', stars=<n>, lines=<m>}"
     */
    @Override
    @NonNull
    public String toString() {
        return String.format("ConstellationData{id='%s', name='%s', stars=%d, lines=%d}",
                id, name, starIds.size(), lineIndices.size());
    }

    /**
     * Create a new Builder for ConstellationData.
     *
     * @return a new Builder for constructing a ConstellationData instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating ConstellationData instances.
     *
     * <p>Example usage:
     * <pre>{@code
     * ConstellationData orion = ConstellationData.builder()
     *     .setId("Ori")
     *     .setName("Orion")
     *     .setStarIds(Arrays.asList("betelgeuse", "rigel", "bellatrix", "mintaka"))
     *     .addLine(0, 1)  // Betelgeuse to Rigel
     *     .addLine(0, 2)  // Betelgeuse to Bellatrix
     *     .addLine(2, 3)  // Bellatrix to Mintaka
     *     .setCenterPoint(GeocentricCoords.fromDegrees(85.0f, -1.2f))
     *     .build();
     * }</pre>
     * </p>
     */
    public static class Builder {

        @NonNull
        private String id = "";

        @NonNull
        private String name = "";

        @NonNull
        private List<String> starIds = new ArrayList<>();

        @NonNull
        private List<int[]> lineIndices = new ArrayList<>();

        @Nullable
        private GeocentricCoords centerPoint = null;

        /**
         * Creates a new Builder instance.
         */
        public Builder() {
        }

        /**
         * Set the constellation's unique identifier.
         *
         * @param id the unique identifier, typically the 3-letter IAU abbreviation
         * @return this Builder instance for chaining
         */
        @NonNull
        public Builder setId(@NonNull String id) {
            this.id = id;
            return this;
        }

        /**
         * Set the user-visible display name for the constellation.
         *
         * @param name the display name; must be non-empty (validation occurs on build)
         * @return this builder for method chaining
         */
        @NonNull
        public Builder setName(@NonNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Replace the builder's star ID list with a defensive copy of the provided list; null clears the list.
         *
         * @param starIds list of star IDs, or {@code null} to set an empty list
         * @return this Builder for method chaining
         */
        @NonNull
        public Builder setStarIds(@Nullable List<String> starIds) {
            this.starIds = starIds != null ? new ArrayList<>(starIds) : new ArrayList<>();
            return this;
        }

        /**
         * Adds a star ID to the constellation.
         *
         * @param starId The star ID to add
         * @return This builder for method chaining
         */
        @NonNull
        public Builder addStarId(@NonNull String starId) {
            this.starIds.add(starId);
            return this;
        }

        /**
                 * Set the list of index pairs used to define constellation lines.
                 *
                 * Each element of `lineIndices` should be an `int[]` containing at least two elements,
                 * where the first two values are treated as `[startIndex, endIndex]`. Null entries or
                 * arrays with fewer than two elements are ignored. Passing `null` clears the builder's
                 * current line list. Arrays are copied defensively.
                 *
                 * @param lineIndices list of index pairs (`int[]`) or `null`
                 * @return this builder for chaining
                 */
        @NonNull
        public Builder setLineIndices(@Nullable List<int[]> lineIndices) {
            this.lineIndices = new ArrayList<>();
            if (lineIndices != null) {
                for (int[] line : lineIndices) {
                    if (line != null && line.length >= 2) {
                        this.lineIndices.add(new int[] {line[0], line[1]});
                    }
                }
            }
            return this;
        }

        /**
         * Add a line defined by the indices of its start and end stars within the builder's star list.
         *
         * @param startIndex index of the start star in the builder's starIds list
         * @param endIndex   index of the end star in the builder's starIds list
         * @return           this builder for method chaining
         */
        @NonNull
        public Builder addLine(int startIndex, int endIndex) {
            this.lineIndices.add(new int[] {startIndex, endIndex});
            return this;
        }

        /**
         * Set the constellation's center coordinates used for labeling and view calculations.
         *
         * @param centerPoint the center coordinates to set, or {@code null} to clear the center point
         * @return this builder for method chaining
         */
        @NonNull
        public Builder setCenterPoint(@Nullable GeocentricCoords centerPoint) {
            this.centerPoint = centerPoint;
            return this;
        }

        /**
         * Sets the constellation's center point from right ascension and declination expressed in degrees.
         *
         * @param ra  Right Ascension in degrees.
         * @param dec Declination in degrees.
         * @return    this builder instance for method chaining.
         */
        @NonNull
        public Builder setCenterPoint(float ra, float dec) {
            this.centerPoint = GeocentricCoords.fromDegrees(ra, dec);
            return this;
        }

        /**
         * Compute and set the builder's center point to the average right ascension and declination of the provided star coordinates.
         *
         * <p>If {@code starCoords} is empty, the builder is left unchanged.</p>
         *
         * @param starCoords list of star coordinates in the same order as the builder's starIds
         * @return this builder for method chaining
         */
        @NonNull
        public Builder calculateCenterFromStars(@NonNull List<GeocentricCoords> starCoords) {
            if (starCoords.isEmpty()) {
                return this;
            }

            float sumRa = 0;
            float sumDec = 0;
            for (GeocentricCoords coord : starCoords) {
                sumRa += coord.getRa();
                sumDec += coord.getDec();
            }

            float avgRa = sumRa / starCoords.size();
            float avgDec = sumDec / starCoords.size();
            this.centerPoint = GeocentricCoords.fromDegrees(avgRa, avgDec);
            return this;
        }

        /**
         * Creates a validated ConstellationData instance from this builder.
         *
         * @return the constructed ConstellationData
         * @throws IllegalStateException if required fields are missing (empty id or name) or if any line index is out of bounds relative to the builder's starIds
         */
        @NonNull
        public ConstellationData build() {
            validate();
            return new ConstellationData(this);
        }

        /**
         * Ensures the builder state meets the requirements for constructing a ConstellationData.
         *
         * Validates that {@code id} and {@code name} are non-empty and that each line index
         * pair references valid indices within the current {@code starIds} list.
         *
         * @throws IllegalStateException if {@code id} or {@code name} is empty, or if any line index is outside the range [0, {@code starIds.size() - 1}]
         */
        private void validate() {
            if (id.isEmpty()) {
                throw new IllegalStateException("ConstellationData requires a non-empty id");
            }
            if (name.isEmpty()) {
                throw new IllegalStateException("ConstellationData requires a non-empty name");
            }
            // Validate line indices are within bounds
            for (int[] line : lineIndices) {
                if (line[0] < 0 || line[0] >= starIds.size() ||
                    line[1] < 0 || line[1] >= starIds.size()) {
                    throw new IllegalStateException(
                            String.format("Line index out of bounds: [%d, %d] for %d stars",
                                    line[0], line[1], starIds.size()));
                }
            }
        }
    }
}