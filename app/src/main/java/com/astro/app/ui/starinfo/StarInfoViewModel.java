package com.astro.app.ui.starinfo;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.astro.app.data.model.ConstellationData;
import com.astro.app.data.model.StarData;
import com.astro.app.data.repository.ConstellationRepository;
import com.astro.app.data.repository.StarRepository;

import java.util.List;

/**
 * ViewModel for the StarInfoActivity.
 *
 * <p>Manages the state for displaying detailed information about a star including:
 * <ul>
 *   <li>Star data (name, coordinates, magnitude, spectral type, etc.)</li>
 *   <li>Constellation information if the star belongs to one</li>
 *   <li>Related stars in the same constellation</li>
 * </ul>
 * </p>
 *
 * <p>This ViewModel retrieves star data from the StarRepository and
 * constellation data from the ConstellationRepository.</p>
 */
public class StarInfoViewModel extends AndroidViewModel {

    private static final String TAG = "StarInfoViewModel";

    // ==================== LiveData Fields ====================

    /** The star being displayed */
    private final MutableLiveData<StarData> starLiveData = new MutableLiveData<>();

    /** The constellation this star belongs to (if any) */
    private final MutableLiveData<ConstellationData> constellationLiveData = new MutableLiveData<>();

    /** Other stars in the same constellation */
    private final MutableLiveData<List<StarData>> relatedStarsLiveData = new MutableLiveData<>();

    /** Loading state for async operations */
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    /** Error messages for UI feedback */
    private final MutableLiveData<String> errorMessageLiveData = new MutableLiveData<>();

    /** Formatted star details for display */
    private final MutableLiveData<StarDetails> starDetailsLiveData = new MutableLiveData<>();

    /** Education content for named stars (from education_content.json) */
    private final MutableLiveData<EducationStar> educationStarLiveData = new MutableLiveData<>();

    // ==================== Dependencies ====================

    @Nullable
    private StarRepository starRepository;

    @Nullable
    private ConstellationRepository constellationRepository;

    @Nullable
    private java.util.Map<String, EducationStar> educationStarMap;

    /**
     * Creates a new StarInfoViewModel.
     *
     * @param application The application context
     */
    public StarInfoViewModel(@NonNull Application application) {
        super(application);
    }

    // ==================== Dependency Injection ====================

    /**
     * Sets the star repository for accessing star data.
     *
     * @param starRepository The star repository instance
     */
    public void setStarRepository(@NonNull StarRepository starRepository) {
        this.starRepository = starRepository;
    }

    /**
     * Sets the constellation repository for accessing constellation data.
     *
     * @param constellationRepository The constellation repository instance
     */
    public void setConstellationRepository(@NonNull ConstellationRepository constellationRepository) {
        this.constellationRepository = constellationRepository;
    }

    // ==================== LiveData Getters ====================

    /**
     * Returns the LiveData for the star being displayed.
     *
     * @return LiveData containing the star data
     */
    @NonNull
    public LiveData<StarData> getStar() {
        return starLiveData;
    }

    /**
     * Returns the LiveData for the star's constellation.
     *
     * @return LiveData containing the constellation, or null if none
     */
    @NonNull
    public LiveData<ConstellationData> getConstellation() {
        return constellationLiveData;
    }

    /**
     * Returns the LiveData for related stars in the same constellation.
     *
     * @return LiveData containing list of related stars
     */
    @NonNull
    public LiveData<List<StarData>> getRelatedStars() {
        return relatedStarsLiveData;
    }

    /**
     * Returns the LiveData for formatted star details.
     *
     * @return LiveData containing the formatted star details
     */
    @NonNull
    public LiveData<StarDetails> getStarDetails() {
        return starDetailsLiveData;
    }

    /**
     * Returns the LiveData for education content (if available).
     *
     * @return LiveData containing education content for named stars
     */
    @NonNull
    public LiveData<EducationStar> getEducationStar() {
        return educationStarLiveData;
    }

    /**
     * Returns the LiveData for loading state.
     *
     * @return LiveData containing true if loading
     */
    @NonNull
    public LiveData<Boolean> isLoading() {
        return loadingLiveData;
    }

    /**
     * Returns the LiveData for error messages.
     *
     * @return LiveData containing error message, or null if no error
     */
    @NonNull
    public LiveData<String> getErrorMessage() {
        return errorMessageLiveData;
    }

    // ==================== Data Loading ====================

