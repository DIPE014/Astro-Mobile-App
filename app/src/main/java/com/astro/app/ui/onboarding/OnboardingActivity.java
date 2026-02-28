package com.astro.app.ui.onboarding;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.astro.app.R;
import com.astro.app.databinding.ActivityOnboardingBinding;
import com.astro.app.ui.settings.SettingsViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity that displays a multi-step onboarding walkthrough for first-time users.
 *
 * <p>The onboarding flow consists of 6 pages that introduce the app's main features:
 * <ol>
 *   <li>Welcome - App logo and tagline</li>
 *   <li>Sky Map - AR sky viewing</li>
 *   <li>Controls - Bottom bar toggles</li>
 *   <li>Star Detection - Photo identification</li>
 *   <li>Drag and Zoom - Touch gestures</li>
 *   <li>Search - Find celestial objects</li>
 * </ol>
 * </p>
 *
 * <p>Uses ViewPager2 with dot indicators and Skip/Next/Done buttons.
 * Completion is persisted via SharedPreferences so the onboarding is
 * only shown on first launch (unless replayed from Settings).</p>
 */
public class OnboardingActivity extends AppCompatActivity {

    /** SharedPreferences key for onboarding completion flag. */
    public static final String KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding";

    private ActivityOnboardingBinding binding;
    private OnboardingPagerAdapter adapter;
    private List<OnboardingPagerAdapter.OnboardingPage> pages;
    private ImageView[] dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupPages();
        setupViewPager();
        setupDotIndicators();
        setupButtons();

        // Initialize to first page state
        updateDotIndicators(0);
        updateButtons(0);
    }

    /**
     * Sets up the list of onboarding pages with their icons, titles, and descriptions.
     * Uses Android system drawables for icons, consistent with the rest of the app.
     */
    private void setupPages() {
        pages = new ArrayList<>();
        pages.add(new OnboardingPagerAdapter.OnboardingPage(
                android.R.drawable.btn_star_big_on,
                R.string.onboarding_welcome_title,
                R.string.onboarding_welcome_description));
        pages.add(new OnboardingPagerAdapter.OnboardingPage(
                android.R.drawable.ic_menu_compass,
                R.string.onboarding_sky_map_title,
                R.string.onboarding_sky_map_description));
        pages.add(new OnboardingPagerAdapter.OnboardingPage(
                android.R.drawable.ic_menu_manage,
                R.string.onboarding_controls_title,
                R.string.onboarding_controls_description));
        pages.add(new OnboardingPagerAdapter.OnboardingPage(
                android.R.drawable.ic_menu_camera,
                R.string.onboarding_star_detection_title,
                R.string.onboarding_star_detection_description));
        pages.add(new OnboardingPagerAdapter.OnboardingPage(
                android.R.drawable.ic_menu_zoom,
                R.string.onboarding_drag_zoom_title,
                R.string.onboarding_drag_zoom_description));
        pages.add(new OnboardingPagerAdapter.OnboardingPage(
                android.R.drawable.ic_menu_search,
                R.string.onboarding_search_title,
                R.string.onboarding_search_description));
        pages.add(new OnboardingPagerAdapter.OnboardingPage(
                android.R.drawable.ic_menu_slideshow,
                R.string.onboarding_sky_quality_title,
                R.string.onboarding_sky_quality_description));
    }

    /**
     * Configures the ViewPager2 with the adapter and page change callback.
     */
    private void setupViewPager() {
        adapter = new OnboardingPagerAdapter(pages);
        binding.viewPager.setAdapter(adapter);

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDotIndicators(position);
                updateButtons(position);
            }
        });
    }

    /**
     * Creates the dot indicators at the bottom of the screen.
     * One dot per page, using a small circle shape.
     */
    private void setupDotIndicators() {
        dots = new ImageView[pages.size()];
        LinearLayout container = binding.dotIndicatorContainer;
        container.removeAllViews();

        for (int i = 0; i < pages.size(); i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageResource(android.R.drawable.presence_invisible);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            dots[i].setLayoutParams(params);
            container.addView(dots[i]);
        }
    }

    /**
     * Updates the dot indicators to reflect the current page position.
     * The active dot is highlighted with the primary color; inactive dots use a muted color.
     *
     * @param position The index of the currently selected page
     */
    private void updateDotIndicators(int position) {
        for (int i = 0; i < dots.length; i++) {
            if (i == position) {
                dots[i].setImageResource(android.R.drawable.radiobutton_on_background);
                dots[i].setColorFilter(getColor(R.color.primary));
            } else {
                dots[i].setImageResource(android.R.drawable.radiobutton_off_background);
                dots[i].setColorFilter(getColor(R.color.text_tertiary));
            }
        }
    }

    /**
     * Updates the button states based on the current page position.
     * Shows "Next" for intermediate pages and "Done" on the last page.
     * The Skip button is hidden on the last page.
     *
     * @param position The index of the currently selected page
     */
    private void updateButtons(int position) {
        boolean isLastPage = position == pages.size() - 1;
        binding.btnNext.setText(isLastPage ? R.string.onboarding_done : R.string.onboarding_next);
        binding.btnSkip.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
    }

    /**
     * Sets up the Skip and Next/Done button click listeners.
     */
    private void setupButtons() {
        binding.btnSkip.setOnClickListener(v -> completeOnboarding());

        binding.btnNext.setOnClickListener(v -> {
            int currentItem = binding.viewPager.getCurrentItem();
            if (currentItem < pages.size() - 1) {
                binding.viewPager.setCurrentItem(currentItem + 1, true);
            } else {
                completeOnboarding();
            }
        });
    }

    /**
     * Marks onboarding as completed in SharedPreferences and finishes this activity.
     * The flag is stored in the same SharedPreferences file used by SettingsViewModel
     * to keep all app preferences in one place.
     */
    private void completeOnboarding() {
        SharedPreferences prefs = getSharedPreferences(
                SettingsViewModel.PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, true).apply();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
