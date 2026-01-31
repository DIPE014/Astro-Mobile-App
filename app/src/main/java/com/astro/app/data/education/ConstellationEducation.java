package com.astro.app.data.education;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Educational content for a constellation.
 */
public class ConstellationEducation {
    @Nullable
    private final String id;
    @NonNull
    private final String name;
    @Nullable
    private final String family;
    @Nullable
    private final String bestMonth;
    @Nullable
    private final String approxCoordinates;
    @Nullable
    private final String backgroundOrigin;
    @Nullable
    private final String asterism;
    @Nullable
    private final String majorStars;
    @Nullable
    private final String formalizedDiscovered;
    @Nullable
    private final String source;

    public ConstellationEducation(@Nullable String id,
                                  @NonNull String name,
                                  @Nullable String family,
                                  @Nullable String bestMonth,
                                  @Nullable String approxCoordinates,
                                  @Nullable String backgroundOrigin,
                                  @Nullable String asterism,
                                  @Nullable String majorStars,
                                  @Nullable String formalizedDiscovered,
                                  @Nullable String source) {
        this.id = id;
        this.name = name;
        this.family = family;
        this.bestMonth = bestMonth;
        this.approxCoordinates = approxCoordinates;
        this.backgroundOrigin = backgroundOrigin;
        this.asterism = asterism;
        this.majorStars = majorStars;
        this.formalizedDiscovered = formalizedDiscovered;
        this.source = source;
    }

    @Nullable
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getFamily() {
        return family;
    }

    @Nullable
    public String getBestMonth() {
        return bestMonth;
    }

    @Nullable
    public String getApproxCoordinates() {
        return approxCoordinates;
    }

    @Nullable
    public String getBackgroundOrigin() {
        return backgroundOrigin;
    }

    @Nullable
    public String getAsterism() {
        return asterism;
    }

    @Nullable
    public String getMajorStars() {
        return majorStars;
    }

    @Nullable
    public String getFormalizedDiscovered() {
        return formalizedDiscovered;
    }

    @Nullable
    public String getSource() {
        return source;
    }
}
