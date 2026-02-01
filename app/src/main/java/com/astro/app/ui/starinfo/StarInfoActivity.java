package com.astro.app.ui.starinfo;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.astro.app.AstroApplication;
import com.astro.app.R;
import com.astro.app.data.model.ConstellationData;
import com.astro.app.data.model.StarData;
import com.astro.app.data.repository.ConstellationRepository;
import com.astro.app.data.repository.StarRepository;

import javax.inject.Inject;

/**
 * Activity for displaying detailed information about a star.
 *
 * <p>This activity shows:
 * <ul>
 *   <li>Star name and alternate names</li>
 *   <li>Celestial coordinates (RA/Dec)</li>
 *   <li>Magnitude (brightness)</li>
 *   <li>Spectral type and color</li>
 *   <li>Distance from Earth</li>
 *   <li>Constellation information (if applicable)</li>
 *   <li>Related stars in the same constellation</li>
 * </ul>
 * </p>
 *
 * <p>Uses StarInfoViewModel for data management and LiveData for UI updates.</p>
 */
public class StarInfoActivity extends AppCompatActivity {

    // Intent extra keys
    public static final String EXTRA_STAR_ID = "star_id";
    public static final String EXTRA_STAR_NAME = "star_name";
    public static final String EXTRA_STAR_RA = "star_ra";
    public static final String EXTRA_STAR_DEC = "star_dec";
    public static final String EXTRA_STAR_MAGNITUDE = "star_magnitude";

    // ViewModel
    private StarInfoViewModel viewModel;

    // Injected dependencies
    @Inject
    StarRepository starRepository;

    @Inject
    ConstellationRepository constellationRepository;

    // Views
    private TextView tvStarName;
    private TextView tvStarAlternateNames;
    private TextView tvRightAscension;
    private TextView tvDeclination;
    private TextView tvMagnitude;
    private TextView tvSpectralType;
    private TextView tvDistance;
    private TextView tvConstellation;
    private View cardEducation;
    private TextView tvEducationDisplayName;
    private TextView tvEducationConstellation;
    private TextView tvEducationFunFact;
    private TextView tvEducationHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_star_info);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StarInfoViewModel.class);

        // Inject dependencies
        ((AstroApplication) getApplication()).getAppComponent().inject(this);

        // Set dependencies on ViewModel
        viewModel.setStarRepository(starRepository);
        viewModel.setConstellationRepository(constellationRepository);

        initializeViews();
        setupClickListeners();
        observeViewModel();
        loadStarData();
    }

    /**
     * Initializes view references.
     */
    private void initializeViews() {
        tvStarName = findViewById(R.id.tvStarName);
        tvStarAlternateNames = findViewById(R.id.tvStarAlternateNames);
        tvRightAscension = findViewById(R.id.tvRightAscension);
        tvDeclination = findViewById(R.id.tvDeclination);
        tvMagnitude = findViewById(R.id.tvMagnitude);
        tvSpectralType = findViewById(R.id.tvSpectralType);
        tvDistance = findViewById(R.id.tvDistance);
        tvConstellation = findViewById(R.id.tvConstellation);
        cardEducation = findViewById(R.id.cardEducation);
        tvEducationDisplayName = findViewById(R.id.tvEducationDisplayName);
        tvEducationConstellation = findViewById(R.id.tvEducationConstellation);
        tvEducationFunFact = findViewById(R.id.tvEducationFunFact);
        tvEducationHistory = findViewById(R.id.tvEducationHistory);
    }

    /**
     * Sets up click listeners for UI elements.
     */
    private void setupClickListeners() {
        // Setup toolbar navigation
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    /**
     * Observes LiveData from the ViewModel.
     */
    private void observeViewModel() {
        // Observe star details
        viewModel.getStarDetails().observe(this, this::displayStarDetails);

        // Observe constellation
        viewModel.getConstellation().observe(this, this::displayConstellation);

        // Observe education content
        viewModel.getEducationStar().observe(this, this::displayEducationContent);

        // Observe errors
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });
    }

    /**
     * Loads star data from intent extras.
     */
    private void loadStarData() {
        String starId = getIntent().getStringExtra(EXTRA_STAR_ID);
        String starName = getIntent().getStringExtra(EXTRA_STAR_NAME);
        float ra = getIntent().getFloatExtra(EXTRA_STAR_RA, 0);
        float dec = getIntent().getFloatExtra(EXTRA_STAR_DEC, 0);
        float magnitude = getIntent().getFloatExtra(EXTRA_STAR_MAGNITUDE, 0);

        if (starId != null && !starId.isEmpty()) {
            // Load full star data by ID
            viewModel.loadStarById(starId);
        } else if (starName != null && !starName.isEmpty()) {
            // Load from intent extras
            viewModel.loadFromExtras(starName, ra, dec, magnitude);
        } else {
            Toast.makeText(this, R.string.error_no_star_data, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Displays star details in the UI.
     */
    private void displayStarDetails(@Nullable StarInfoViewModel.StarDetails details) {
        if (details == null) return;

        // Name
        if (tvStarName != null) {
            tvStarName.setText(details.getName());
        }

        // Alternate names
        if (tvStarAlternateNames != null) {
            StarData star = details.getStarData();
            if (star.hasAlternateNames()) {
                String names = String.join(", ", star.getAlternateNames());
                tvStarAlternateNames.setText(names);
                tvStarAlternateNames.setVisibility(View.VISIBLE);
            } else {
                tvStarAlternateNames.setVisibility(View.GONE);
            }
        }

        // Right Ascension
        if (tvRightAscension != null) {
            tvRightAscension.setText(details.getFormattedRaHms());
        }

        // Declination
        if (tvDeclination != null) {
            tvDeclination.setText(details.getFormattedDecDms());
        }

        // Magnitude
        if (tvMagnitude != null) {
            tvMagnitude.setText(details.getFormattedMagnitude());
        }

        // Spectral type
        if (tvSpectralType != null) {
            tvSpectralType.setText(details.getFormattedSpectralType());
        }

        // Distance
        if (tvDistance != null) {
            tvDistance.setText(details.getFormattedDistance());
        }
    }

    /**
     * Displays constellation information.
     */
    private void displayConstellation(@Nullable ConstellationData constellation) {
        if (constellation == null) {
            return;
        }

        if (tvConstellation != null) {
            tvConstellation.setText(constellation.getName());
        }
    }

    /**
     * Displays education content if available.
     */
    private void displayEducationContent(@Nullable StarInfoViewModel.EducationStar educationStar) {
        if (cardEducation != null) {
            cardEducation.setVisibility(educationStar != null ? View.VISIBLE : View.GONE);
        }
        if (educationStar == null) {
            return;
        }

        if (tvEducationDisplayName != null) {
            tvEducationDisplayName.setText(educationStar.getDisplayName());
        }
        if (tvEducationConstellation != null) {
            tvEducationConstellation.setText(educationStar.getConstellation());
        }
        if (tvConstellation != null) {
            tvConstellation.setText(educationStar.getConstellation());
        }
        if (tvEducationFunFact != null) {
            tvEducationFunFact.setText(educationStar.getFunFact());
        }
        if (tvEducationHistory != null) {
            tvEducationHistory.setText(educationStar.getHistory());
        }
    }
}