    /**
     * Loads star data by star ID.
     *
     * <p>This will also load the associated constellation if the star
     * belongs to one, and the related stars in that constellation.</p>
     *
     * @param starId The ID of the star to load
     */
    public void loadStarById(@NonNull String starId) {
        if (starRepository == null) {
            errorMessageLiveData.setValue("Star repository not initialized");
            return;
        }

        loadingLiveData.setValue(true);
        errorMessageLiveData.setValue(null);

        try {
            StarData star = starRepository.getStarById(starId);
            if (star != null) {
                starLiveData.setValue(star);
                starDetailsLiveData.setValue(createStarDetails(star));
                loadEducationForStar(star);
                loadConstellationForStar(star);
                Log.d(TAG, "Loaded star: " + star.getName());
            } else {
                errorMessageLiveData.setValue("Star not found: " + starId);
                Log.w(TAG, "Star not found: " + starId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading star: " + starId, e);
            errorMessageLiveData.setValue("Error loading star: " + e.getMessage());
        } finally {
            loadingLiveData.setValue(false);
        }
    }

    /**
     * Loads star data by star name.
     *
     * @param name The name of the star to load
     */
    public void loadStarByName(@NonNull String name) {
        if (starRepository == null) {
            errorMessageLiveData.setValue("Star repository not initialized");
            return;
        }

        loadingLiveData.setValue(true);
        errorMessageLiveData.setValue(null);

        try {
            StarData star = starRepository.getStarByName(name);
            if (star != null) {
                starLiveData.setValue(star);
                starDetailsLiveData.setValue(createStarDetails(star));
                loadEducationForStar(star);
                loadConstellationForStar(star);
                Log.d(TAG, "Loaded star by name: " + star.getName());
            } else {
                errorMessageLiveData.setValue("Star not found: " + name);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading star by name: " + name, e);
            errorMessageLiveData.setValue("Error loading star: " + e.getMessage());
        } finally {
            loadingLiveData.setValue(false);
        }
    }

    /**
     * Sets the star directly from star data.
     *
     * <p>Use this when the star data is already available (e.g., passed via intent).</p>
     *
     * @param star The star data to display
     */
    public void setStar(@NonNull StarData star) {
        starLiveData.setValue(star);
        starDetailsLiveData.setValue(createStarDetails(star));
        loadEducationForStar(star);
        loadConstellationForStar(star);
    }

    /**
     * Loads star from intent extras.
     *
     * <p>Creates a minimal StarData object from the provided values.
     * This is used when star data is passed via intent extras.</p>
     *
     * @param name      The star name
     * @param ra        Right Ascension in degrees
     * @param dec       Declination in degrees
     * @param magnitude Apparent magnitude
     */
    public void loadFromExtras(@Nullable String name, float ra, float dec, float magnitude) {
        if (name == null || name.isEmpty()) {
            errorMessageLiveData.setValue("No star data provided");
            return;
        }

        // Try to find the full star data in the repository
        if (starRepository != null) {
            StarData fullStar = starRepository.getStarByName(name);
            if (fullStar != null) {
                starLiveData.setValue(fullStar);
                starDetailsLiveData.setValue(createStarDetails(fullStar));
                loadEducationForStar(fullStar);
                loadConstellationForStar(fullStar);
                return;
            }
        }

        // If not found, create a minimal star from the extras
        StarData minimalStar = StarData.builder()
                .setId("unknown_" + name.toLowerCase().replace(" ", "_"))
                .setName(name)
                .setRa(ra)
                .setDec(dec)
                .setMagnitude(magnitude)
                .build();

        starLiveData.setValue(minimalStar);
        starDetailsLiveData.setValue(createStarDetails(minimalStar));
        loadEducationForStar(minimalStar);
    }

    /**
     * Loads constellation information for a star.
     *
     * @param star The star to get constellation info for
     */
    private void loadConstellationForStar(@NonNull StarData star) {
        if (constellationRepository == null) {
            Log.d(TAG, "Constellation repository not set");
            return;
        }

        String constellationId = star.getConstellationId();
        if (constellationId == null || constellationId.isEmpty()) {
            constellationLiveData.setValue(null);
            relatedStarsLiveData.setValue(null);
            return;
        }

        try {
            ConstellationData constellation = constellationRepository.getConstellationById(constellationId);
            constellationLiveData.setValue(constellation);

            // Load related stars in the same constellation
            if (constellation != null && starRepository != null) {
                List<StarData> related = starRepository.getStarsInConstellation(constellationId);
                // Remove the current star from related stars
                related.removeIf(s -> s.getId().equals(star.getId()));
                relatedStarsLiveData.setValue(related);
                Log.d(TAG, "Loaded " + related.size() + " related stars in " + constellation.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading constellation: " + constellationId, e);
            constellationLiveData.setValue(null);
        }
    }

    /**
     * Loads education content for a named star from education_content.json.
     */
    private void loadEducationForStar(@NonNull StarData star) {
        EducationStar educationStar = findEducationStar(star);
        educationStarLiveData.setValue(educationStar);
    }

    @Nullable
    private EducationStar findEducationStar(@NonNull StarData star) {
        if (educationStarMap == null) {
            educationStarMap = loadEducationStarMap();
        }
        if (educationStarMap == null || educationStarMap.isEmpty()) {
            return null;
        }

        String nameKey = star.getName().toLowerCase(java.util.Locale.ROOT);
        EducationStar match = educationStarMap.get(nameKey);
        if (match != null) {
            return match;
        }

        for (String alternate : star.getAlternateNames()) {
            if (alternate == null || alternate.isEmpty()) {
                continue;
            }
            match = educationStarMap.get(alternate.toLowerCase(java.util.Locale.ROOT));
            if (match != null) {
                return match;
            }
        }

        return null;
    }

    @Nullable
    private java.util.Map<String, EducationStar> loadEducationStarMap() {
        try (java.io.InputStream inputStream = getApplication().getAssets().open("education_content.json")) {
            String json = readStreamToString(inputStream);
            org.json.JSONObject root = new org.json.JSONObject(json);
            org.json.JSONArray stars = root.optJSONArray("brightest_stars");
            if (stars == null) {
                return null;
            }

            java.util.Map<String, EducationStar> map = new java.util.HashMap<>();
            for (int i = 0; i < stars.length(); i++) {
                org.json.JSONObject star = stars.optJSONObject(i);
                if (star == null) {
                    continue;
                }
                String displayName = star.optString("displayName", "").trim();
                if (displayName.isEmpty()) {
                    continue;
                }
                EducationStar entry = new EducationStar(
                        displayName,
                        star.optString("constellation", "").trim(),
                        star.optString("funFact", "").trim(),
                        star.optString("history", "").trim());
                map.put(displayName.toLowerCase(java.util.Locale.ROOT), entry);
            }

            return map;
        } catch (Exception e) {
            Log.w(TAG, "Failed to load education_content.json", e);
            return null;
        }
    }

    @NonNull
    private String readStreamToString(@NonNull java.io.InputStream inputStream) throws java.io.IOException {
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toString(java.nio.charset.StandardCharsets.UTF_8.name());
    }

    /**
     * Returns constellation information for a star by constellation ID.
     *
     * @param constellationId The constellation ID
     * @return The constellation data, or null if not found
     */
    @Nullable
    public ConstellationData getConstellationById(@NonNull String constellationId) {
        if (constellationRepository != null) {
            return constellationRepository.getConstellationById(constellationId);
        }
        return null;
    }

    // ==================== Star Details Formatting ====================

    /**
     * Creates formatted star details for display.
     *
     * @param star The star data
     * @return Formatted star details
     */
    @NonNull
    private StarDetails createStarDetails(@NonNull StarData star) {
        return new StarDetails(star);
    }

    /**
     * Clears any displayed error message.
     */
    public void clearError() {
        errorMessageLiveData.setValue(null);
    }

    // ==================== Inner Classes ====================

    /**
     * Formatted star details for UI display.
     *
     * <p>This class provides pre-formatted strings for common star properties,
     * suitable for direct binding to UI elements.</p>
     */
    public static class StarDetails {

        private final StarData star;

        /**
         * Creates new star details from star data.
         *
         * @param star The star data
         */
        public StarDetails(@NonNull StarData star) {
            this.star = star;
        }

        /**
         * Returns the star name.
         *
         * @return The star's display name
         */
        @NonNull
        public String getName() {
            return star.getName();
        }

        /**
         * Returns formatted Right Ascension.
         *
         * @return RA in degrees with units
         */
        @NonNull
        public String getFormattedRa() {
            return String.format("%.4f\u00b0", star.getRa());
        }

        /**
         * Returns formatted Right Ascension in HMS format.
         *
         * @return RA in hours:minutes:seconds format
         */
        @NonNull
        public String getFormattedRaHms() {
            float ra = star.getRa();
            float hours = ra / 15f;  // Convert degrees to hours
            int h = (int) hours;
            float minutesDecimal = (hours - h) * 60f;
            int m = (int) minutesDecimal;
            float s = (minutesDecimal - m) * 60f;
            return String.format("%dh %dm %.1fs", h, m, s);
        }

        /**
         * Returns formatted Declination.
         *
         * @return Dec in degrees with units
         */
        @NonNull
        public String getFormattedDec() {
            return String.format("%+.4f\u00b0", star.getDec());
        }

        /**
         * Returns formatted Declination in DMS format.
         *
         * @return Dec in degrees:arcminutes:arcseconds format
         */
        @NonNull
        public String getFormattedDecDms() {
            float dec = star.getDec();
            char sign = dec >= 0 ? '+' : '-';
            dec = Math.abs(dec);
            int d = (int) dec;
            float minutesDecimal = (dec - d) * 60f;
            int m = (int) minutesDecimal;
            float s = (minutesDecimal - m) * 60f;
            return String.format("%c%d\u00b0 %d' %.1f\"", sign, d, m, s);
        }

        /**
         * Returns formatted apparent magnitude.
         *
         * @return Magnitude with description
         */
        @NonNull
        public String getFormattedMagnitude() {
            float mag = star.getMagnitude();
            String brightness;
            if (mag < 0) {
                brightness = "Very Bright";
            } else if (mag < 1) {
                brightness = "Bright";
            } else if (mag < 3) {
                brightness = "Visible";
            } else if (mag < 6) {
                brightness = "Dim";
            } else {
                brightness = "Faint";
            }
            return String.format("%.2f (%s)", mag, brightness);
        }

        /**
         * Returns the spectral type.
         *
         * @return Spectral type or "Unknown" if not available
         */
        @NonNull
        public String getSpectralType() {
            String type = star.getSpectralType();
            return type != null ? type : "Unknown";
        }

        /**
         * Returns formatted spectral type with description.
         *
         * @return Spectral type with color/temperature description
         */
        @NonNull
        public String getFormattedSpectralType() {
            String type = star.getSpectralType();
            if (type == null || type.isEmpty()) {
                return "Unknown";
            }

            String description;
            char spectralClass = type.charAt(0);
            switch (spectralClass) {
                case 'O':
                    description = "Blue (Very Hot)";
                    break;
                case 'B':
                    description = "Blue-White (Hot)";
                    break;
                case 'A':
                    description = "White";
                    break;
                case 'F':
                    description = "Yellow-White";
                    break;
                case 'G':
                    description = "Yellow (Sun-like)";
                    break;
                case 'K':
                    description = "Orange";
                    break;
                case 'M':
                    description = "Red (Cool)";
                    break;
                default:
                    description = "";
            }

            return description.isEmpty() ? type : type + " - " + description;
        }

        /**
         * Returns formatted distance.
         *
         * @return Distance in light years or "Unknown" if not available
         */
        @NonNull
        public String getFormattedDistance() {
            if (!star.hasKnownDistance()) {
                return "Unknown";
            }
            float distance = star.getDistance();
            if (distance < 100) {
                return String.format("%.1f light years", distance);
            } else {
                return String.format("%.0f light years", distance);
            }
        }

        /**
         * Returns whether the star is visible to naked eye.
         *
         * @return "Yes" or "No"
         */
        @NonNull
        public String getVisibleToNakedEye() {
            return star.isNakedEyeVisible() ? "Yes" : "No";
        }

        /**
         * Returns the star's constellation ID.
         *
         * @return Constellation ID or null
         */
        @Nullable
        public String getConstellationId() {
            return star.getConstellationId();
        }

        /**
         * Checks if the star has constellation information.
         *
         * @return true if the star belongs to a constellation
         */
        public boolean hasConstellation() {
            return star.hasConstellation();
        }

        /**
         * Returns the spectral color as an ARGB int.
         *
         * @return ARGB color value
         */
        public int getSpectralColor() {
            return star.getSpectralColor();
        }

        /**
         * Returns the underlying star data.
         *
         * @return The star data object
         */
        @NonNull
        public StarData getStarData() {
            return star;
        }
    }

    /**
     * Education content entry for a named star.
     */
    public static class EducationStar {
        private final String displayName;
        private final String constellation;
        private final String funFact;
        private final String history;

        public EducationStar(@NonNull String displayName,
                             @NonNull String constellation,
                             @NonNull String funFact,
                             @NonNull String history) {
            this.displayName = displayName;
            this.constellation = constellation;
            this.funFact = funFact;
            this.history = history;
        }

        @NonNull
        public String getDisplayName() {
            return displayName;
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
    }
}
