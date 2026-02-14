package com.astro.app.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.astro.app.R;

import java.util.List;

/**
 * RecyclerView adapter for the onboarding ViewPager2.
 *
 * <p>Each page displays an icon, title, and description to introduce
 * the user to the app's features during the first-launch walkthrough.</p>
 */
public class OnboardingPagerAdapter extends RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder> {

    private final List<OnboardingPage> pages;

    /**
     * Creates a new OnboardingPagerAdapter.
     *
     * @param pages The list of onboarding pages to display
     */
    public OnboardingPagerAdapter(@NonNull List<OnboardingPage> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_page, parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        OnboardingPage page = pages.get(position);
        holder.bind(page);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    /**
     * ViewHolder for an onboarding page.
     */
    static class OnboardingViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivIcon;
        private final TextView tvTitle;
        private final TextView tvDescription;

        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivOnboardingIcon);
            tvTitle = itemView.findViewById(R.id.tvOnboardingTitle);
            tvDescription = itemView.findViewById(R.id.tvOnboardingDescription);
        }

        /**
         * Binds an onboarding page's data to the views.
         *
         * @param page The onboarding page data
         */
        void bind(@NonNull OnboardingPage page) {
            ivIcon.setImageResource(page.iconResId);
            tvTitle.setText(page.titleResId);
            tvDescription.setText(page.descriptionResId);
        }
    }

    /**
     * Data class representing a single onboarding page.
     */
    public static class OnboardingPage {

        @DrawableRes
        public final int iconResId;

        @StringRes
        public final int titleResId;

        @StringRes
        public final int descriptionResId;

        /**
         * Creates a new OnboardingPage.
         *
         * @param iconResId        The drawable resource ID for the page icon
         * @param titleResId       The string resource ID for the page title
         * @param descriptionResId The string resource ID for the page description
         */
        public OnboardingPage(@DrawableRes int iconResId,
                              @StringRes int titleResId,
                              @StringRes int descriptionResId) {
            this.iconResId = iconResId;
            this.titleResId = titleResId;
            this.descriptionResId = descriptionResId;
        }
    }
}
