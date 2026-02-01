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

    private View planetSection;
    private TextView tvPlanetSummary;
    private TextView tvPlanetFacts;
    private TextView tvPlanetHowToSpot;
    private TextView tvPlanetFunFact;

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
        setContentView(R.layout.activity_education_detail);

        repository = new EducationRepository(getAssets());

        initializeViews();
        setupToolbar();
        loadContent();
    }

    private void initializeViews() {
        tvTitle = findViewById(R.id.tvEducationTitle);
        tvSubtitle = findViewById(R.id.tvEducationSubtitle);

        planetSection = findViewById(R.id.sectionPlanet);
        tvPlanetSummary = findViewById(R.id.tvPlanetSummary);
        tvPlanetFacts = findViewById(R.id.tvPlanetFacts);
        tvPlanetHowToSpot = findViewById(R.id.tvPlanetHowToSpot);
        tvPlanetFunFact = findViewById(R.id.tvPlanetFunFact);

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

    private void setupToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void loadContent() {
        String type = getIntent().getStringExtra(EXTRA_OBJECT_TYPE);
        String name = getIntent().getStringExtra(EXTRA_OBJECT_NAME);
        String id = getIntent().getStringExtra(EXTRA_OBJECT_ID);

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
            PlanetEducation education = repository.getPlanetByName(name);
            if (education == null && id != null) {
                education = repository.getPlanetById(id);
            }
            if (education == null) {
                Toast.makeText(this, R.string.education_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            bindPlanet(education);
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

        setTextOrHide(tvPlanetSummary, education.getSummary());
        setTextOrHide(tvPlanetHowToSpot, education.getHowToSpot());
        setTextOrHide(tvPlanetFunFact, education.getFunFact());
        setTextOrHide(tvPlanetFacts, buildBulletList(education.getFacts()));
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

    private void setTextOrHide(@Nullable TextView view, @Nullable String value) {
        if (view == null) return;
        if (value == null || value.trim().isEmpty()) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(value);
            view.setVisibility(View.VISIBLE);
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
