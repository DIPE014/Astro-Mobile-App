package com.astro.app.data.education;

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
import java.util.List;
import java.util.Map;

/**
 * Loads educational content for constellations and planets from assets.
 */
public class EducationRepository {
    private static final String TAG = "EducationRepository";
    private static final String CONSTELLATION_FILE = "constellation_education_no_is_zodiac.json";
    private static final String PLANET_FILE = "planets_education.json";

    private final AssetManager assetManager;
    private final Map<String, ConstellationEducation> constellationByName = new HashMap<>();
    private final Map<String, ConstellationEducation> constellationById = new HashMap<>();
    private final Map<String, PlanetEducation> planetByName = new HashMap<>();
    private final Map<String, PlanetEducation> planetById = new HashMap<>();
    private boolean loaded = false;

    public EducationRepository(@NonNull AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Nullable
    public synchronized ConstellationEducation getConstellationByName(@Nullable String name) {
        ensureLoaded();
        if (name == null) return null;
        return constellationByName.get(normalizeKey(name));
    }

    @Nullable
    public synchronized ConstellationEducation getConstellationById(@Nullable String id) {
        ensureLoaded();
        if (id == null) return null;
        return constellationById.get(normalizeKey(id));
    }

    @Nullable
    public synchronized PlanetEducation getPlanetByName(@Nullable String name) {
        ensureLoaded();
        if (name == null) return null;
        return planetByName.get(normalizeKey(name));
    }

    @Nullable
    public synchronized PlanetEducation getPlanetById(@Nullable String id) {
        ensureLoaded();
        if (id == null) return null;
        return planetById.get(normalizeKey(id));
    }

    private void ensureLoaded() {
        if (loaded) {
            return;
        }
        loadConstellations();
        loadPlanets();
        loaded = true;
    }

    private void loadConstellations() {
        try {
            String json = readAsset(CONSTELLATION_FILE);
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = optString(obj, "id");
                String name = optString(obj, "name");
                if (name == null || name.isEmpty()) {
                    continue;
                }
                ConstellationEducation education = new ConstellationEducation(
                        id,
                        name,
                        optString(obj, "family"),
                        optString(obj, "bestMonth"),
                        optString(obj, "approxCoordinates"),
                        optString(obj, "backgroundOrigin"),
                        optString(obj, "asterism"),
                        optString(obj, "majorStars"),
                        optString(obj, "formalizedDiscovered"),
                        optString(obj, "source")
                );
                constellationByName.put(normalizeKey(name), education);
                if (id != null && !id.isEmpty()) {
                    constellationById.put(normalizeKey(id), education);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load constellation education: " + e.getMessage());
        }
    }

    private void loadPlanets() {
        try {
            String json = readAsset(PLANET_FILE);
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = optString(obj, "id");
                String name = optString(obj, "name");
                if (name == null || name.isEmpty()) {
                    continue;
                }
                List<String> facts = new ArrayList<>();
                JSONArray factsJson = obj.optJSONArray("facts");
                if (factsJson != null) {
                    for (int j = 0; j < factsJson.length(); j++) {
                        String fact = factsJson.optString(j, null);
                        if (fact != null && !fact.isEmpty()) {
                            facts.add(fact);
                        }
                    }
                }
                PlanetEducation education = new PlanetEducation(
                        id,
                        name,
                        optString(obj, "summary"),
                        facts,
                        optString(obj, "howToSpot"),
                        optString(obj, "funFact")
                );
                planetByName.put(normalizeKey(name), education);
                if (id != null && !id.isEmpty()) {
                    planetById.put(normalizeKey(id), education);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load planet education: " + e.getMessage());
        }
    }

    @Nullable
    private String optString(@NonNull JSONObject obj, @NonNull String key) {
        String value = obj.optString(key, null);
        return value != null && !value.trim().isEmpty() ? value.trim() : null;
    }

    @NonNull
    private String readAsset(@NonNull String fileName) throws Exception {
        try (InputStream inputStream = assetManager.open(fileName);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
    }

    @NonNull
    private String normalizeKey(@NonNull String value) {
        return value.trim().toLowerCase();
    }
}
