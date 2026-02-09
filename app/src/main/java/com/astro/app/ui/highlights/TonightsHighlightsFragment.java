package com.astro.app.ui.highlights;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.astro.app.R;
import com.astro.app.core.highlights.TonightsHighlights;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

/**
 * Bottom sheet fragment displaying tonight's visible celestial highlights.
 */
public class TonightsHighlightsFragment extends BottomSheetDialogFragment {

    public interface OnHighlightSelectedListener {
        void onHighlightSelected(TonightsHighlights.Highlight highlight);
    }

    private OnHighlightSelectedListener listener;
    private List<TonightsHighlights.Highlight> highlights;

    public static TonightsHighlightsFragment newInstance() {
        return new TonightsHighlightsFragment();
    }

    public void setHighlights(List<TonightsHighlights.Highlight> highlights) {
        this.highlights = highlights;
    }

    public void setOnHighlightSelectedListener(OnHighlightSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tonights_highlights, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CircularProgressIndicator progress = view.findViewById(R.id.progressIndicator);
        TextView tvEmpty = view.findViewById(R.id.tvEmpty);
        LinearLayout contentContainer = view.findViewById(R.id.contentContainer);

        if (highlights != null) {
            progress.setVisibility(View.GONE);
            if (highlights.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                contentContainer.setVisibility(View.VISIBLE);
                populateHighlights(contentContainer, highlights);
            }
        }
    }

    private void populateHighlights(LinearLayout container, List<TonightsHighlights.Highlight> items) {
        Context ctx = requireContext();
        String lastType = null;

        for (TonightsHighlights.Highlight item : items) {
            // Add section header when type changes
            if (!item.type.equals(lastType)) {
                lastType = item.type;
                TextView header = new TextView(ctx);
                header.setText(getSectionTitle(item.type));
                header.setTextSize(14f);
                header.setTypeface(null, Typeface.BOLD);
                header.setTextColor(getSectionColor(item.type));
                int pad = dpToPx(ctx, 16);
                int topPad = container.getChildCount() > 0 ? dpToPx(ctx, 12) : dpToPx(ctx, 4);
                header.setPadding(pad, topPad, pad, dpToPx(ctx, 4));
                container.addView(header);
            }

            // Add item row
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            int hPad = dpToPx(ctx, 16);
            int vPad = dpToPx(ctx, 10);
            row.setPadding(hPad, vPad, hPad, vPad);
            row.setBackground(ctx.getDrawable(android.R.drawable.list_selector_background));
            row.setClickable(true);

            // Name
            TextView tvName = new TextView(ctx);
            tvName.setText(item.name);
            tvName.setTextSize(16f);
            tvName.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(nameParams);
            row.addView(tvName);

            // Direction
            TextView tvDir = new TextView(ctx);
            tvDir.setText(item.direction);
            tvDir.setTextSize(13f);
            tvDir.setTextColor(Color.argb(180, 200, 200, 200));
            int dirMargin = dpToPx(ctx, 8);
            LinearLayout.LayoutParams dirParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dirParams.setMarginStart(dirMargin);
            tvDir.setLayoutParams(dirParams);
            row.addView(tvDir);

            // Extra info
            TextView tvExtra = new TextView(ctx);
            tvExtra.setText(item.extra);
            tvExtra.setTextSize(12f);
            tvExtra.setTextColor(Color.argb(150, 180, 180, 180));
            LinearLayout.LayoutParams extraParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            extraParams.setMarginStart(dirMargin);
            tvExtra.setLayoutParams(extraParams);
            row.addView(tvExtra);

            row.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHighlightSelected(item);
                }
                dismiss();
            });

            container.addView(row);
        }
    }

    private String getSectionTitle(String type) {
        switch (type) {
            case "planet": return "Planets";
            case "star": return "Bright Stars";
            case "constellation": return "Constellations";
            case "dso": return "Deep Sky";
            default: return "Other";
        }
    }

    private int getSectionColor(String type) {
        switch (type) {
            case "planet": return Color.rgb(255, 183, 77);
            case "star": return Color.rgb(255, 255, 200);
            case "constellation": return Color.rgb(150, 180, 255);
            case "dso": return Color.rgb(100, 200, 255);
            default: return Color.WHITE;
        }
    }

    private static int dpToPx(Context ctx, int dp) {
        return (int) (dp * ctx.getResources().getDisplayMetrics().density);
    }
}
