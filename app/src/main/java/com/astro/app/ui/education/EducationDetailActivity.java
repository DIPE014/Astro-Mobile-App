package com.astro.app.ui.education;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.astro.app.R;
import com.astro.app.data.education.ConstellationEducation;
import com.astro.app.data.education.EducationRepository;
import com.astro.app.data.education.PlanetEducation;
import com.astro.app.data.education.SolarSystemEducation;

import java.util.List;

/**
 * Displays educational content for constellations and planets.
 */
public class EducationDetailActivity extends AppCompatActivity {
    public static final String EXTRA_OBJECT_TYPE = "education_object_type";
    public static final String EXTRA_OBJECT_NAME = "education_object_name";
    public static final String EXTRA_OBJECT_ID = "education_object_id";

    public static final String TYPE_CONSTELLATION = "constellation";
    public static final String TYPE_PLANET = "planet";

    private TextView tvTitle;
    private TextView tvSubtitle;

    private String objectType;
    private String objectName;
    private String objectId;

    private View planetSection;
    private View planetLegacySection;
    private TextView tvPlanetSummary;
    private TextView tvPlanetFacts;
    private TextView tvPlanetHowToSpot;
    private TextView tvPlanetFunFact;
    private TextView tvPlanetHistory;
    private TextView tvPlanetApparentMagnitude;
    private TextView tvPlanetDistance;
    private TextView tvPlanetConstellation;
    private TextView tvPlanetRightAscension;
    private TextView tvPlanetDeclination;
    private TextView tvPlanetAbsoluteMagnitude;
    private TextView tvPlanetRaDecEpoch;
    private TextView tvPlanetTemperature;
    private TextView tvPlanetRadius;
    private TextView tvPlanetMass;
    private TextView tvPlanetLuminosity;
    private View cardPlanetQuickStats;
    private View cardPlanetPosition;
    private View cardPlanetPhysical;
    private View cardPlanetEducation;

    // Star-style planet views (activity_star_info.xml)
    private TextView tvStarName;
    private TextView tvStarAlternateNames;
    private TextView tvRightAscension;
    private TextView tvDeclination;
    private TextView tvMagnitude;
    private TextView tvSpectralType;
    private TextView tvDistance;
    private TextView tvConstellation;
    private TextView tvApparentMagnitude;
    private TextView tvAbsoluteMagnitude;
    private TextView tvTemperature;
    private TextView tvRadius;
    private TextView tvMass;
    private TextView tvLuminosity;
    private View cardEducation;
    private TextView tvEducationDisplayName;
    private TextView tvEducationConstellation;
    private TextView tvEducationFunFact;
    private TextView tvEducationHistory;

    private View constellationSection;
    private TextView tvConstellationFamily;
    private TextView tvConstellationBestMonth;
    private TextView tvConstellationApproxCoordinates;
    private TextView tvConstellationBackgroundOrigin;
    private TextView tvConstellationAsterism;
    private TextView tvConstellationMajorStars;
    private TextView tvConstellationFormalized;
    private TextView tvConstellationSource;

    private EducationRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        objectType = getIntent().getStringExtra(EXTRA_OBJECT_TYPE);
        objectName = getIntent().getStringExtra(EXTRA_OBJECT_NAME);
        objectId = getIntent().getStringExtra(EXTRA_OBJECT_ID);

        if (TYPE_PLANET.equals(objectType)) {
            setContentView(R.layout.activity_star_info);
        } else {
            setContentView(R.layout.activity_education_detail);
        }

        repository = new EducationRepository(getAssets());

