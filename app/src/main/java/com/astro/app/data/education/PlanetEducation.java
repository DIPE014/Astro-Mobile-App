package com.astro.app.data.education;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Educational content for a planet or solar system body.
 */
public class PlanetEducation {
    @Nullable
    private final String id;
    @NonNull
    private final String name;
    @Nullable
    private final String summary;
    @NonNull
    private final List<String> facts;
    @Nullable
    private final String howToSpot;
    @Nullable
    private final String funFact;

    public PlanetEducation(@Nullable String id,
                           @NonNull String name,
                           @Nullable String summary,
                           @NonNull List<String> facts,
                           @Nullable String howToSpot,
                           @Nullable String funFact) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.facts = Collections.unmodifiableList(facts);
        this.howToSpot = howToSpot;
        this.funFact = funFact;
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
    public String getSummary() {
        return summary;
    }

    @NonNull
    public List<String> getFacts() {
        return facts;
    }

    @Nullable
    public String getHowToSpot() {
        return howToSpot;
    }

    @Nullable
    public String getFunFact() {
        return funFact;
    }
}
