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

    /**
     * Initializes the activity: sets the layout, performs dependency injection, and prepares the search UI and index.
     *
     * @param savedInstanceState previously saved state, if any
     */
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
     * Binds activity UI elements, wires back/clear button behavior, initializes the results RecyclerView with its adapter, and shows the empty state.
     *
     * Finds and assigns the search input, results list, empty and loading views; sets the back button to finish the activity and the clear button to clear and refocus the search field; creates and attaches the SearchResultAdapter with a LinearLayoutManager.
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
         * Attach listeners to the search input to drive live autocomplete and UI controls.
         *
         * <p>On text change, triggers a search with the current text. After text is changed,
         * toggles the clear button visibility based on whether the input is empty. Handles the
         * keyboard search action (IME_ACTION_SEARCH) by performing a search with the current query.
         */
    private void setupListeners() {
        // Text change listener for autocomplete
        etSearch.addTextChangedListener(new TextWatcher() {
            /**
             * Invoked immediately before the text is changed.
             *
             * @param s the current text content
             * @param start the start index of the change
             * @param count the number of characters that will be replaced
             * @param after the number of characters that will replace the old text
             */
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            /**
             * Invoked when the input text changes and triggers a search using the current text.
             *
             * @param s the current text in the search input
             * @param start the start position of the change
             * @param before the length of the previous text that was replaced
             * @param count the length of the new text that was inserted
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            /**
             * Toggles the visibility of the clear button (R.id.btnClear) based on the provided text:
             * shows the button when the text length is greater than zero and hides it when empty.
             *
             * @param s the current text content of the input field
             */
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
         * Updates the UI with autocomplete search results for the given query.
         *
         * <p>If the search index is not built this method no-ops. An empty query clears results
         * and shows the empty state. If no suggestions are found it shows the no-results state.
         * Otherwise it resolves suggestions to SearchResult objects, orders them with prefix
         * (exact) matches first and then alphabetically, limits the list to 50 items, updates
         * the adapter, and displays the results view.</p>
         *
         * @param query the user-entered search string to query against the search index
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
     * Display the search empty state by showing the empty-state view, hiding the results list and loading indicator, and updating the empty title and subtitle text.
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
     * Display the "no results" empty state, hide the results list and loading indicator.
     *
     * Updates the empty state's title to the localized "no results" string and clears its subtitle.
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
         * Display the loading UI and hide the empty-state and results views.
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

    /**
     * Handle a selected search result by returning its details to the calling activity and finishing.
     *
     * @param result the selected SearchResult whose name, coordinates, type, and optional id will be returned
     */
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