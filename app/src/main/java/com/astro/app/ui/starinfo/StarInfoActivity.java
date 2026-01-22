package com.astro.app.ui.starinfo;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.astro.app.AstroApplication;
import com.astro.app.R;
import com.astro.app.data.model.ConstellationData;
import com.astro.app.data.model.StarData;
import com.astro.app.data.repository.ConstellationRepository;
import com.astro.app.data.repository.StarRepository;
import com.google.android.material.button.MaterialButton;

import java.util.List;

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
    private TextView tvRightAscensionHms;
    private TextView tvDeclination;
    private TextView tvDeclinationDms;
    private TextView tvMagnitude;
    private TextView tvSpectralType;
    private TextView tvDistance;
    private TextView tvVisibleToNakedEye;
    private TextView tvConstellationName;
    private View constellationSection;
    private View relatedStarsSection;
    private RecyclerView rvRelatedStars;
    private ProgressBar progressBar;
    private View starColorIndicator;

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
        tvRightAscensionHms = findViewById(R.id.tvRightAscensionHms);
        tvDeclination = findViewById(R.id.tvDeclination);
        tvDeclinationDms = findViewById(R.id.tvDeclinationDms);
        tvMagnitude = findViewById(R.id.tvMagnitude);
        tvSpectralType = findViewById(R.id.tvSpectralType);
        tvDistance = findViewById(R.id.tvDistance);
        tvVisibleToNakedEye = findViewById(R.id.tvVisibleToNakedEye);
        tvConstellationName = findViewById(R.id.tvConstellationName);
        constellationSection = findViewById(R.id.constellationSection);
        relatedStarsSection = findViewById(R.id.relatedStarsSection);
        rvRelatedStars = findViewById(R.id.rvRelatedStars);
        progressBar = findViewById(R.id.progressBar);
        starColorIndicator = findViewById(R.id.starColorIndicator);

        // Setup RecyclerView for related stars
        if (rvRelatedStars != null) {
            rvRelatedStars.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    /**
     * Sets up click listeners for UI elements.
     */
    private void setupClickListeners() {
        // Back button
        MaterialButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Constellation section click - could navigate to constellation details
        if (constellationSection != null) {
            constellationSection.setOnClickListener(v -> {
                ConstellationData constellation = viewModel.getConstellation().getValue();
                if (constellation != null) {
                    // TODO: Navigate to constellation details
                    Toast.makeText(this,
                            getString(R.string.constellation_info, constellation.getName()),
                            Toast.LENGTH_SHORT).show();
                }
            });
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

        // Observe related stars
        viewModel.getRelatedStars().observe(this, this::displayRelatedStars);

        // Observe loading state
        viewModel.isLoading().observe(this, loading -> {
            if (progressBar != null) {
                progressBar.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
            }
        });

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
            tvRightAscension.setText(details.getFormattedRa());
        }
        if (tvRightAscensionHms != null) {
            tvRightAscensionHms.setText(details.getFormattedRaHms());
        }

        // Declination
        if (tvDeclination != null) {
            tvDeclination.setText(details.getFormattedDec());
        }
        if (tvDeclinationDms != null) {
            tvDeclinationDms.setText(details.getFormattedDecDms());
        }

        // Magnitude
        if (tvMagnitude != null) {
            tvMagnitude.setText(details.getFormattedMagnitude());
        }

        // Spectral type
        if (tvSpectralType != null) {
            tvSpectralType.setText(details.getFormattedSpectralType());
        }

        // Star color indicator
        if (starColorIndicator != null) {
            starColorIndicator.setBackgroundColor(details.getSpectralColor());
        }

        // Distance
        if (tvDistance != null) {
            tvDistance.setText(details.getFormattedDistance());
        }

        // Visible to naked eye
        if (tvVisibleToNakedEye != null) {
            tvVisibleToNakedEye.setText(details.getVisibleToNakedEye());
        }
    }

    /**
     * Displays constellation information.
     */
    private void displayConstellation(@Nullable ConstellationData constellation) {
        if (constellation == null) {
            if (constellationSection != null) {
                constellationSection.setVisibility(View.GONE);
            }
            return;
        }

        if (constellationSection != null) {
            constellationSection.setVisibility(View.VISIBLE);
        }

        if (tvConstellationName != null) {
            tvConstellationName.setText(constellation.getName());
        }
    }

    /**
     * Displays related stars in the same constellation.
     */
    private void displayRelatedStars(@Nullable List<StarData> relatedStars) {
        if (relatedStars == null || relatedStars.isEmpty()) {
            if (relatedStarsSection != null) {
                relatedStarsSection.setVisibility(View.GONE);
            }
            return;
        }

        if (relatedStarsSection != null) {
            relatedStarsSection.setVisibility(View.VISIBLE);
        }

        // Setup adapter for related stars
        if (rvRelatedStars != null) {
            RelatedStarsAdapter adapter = new RelatedStarsAdapter(relatedStars, star -> {
                // Navigate to selected star
                viewModel.loadStarById(star.getId());
            });
            rvRelatedStars.setAdapter(adapter);
        }
    }

    /**
     * Adapter for displaying related stars in a RecyclerView.
     */
    private static class RelatedStarsAdapter extends RecyclerView.Adapter<RelatedStarsAdapter.ViewHolder> {

        private final List<StarData> stars;
        private final OnStarClickListener listener;

        interface OnStarClickListener {
            void onStarClick(StarData star);
        }

        RelatedStarsAdapter(List<StarData> stars, OnStarClickListener listener) {
            this.stars = stars;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_related_star, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            StarData star = stars.get(position);
            holder.bind(star, listener);
        }

        @Override
        public int getItemCount() {
            return stars.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvName;
            private final TextView tvMagnitude;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvRelatedStarName);
                tvMagnitude = itemView.findViewById(R.id.tvRelatedStarMagnitude);
            }

            void bind(StarData star, OnStarClickListener listener) {
                if (tvName != null) {
                    tvName.setText(star.getName());
                }
                if (tvMagnitude != null) {
                    tvMagnitude.setText(String.format("Mag: %.2f", star.getMagnitude()));
                }
                itemView.setOnClickListener(v -> listener.onStarClick(star));
            }
        }
    }
}
