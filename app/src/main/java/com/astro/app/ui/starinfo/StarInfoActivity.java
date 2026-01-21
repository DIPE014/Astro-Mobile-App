package com.astro.app.ui.starinfo;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.astro.app.R;

/**
 * FRONTEND - Person A
 *
 * Shows detailed information about a selected star.
 */
public class StarInfoActivity extends AppCompatActivity {

    public static final String EXTRA_STAR_NAME = "star_name";
    public static final String EXTRA_STAR_RA = "star_ra";
    public static final String EXTRA_STAR_DEC = "star_dec";
    public static final String EXTRA_STAR_MAGNITUDE = "star_magnitude";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_star_info);

        // Get star data from intent
        String name = getIntent().getStringExtra(EXTRA_STAR_NAME);
        float ra = getIntent().getFloatExtra(EXTRA_STAR_RA, 0);
        float dec = getIntent().getFloatExtra(EXTRA_STAR_DEC, 0);
        float magnitude = getIntent().getFloatExtra(EXTRA_STAR_MAGNITUDE, 0);

        // TODO: Bind data to views
        // TextView tvName = findViewById(R.id.tvStarName);
        // tvName.setText(name);
    }
}
