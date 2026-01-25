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
        void onResultClick(@NonNull SearchResult result);
    }

    private final List<SearchResult> results = new ArrayList<>();
    private final OnResultClickListener listener;

    /**
     * Creates an adapter with the given click listener.
     *
     * @param listener Callback for result clicks
     */
    public SearchResultAdapter(@NonNull OnResultClickListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the results list.
     *
     * @param newResults New list of results
     */
    public void setResults(@NonNull List<SearchResult> newResults) {
        results.clear();
        results.addAll(newResults);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = results.get(position);
        holder.bind(result, listener);
    }

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

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivResultIcon);
            tvName = itemView.findViewById(R.id.tvResultName);
            tvType = itemView.findViewById(R.id.tvResultType);
            tvCoords = itemView.findViewById(R.id.tvResultCoords);
        }

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
         * Formats coordinates for display.
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
         * Gets the icon resource for an object type.
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
         * Gets the color resource for an object type.
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
