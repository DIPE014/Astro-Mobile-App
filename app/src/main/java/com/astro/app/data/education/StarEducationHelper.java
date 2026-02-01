package com.astro.app.data.education;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class StarEducationHelper {
    private JSONObject educationData;
    private JSONObject constellationNames;

    public StarEducationHelper(Context context) {
        // Automatically loads your files from the assets folder when the app starts
        educationData = loadJSONFromAsset(context, "education_content.json");
        constellationNames = loadJSONFromAsset(context, "constellations.json");
    }

    /**
     * This is the method your UI Lead calls when a user hovers over a star.
     * @param starId The unique ID (like "merak")
     * @param conAbbr The 3-letter constellation code (like "UMa")
     */
    public String getStarDescription(String starId, String conAbbr) {
        try {
            // STEP 1: Check if there is a special story in education_content.json
            JSONArray stars = educationData.getJSONArray("brightest_stars");
            for (int i = 0; i < stars.length(); i++) {
                JSONObject star = stars.getJSONObject(i);
                if (star.getString("id").equalsIgnoreCase(starId)) {
                    // FOUND: Return the custom fun fact
                    return star.getString("funFact");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // STEP 2: FALLBACK - If no story is found, translate the code
        // It looks up "UMa" in constellations.json and returns "Ursa Major"
        String fullName = constellationNames.optString(conAbbr, "Unknown Constellation");
        return "Star in " + fullName;
    }

    // Helper method to read files from the assets folder
    private JSONObject loadJSONFromAsset(Context context, String fileName) {
        String json;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
            return new JSONObject(json);
        } catch (Exception ex) {
            ex.printStackTrace();
            return new JSONObject();
        }
    }
}