        if (TYPE_PLANET.equals(objectType)) {
            initializeStarStylePlanetViews();
        } else {
            initializeViews();
        }
        setupToolbar();
        loadContent();
    }

    private void initializeViews() {
        tvTitle = findViewById(R.id.tvEducationTitle);
        tvSubtitle = findViewById(R.id.tvEducationSubtitle);

        planetSection = findViewById(R.id.sectionPlanet);
        planetLegacySection = findViewById(R.id.planetLegacySection);
        tvPlanetSummary = findViewById(R.id.tvPlanetSummary);
        tvPlanetFacts = findViewById(R.id.tvPlanetFacts);
        tvPlanetHowToSpot = findViewById(R.id.tvPlanetHowToSpot);
        tvPlanetFunFact = findViewById(R.id.tvPlanetFunFact);
        tvPlanetHistory = findViewById(R.id.tvPlanetHistory);
        tvPlanetApparentMagnitude = findViewById(R.id.tvPlanetApparentMagnitude);
        tvPlanetDistance = findViewById(R.id.tvPlanetDistance);
        tvPlanetConstellation = findViewById(R.id.tvPlanetConstellation);
        tvPlanetRightAscension = findViewById(R.id.tvPlanetRightAscension);
        tvPlanetDeclination = findViewById(R.id.tvPlanetDeclination);
        tvPlanetAbsoluteMagnitude = findViewById(R.id.tvPlanetAbsoluteMagnitude);
        tvPlanetRaDecEpoch = findViewById(R.id.tvPlanetRaDecEpoch);
        tvPlanetTemperature = findViewById(R.id.tvPlanetTemperature);
        tvPlanetRadius = findViewById(R.id.tvPlanetRadius);
        tvPlanetMass = findViewById(R.id.tvPlanetMass);
        tvPlanetLuminosity = findViewById(R.id.tvPlanetLuminosity);
        cardPlanetQuickStats = findViewById(R.id.cardPlanetQuickStats);
        cardPlanetPosition = findViewById(R.id.cardPlanetPosition);
        cardPlanetPhysical = findViewById(R.id.cardPlanetPhysical);
        cardPlanetEducation = findViewById(R.id.cardPlanetEducation);

        constellationSection = findViewById(R.id.sectionConstellation);
        tvConstellationFamily = findViewById(R.id.tvConstellationFamily);
        tvConstellationBestMonth = findViewById(R.id.tvConstellationBestMonth);
        tvConstellationApproxCoordinates = findViewById(R.id.tvConstellationApproxCoordinates);
        tvConstellationBackgroundOrigin = findViewById(R.id.tvConstellationBackgroundOrigin);
        tvConstellationAsterism = findViewById(R.id.tvConstellationAsterism);
        tvConstellationMajorStars = findViewById(R.id.tvConstellationMajorStars);
        tvConstellationFormalized = findViewById(R.id.tvConstellationFormalized);
        tvConstellationSource = findViewById(R.id.tvConstellationSource);
    }

    private void initializeStarStylePlanetViews() {
        tvStarName = findViewById(R.id.tvStarName);
        tvStarAlternateNames = findViewById(R.id.tvStarAlternateNames);
        tvRightAscension = findViewById(R.id.tvRightAscension);
        tvDeclination = findViewById(R.id.tvDeclination);
        tvMagnitude = findViewById(R.id.tvMagnitude);
        tvSpectralType = findViewById(R.id.tvSpectralType);
        tvDistance = findViewById(R.id.tvDistance);
        tvConstellation = findViewById(R.id.tvConstellation);
        tvApparentMagnitude = findViewById(R.id.tvApparentMagnitude);
        tvAbsoluteMagnitude = findViewById(R.id.tvAbsoluteMagnitude);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvRadius = findViewById(R.id.tvRadius);
        tvMass = findViewById(R.id.tvMass);
        tvLuminosity = findViewById(R.id.tvLuminosity);
        cardEducation = findViewById(R.id.cardEducation);
        tvEducationDisplayName = findViewById(R.id.tvEducationDisplayName);
        tvEducationConstellation = findViewById(R.id.tvEducationConstellation);
        tvEducationFunFact = findViewById(R.id.tvEducationFunFact);
        tvEducationHistory = findViewById(R.id.tvEducationHistory);
    }

    private void setupToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void loadContent() {
        String type = objectType;
        String name = objectName;
        String id = objectId;

        if (type == null || name == null) {
            Toast.makeText(this, R.string.education_missing_data, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (TYPE_CONSTELLATION.equals(type)) {
            ConstellationEducation education = repository.getConstellationByName(name);
            if (education == null && id != null) {
                education = repository.getConstellationById(id);
            }
            if (education == null) {
                Toast.makeText(this, R.string.education_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            bindConstellation(education);
        } else {
            SolarSystemEducation solar = repository.getSolarSystemByName(name);
            if (solar == null && id != null) {
                solar = repository.getSolarSystemById(id);
            }
            if (solar != null) {
                if (TYPE_PLANET.equals(type)) {
                    bindSolarSystemStarStyle(solar);
                } else {
                    bindSolarSystem(solar);
                }
                return;
            }

            PlanetEducation education = repository.getPlanetByName(name);
            if (education == null && id != null) {
                education = repository.getPlanetById(id);
            }
            if (education == null) {
                Toast.makeText(this, R.string.education_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            if (TYPE_PLANET.equals(type)) {
                bindPlanetLegacyStarStyle(education);
            } else {
                bindPlanet(education);
            }
        }
    }

    private void bindSolarSystemStarStyle(@NonNull SolarSystemEducation education) {
        if (tvStarName != null) {
            tvStarName.setText(education.getName());
        }
        if (tvStarAlternateNames != null) {
            tvStarAlternateNames.setVisibility(View.GONE);
        }
        if (tvMagnitude != null) {
            tvMagnitude.setText(emptyToDash(education.getApparentMagnitude()));
        }
        if (tvDistance != null) {
            tvDistance.setText(emptyToDash(education.getDistance()));
        }
        if (tvConstellation != null) {
            tvConstellation.setText(emptyToDash(education.getConstellation()));
        }
        if (tvRightAscension != null) {
            tvRightAscension.setText(formatRa(education.getRa()));
        }
        if (tvDeclination != null) {
            tvDeclination.setText(formatDec(education.getDec()));
        }
        if (tvSpectralType != null) {
            String epoch = education.getRaDecEpoch();
            String note = education.getRaDecNote();
            String value = epoch;
            if (!note.isEmpty()) {
                value = value.isEmpty() ? note : (epoch + " • " + note);
            }
            tvSpectralType.setText(emptyToDash(value));
        }
        if (tvApparentMagnitude != null) {
            tvApparentMagnitude.setText(emptyToDash(education.getApparentMagnitude()));
        }
        if (tvAbsoluteMagnitude != null) {
            tvAbsoluteMagnitude.setText(emptyToDash(education.getAbsoluteMagnitude()));
        }
        if (tvTemperature != null) {
            tvTemperature.setText(emptyToDash(education.getTemperature()));
        }
        if (tvRadius != null) {
            tvRadius.setText(emptyToDash(education.getRadius()));
        }
        if (tvMass != null) {
            tvMass.setText(emptyToDash(education.getMass()));
        }
        if (tvLuminosity != null) {
            tvLuminosity.setText(emptyToDash(education.getLuminosity()));
        }
        if (cardEducation != null) {
            cardEducation.setVisibility(View.VISIBLE);
        }
        if (tvEducationDisplayName != null) {
            tvEducationDisplayName.setText(education.getName());
        }
        if (tvEducationConstellation != null) {
            tvEducationConstellation.setText(emptyToDash(education.getConstellation()));
        }
        if (tvEducationFunFact != null) {
            tvEducationFunFact.setText(emptyToDash(education.getFunFact()));
        }
        if (tvEducationHistory != null) {
            tvEducationHistory.setText(emptyToDash(education.getHistory()));
        }
    }

    private void bindPlanetLegacyStarStyle(@NonNull PlanetEducation education) {
        if (tvStarName != null) {
            tvStarName.setText(education.getName());
        }
        if (tvStarAlternateNames != null) {
            tvStarAlternateNames.setVisibility(View.GONE);
        }
        if (cardEducation != null) {
            cardEducation.setVisibility(View.VISIBLE);
        }
        if (tvEducationDisplayName != null) {
            tvEducationDisplayName.setText(education.getName());
        }
        if (tvEducationConstellation != null) {
            tvEducationConstellation.setText("-");
        }
        if (tvEducationFunFact != null) {
            tvEducationFunFact.setText(emptyToDash(education.getFunFact()));
        }
        if (tvEducationHistory != null) {
            String history = buildBulletList(education.getFacts());
            if (history == null || history.trim().isEmpty()) {
                history = education.getHowToSpot();
            }
            tvEducationHistory.setText(emptyToDash(history));
        }
    }

    private void bindPlanet(@NonNull PlanetEducation education) {
        if (tvTitle != null) {
            tvTitle.setText(education.getName());
        }
        if (tvSubtitle != null) {
            String name = education.getName();
            if ("Sun".equalsIgnoreCase(name)) {
                tvSubtitle.setText(R.string.object_sun);
            } else if ("Moon".equalsIgnoreCase(name)) {
                tvSubtitle.setText(R.string.object_moon);
            } else {
                tvSubtitle.setText(R.string.object_planet);
            }
        }

        if (constellationSection != null) {
            constellationSection.setVisibility(View.GONE);
        }
        if (planetSection != null) {
            planetSection.setVisibility(View.VISIBLE);
        }
        setPlanetLegacyVisible(true);
        setPlanetCardsVisible(false);
        if (cardPlanetEducation != null) {
            cardPlanetEducation.setVisibility(View.VISIBLE);
        }

        setTextOrHide(tvPlanetSummary, education.getSummary());
        setTextOrHide(tvPlanetHowToSpot, education.getHowToSpot());
        setTextOrHide(tvPlanetFunFact, education.getFunFact());
        setTextOrHide(tvPlanetHistory, null);
        setTextOrHide(tvPlanetFacts, buildBulletList(education.getFacts()));
    }

    private void bindSolarSystem(@NonNull SolarSystemEducation education) {
        if (tvTitle != null) {
            tvTitle.setText(education.getName());
        }
        if (tvSubtitle != null) {
            String name = education.getName();
            if ("Sun".equalsIgnoreCase(name)) {
                tvSubtitle.setText(R.string.object_sun);
            } else if ("Moon".equalsIgnoreCase(name)) {
                tvSubtitle.setText(R.string.object_moon);
            } else {
                tvSubtitle.setText(R.string.object_planet);
            }
        }

        if (constellationSection != null) {
            constellationSection.setVisibility(View.GONE);
        }
        if (planetSection != null) {
            planetSection.setVisibility(View.VISIBLE);
        }
        setPlanetLegacyVisible(false);
        setPlanetCardsVisible(true);

        if (tvPlanetApparentMagnitude != null) {
            tvPlanetApparentMagnitude.setText(emptyToDash(education.getApparentMagnitude()));
        }
        if (tvPlanetDistance != null) {
            tvPlanetDistance.setText(emptyToDash(education.getDistance()));
        }
        if (tvPlanetConstellation != null) {
            tvPlanetConstellation.setText(emptyToDash(education.getConstellation()));
        }
        if (tvPlanetRightAscension != null) {
            tvPlanetRightAscension.setText(formatRa(education.getRa()));
        }
        if (tvPlanetDeclination != null) {
            tvPlanetDeclination.setText(formatDec(education.getDec()));
        }
        if (tvPlanetAbsoluteMagnitude != null) {
            tvPlanetAbsoluteMagnitude.setText(emptyToDash(education.getAbsoluteMagnitude()));
        }
        if (tvPlanetRaDecEpoch != null) {
            String epoch = education.getRaDecEpoch();
            String note = education.getRaDecNote();
            String extra = epoch;
            if (!note.isEmpty()) {
                extra = extra.isEmpty() ? note : (epoch + " • " + note);
            }
            tvPlanetRaDecEpoch.setText(emptyToDash(extra));
        }
        if (tvPlanetTemperature != null) {
            tvPlanetTemperature.setText(emptyToDash(education.getTemperature()));
        }
        if (tvPlanetRadius != null) {
            tvPlanetRadius.setText(emptyToDash(education.getRadius()));
        }
        if (tvPlanetMass != null) {
            tvPlanetMass.setText(emptyToDash(education.getMass()));
        }
        if (tvPlanetLuminosity != null) {
            tvPlanetLuminosity.setText(emptyToDash(education.getLuminosity()));
        }
        setTextOrHide(tvPlanetFunFact, education.getFunFact());
        setTextOrHide(tvPlanetHistory, education.getHistory());
    }

    private void bindConstellation(@NonNull ConstellationEducation education) {
        if (tvTitle != null) {
            tvTitle.setText(education.getName());
        }
        if (tvSubtitle != null) {
            tvSubtitle.setText(R.string.object_constellation);
        }

        if (planetSection != null) {
            planetSection.setVisibility(View.GONE);
        }
        if (constellationSection != null) {
            constellationSection.setVisibility(View.VISIBLE);
        }

        setTextOrHide(tvConstellationFamily, education.getFamily());
        setTextOrHide(tvConstellationBestMonth, education.getBestMonth());
        setTextOrHide(tvConstellationApproxCoordinates, education.getApproxCoordinates());
        setTextOrHide(tvConstellationBackgroundOrigin, education.getBackgroundOrigin());
        setTextOrHide(tvConstellationAsterism, education.getAsterism());
        setTextOrHide(tvConstellationMajorStars, education.getMajorStars());
        setTextOrHide(tvConstellationFormalized, education.getFormalizedDiscovered());
        setTextOrHide(tvConstellationSource, education.getSource());
    }

    private void setPlanetLegacyVisible(boolean visible) {
        if (planetLegacySection != null) {
            planetLegacySection.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void setPlanetCardsVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        if (cardPlanetQuickStats != null) {
            cardPlanetQuickStats.setVisibility(visibility);
        }
        if (cardPlanetPosition != null) {
            cardPlanetPosition.setVisibility(visibility);
        }
        if (cardPlanetPhysical != null) {
            cardPlanetPhysical.setVisibility(visibility);
        }
        if (cardPlanetEducation != null) {
            cardPlanetEducation.setVisibility(visibility);
        }
    }

    private void setTextOrHide(@Nullable TextView view, @Nullable String value) {
        if (view == null) return;
        if (value == null || value.trim().isEmpty()) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(value);
            view.setVisibility(View.VISIBLE);
        }
    }

    @NonNull
    private String emptyToDash(@Nullable String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    @NonNull
    private String formatRa(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        try {
            double raDegrees = Double.parseDouble(value);
            double hours = raDegrees / 15.0;
            int h = (int) hours;
            double minutesDecimal = (hours - h) * 60.0;
            int m = (int) minutesDecimal;
            double s = (minutesDecimal - m) * 60.0;
            return String.format("%dh %dm %.1fs", h, m, s);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    @NonNull
    private String formatDec(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        try {
            double decDegrees = Double.parseDouble(value);
            char sign = decDegrees >= 0 ? '+' : '-';
            decDegrees = Math.abs(decDegrees);
            int d = (int) decDegrees;
            double minutesDecimal = (decDegrees - d) * 60.0;
            int m = (int) minutesDecimal;
            double s = (minutesDecimal - m) * 60.0;
            return String.format("%c%d\u00b0 %d' %.1f\"", sign, d, m, s);
        } catch (NumberFormatException e) {
            return value;
        }
    }

    @Nullable
    private String buildBulletList(@NonNull List<String> items) {
        if (items.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            if (item == null || item.trim().isEmpty()) continue;
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("- ").append(item.trim());
        }
        return builder.length() == 0 ? null : builder.toString();
    }
}
