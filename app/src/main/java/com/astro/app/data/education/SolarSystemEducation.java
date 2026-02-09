package com.astro.app.data.education;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Educational content for a solar system body (planet, Sun, Moon).
 */
public class SolarSystemEducation {
    @Nullable
    private final String id;
    @NonNull
    private final String name;
    @NonNull
    private final String constellation;
    @NonNull
    private final String funFact;
    @NonNull
    private final String history;
    @NonNull
    private final String distance;
    @NonNull
    private final String apparentMagnitude;
    @NonNull
    private final String absoluteMagnitude;
    @NonNull
    private final String temperature;
    @NonNull
    private final String radius;
    @NonNull
    private final String mass;
    @NonNull
    private final String luminosity;
    @NonNull
    private final String ra;
    @NonNull
    private final String dec;
    @NonNull
    private final String raDecEpoch;
    @NonNull
    private final String raDecNote;

    public SolarSystemEducation(@Nullable String id,
                                @NonNull String name,
                                @NonNull String constellation,
                                @NonNull String funFact,
                                @NonNull String history,
                                @NonNull String distance,
                                @NonNull String apparentMagnitude,
                                @NonNull String absoluteMagnitude,
                                @NonNull String temperature,
                                @NonNull String radius,
                                @NonNull String mass,
                                @NonNull String luminosity,
                                @NonNull String ra,
                                @NonNull String dec,
                                @NonNull String raDecEpoch,
                                @NonNull String raDecNote) {
        this.id = id;
        this.name = name;
        this.constellation = constellation;
        this.funFact = funFact;
        this.history = history;
        this.distance = distance;
        this.apparentMagnitude = apparentMagnitude;
        this.absoluteMagnitude = absoluteMagnitude;
        this.temperature = temperature;
        this.radius = radius;
        this.mass = mass;
        this.luminosity = luminosity;
        this.ra = ra;
        this.dec = dec;
        this.raDecEpoch = raDecEpoch;
        this.raDecNote = raDecNote;
    }

    @Nullable
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getConstellation() {
        return constellation;
    }

    @NonNull
    public String getFunFact() {
        return funFact;
    }

    @NonNull
    public String getHistory() {
        return history;
    }

    @NonNull
    public String getDistance() {
        return distance;
    }

    @NonNull
    public String getApparentMagnitude() {
        return apparentMagnitude;
    }

    @NonNull
    public String getAbsoluteMagnitude() {
        return absoluteMagnitude;
    }

    @NonNull
    public String getTemperature() {
        return temperature;
    }

    @NonNull
    public String getRadius() {
        return radius;
    }

    @NonNull
    public String getMass() {
        return mass;
    }

    @NonNull
    public String getLuminosity() {
        return luminosity;
    }

    @NonNull
    public String getRa() {
        return ra;
    }

    @NonNull
    public String getDec() {
        return dec;
    }

    @NonNull
    public String getRaDecEpoch() {
        return raDecEpoch;
    }

    @NonNull
    public String getRaDecNote() {
        return raDecNote;
    }
}
