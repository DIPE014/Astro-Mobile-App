package com.astro.app.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.astro.app.AstroApplication;
import com.astro.app.R;
import com.astro.app.core.control.space.Universe;
import com.astro.app.data.repository.ConstellationRepository;
import com.astro.app.data.repository.StarRepository;
import com.astro.app.search.SearchIndex;
import com.astro.app.search.SearchResult;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Search activity for finding celestial objects.
 *
 * <p>Provides an autocomplete search interface using Material Design 3 components.
 * Users can search for stars, planets, and constellations by name.</p>
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>Real-time autocomplete suggestions</li>
 *   <li>Category icons (star, planet, constellation)</li>
 *   <li>Returns selected result to calling activity</li>
 * </ul>
 */
public class SearchActivity extends AppCompatActivity implements SearchResultAdapter.OnResultClickListener {

    private static final String TAG = "SearchActivity";

    /** Extra key for the selected result name */
    public static final String EXTRA_RESULT_NAME = "result_name";

    /** Extra key for the selected result RA */
    public static final String EXTRA_RESULT_RA = "result_ra";

    /** Extra key for the selected result Dec */
    public static final String EXTRA_RESULT_DEC = "result_dec";

    /** Extra key for the selected result type */
    public static final String EXTRA_RESULT_TYPE = "result_type";

    /** Extra key for the selected result object ID */
    public static final String EXTRA_RESULT_ID = "result_id";

    // Injected dependencies
    @Inject
    StarRepository starRepository;

    @Inject
    ConstellationRepository constellationRepository;

    @Inject
    Universe universe;

    // Views
    private TextInputEditText etSearch;
    private RecyclerView rvResults;
    private View emptyState;
    private View loadingState;

    // Search
    private SearchIndex searchIndex;
    private SearchResultAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Inject dependencies
        ((AstroApplication) getApplication()).getAppComponent().inject(this);

        initializeViews();
        initializeSearchIndex();
        setupListeners();
    }

    /**
     * Initializes view references.
     */
    private void initializeViews() {
        etSearch = findViewById(R.id.etSearch);
        rvResults = findViewById(R.id.rvResults);
        emptyState = findViewById(R.id.emptyState);
        loadingState = findViewById(R.id.loadingState);

        // Back button
        MaterialButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Clear button
        ImageView btnClear = findViewById(R.id.btnClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                etSearch.setText("");
                etSearch.requestFocus();
            });
        }

        // Setup RecyclerView
        adapter = new SearchResultAdapter(this);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(adapter);

        // Show empty state initially
        showEmptyState();
    }

    /**
     * Initializes the search index.
     */
    private void initializeSearchIndex() {
        showLoading();

        // Build index on background thread
        new Thread(() -> {
            searchIndex = new SearchIndex(starRepository, constellationRepository, universe);
            searchIndex.buildIndex();

            runOnUiThread(() -> {
                hideLoading();
                showEmptyState();
                Log.d(TAG, "Search index ready with " + searchIndex.size() + " entries");
            });
        }).start();
    }

    /**
     * Sets up event listeners.
     */
    private void setupListeners() {
        // Text change listener for autocomplete
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Show/hide clear button
                ImageView btnClear = findViewById(R.id.btnClear);
                if (btnClear != null) {
                    btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                }
            }
        });

        // Search action on keyboard
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText() != null ? etSearch.getText().toString() : "";
                performSearch(query);
                return true;
            }
            return false;
        });
    }

    /**
     * Performs a search with the given query.
     *
     * @param query The search query
     */
    private void performSearch(String query) {
        if (searchIndex == null || !searchIndex.isIndexBuilt()) {
            return;
        }

        if (query.isEmpty()) {
            showEmptyState();
            adapter.setResults(new ArrayList<>());
            return;
        }

        // Get autocomplete suggestions
        Set<String> suggestions = searchIndex.getAutocompleteSuggestions(query);

        if (suggestions.isEmpty()) {
            showNoResults();
            adapter.setResults(new ArrayList<>());
            return;
        }

        // Convert suggestions to search results
        List<SearchResult> results = new ArrayList<>();
        for (String suggestion : suggestions) {
            SearchResult result = searchIndex.getByName(suggestion);
            if (result != null) {
                results.add(result);
            }
        }

        // Sort results: exact matches first, then alphabetically
        String queryLower = query.toLowerCase();
        results.sort((a, b) -> {
            boolean aStartsWith = a.getName().toLowerCase().startsWith(queryLower);
            boolean bStartsWith = b.getName().toLowerCase().startsWith(queryLower);
            if (aStartsWith && !bStartsWith) return -1;
            if (!aStartsWith && bStartsWith) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        // Limit results
        if (results.size() > 50) {
            results = results.subList(0, 50);
        }

        adapter.setResults(results);
        showResults();

        Log.d(TAG, "Found " + results.size() + " results for '" + query + "'");
    }

    /**
     * Shows the empty state (initial state).
     */
    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        rvResults.setVisibility(View.GONE);
        loadingState.setVisibility(View.GONE);

        // Update empty state text
        TextView tvEmptyTitle = emptyState.findViewById(R.id.tvEmptyTitle);
        TextView tvEmptySubtitle = emptyState.findViewById(R.id.tvEmptySubtitle);
        if (tvEmptyTitle != null) {
            tvEmptyTitle.setText(R.string.empty_search);
        }
        if (tvEmptySubtitle != null) {
            tvEmptySubtitle.setText(R.string.empty_search_description);
        }
    }

    /**
     * Shows the no results state.
     */
    private void showNoResults() {
        emptyState.setVisibility(View.VISIBLE);
        rvResults.setVisibility(View.GONE);
        loadingState.setVisibility(View.GONE);

        // Update empty state text
        TextView tvEmptyTitle = emptyState.findViewById(R.id.tvEmptyTitle);
        TextView tvEmptySubtitle = emptyState.findViewById(R.id.tvEmptySubtitle);
        if (tvEmptyTitle != null) {
            tvEmptyTitle.setText(R.string.search_no_results);
        }
        if (tvEmptySubtitle != null) {
            tvEmptySubtitle.setText("");
        }
    }

    /**
     * Shows the results list.
     */
    private void showResults() {
        emptyState.setVisibility(View.GONE);
        rvResults.setVisibility(View.VISIBLE);
        loadingState.setVisibility(View.GONE);
    }

    /**
     * Shows the loading state.
     */
    private void showLoading() {
        emptyState.setVisibility(View.GONE);
        rvResults.setVisibility(View.GONE);
        loadingState.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the loading state.
     */
    private void hideLoading() {
        loadingState.setVisibility(View.GONE);
    }

    @Override
    public void onResultClick(@NonNull SearchResult result) {
        Log.d(TAG, "Selected: " + result.getName());

        // Return result to calling activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_RESULT_NAME, result.getName());
        resultIntent.putExtra(EXTRA_RESULT_RA, result.getRa());
        resultIntent.putExtra(EXTRA_RESULT_DEC, result.getDec());
        resultIntent.putExtra(EXTRA_RESULT_TYPE, result.getObjectType().name());
        if (result.getObjectId() != null) {
            resultIntent.putExtra(EXTRA_RESULT_ID, result.getObjectId());
        }

        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
