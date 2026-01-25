package com.astro.app.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.astro.app.R;
import com.astro.app.search.SearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying search results.
 *
 * <p>Shows each result with an icon indicating the object type (star, planet,
 * constellation), the object name, and its type as a subtitle.</p>
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    /**
     * Interface for handling result clicks.
     */
    public interface OnResultClickListener {
        /**
 * Called when the user selects a search result item.
 *
 * @param result the selected {@link SearchResult}; never null
 */
void onResultClick(@NonNull SearchResult result);
    }

    private final List<SearchResult> results = new ArrayList<>();
    private final OnResultClickListener listener;

    /**
     * Creates a SearchResultAdapter that notifies the provided listener when a result item is clicked.
     *
     * @param listener the non-null listener invoked for result item clicks
     */
    public SearchResultAdapter(@NonNull OnResultClickListener listener) {
        this.listener = listener;
    }

    /**
     * Replace the adapter's current search results with the provided list and refresh the displayed items.
     *
     * @param newResults the new search results to display (must not be null)
     */
    public void setResults(@NonNull List<SearchResult> newResults) {
        results.clear();
        results.addAll(newResults);
        notifyDataSetChanged();
    }

    /**
     * Inflates the item_search_result layout and returns a ViewHolder for it.
     *
     * @param parent   the parent ViewGroup providing layout params and context
     * @param viewType the view type for the new view
     * @return         a new ViewHolder wrapping the inflated item view
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds the search result for the given adapter position to the provided ViewHolder.
     *
     * @param holder the ViewHolder to populate with data
     * @param position the adapter position of the item to bind
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = results.get(position);
        holder.bind(result, listener);
    }

    /**
     * Number of items currently managed by the adapter.
     *
     * @return the number of SearchResult items in the adapter
     */
    @Override
    public int getItemCount() {
        return results.size();
    }

    /**
     * ViewHolder for search result items.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvName;
        private final TextView tvType;
        private final TextView tvCoords;

        /**
         * Initializes view references for a search result item view.
         *
         * @param itemView the root view of the search result item used to locate child views
         */
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivResultIcon);
            tvName = itemView.findViewById(R.id.tvResultName);
            tvType = itemView.findViewById(R.id.tvResultType);
            tvCoords = itemView.findViewById(R.id.tvResultCoords);
        }

        /**
         * Populates the ViewHolder's views with the given search result and wires the item click callback.
         *
         * Sets the name, type, formatted coordinates, type-specific icon and icon color, and attaches
         * the provided listener to handle item clicks.
         *
         * @param result   the SearchResult to display
         * @param listener callback invoked when the item is clicked
         */
        void bind(@NonNull SearchResult result, @NonNull OnResultClickListener listener) {
            tvName.setText(result.getName());
            tvType.setText(result.getObjectType().getDisplayName());

            // Format coordinates
            String coords = formatCoords(result.getRa(), result.getDec());
            tvCoords.setText(coords);

            // Set icon based on type
            int iconRes = getIconForType(result.getObjectType());
            ivIcon.setImageResource(iconRes);

            // Set icon color based on type
            int colorRes = getColorForType(result.getObjectType());
            ivIcon.setColorFilter(itemView.getContext().getColor(colorRes));

            // Click listener
            itemView.setOnClickListener(v -> listener.onResultClick(result));
        }

        /**
         * Format celestial coordinates (right ascension and declination) into a human-readable string.
         *
         * @param ra  right ascension in degrees
         * @param dec declination in degrees
         * @return    a string in the form "Hh Mm, ±D° M'" where RA is converted to hours and minutes and Dec is shown with sign, degrees, and arcminutes
         */
        private String formatCoords(float ra, float dec) {
            // Convert RA to hours
            float raHours = ra / 15f;
            int hours = (int) raHours;
            int minutes = (int) ((raHours - hours) * 60);

            // Format Dec
            String sign = dec >= 0 ? "+" : "";
            int decDeg = (int) dec;
            int decMin = (int) (Math.abs(dec - decDeg) * 60);

            return String.format("%dh %dm, %s%d° %d'", hours, minutes, sign, decDeg, decMin);
        }

        /**
         * Selects a drawable resource id that represents the given search result object type.
         *
         * @param type the SearchResult.ObjectType to map to an icon
         * @return the Android drawable resource id corresponding to the object type
         */
        private int getIconForType(SearchResult.ObjectType type) {
            switch (type) {
                case STAR:
                    return android.R.drawable.btn_star;
                case PLANET:
                    return android.R.drawable.presence_online;
                case CONSTELLATION:
                    return android.R.drawable.ic_menu_mapmode;
                case MOON:
                    return android.R.drawable.ic_menu_month;
                case SUN:
                    return android.R.drawable.ic_menu_day;
                default:
                    return android.R.drawable.ic_menu_compass;
            }
        }

        /**
         * Selects the color resource associated with a search result object type.
         *
         * @param type the object's type whose color should be returned
         * @return the color resource ID corresponding to the given object type (falls back to the primary text color for unknown types)
         */
        private int getColorForType(SearchResult.ObjectType type) {
            switch (type) {
                case STAR:
                    return R.color.star_highlight;
                case PLANET:
                    return R.color.planet_saturn;
                case CONSTELLATION:
                    return R.color.constellation_line;
                case MOON:
                    return R.color.planet_moon;
                case SUN:
                    return R.color.planet_sun;
                default:
                    return R.color.text_primary;
            }
        }
    }
}