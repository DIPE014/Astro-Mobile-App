package com.astro.app.ui.chat;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.astro.app.R;
import com.astro.app.core.control.SolarSystemBody;
import com.astro.app.core.control.space.Universe;
import com.astro.app.core.math.RaDec;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Bottom sheet fragment that hosts the AstroBot chat interface.
 *
 * <p>Displays a RecyclerView of chat messages, dynamically generated suggestion
 * chips based on currently visible sky objects, and a text input field. Reads
 * the OpenAI API key from EncryptedSharedPreferences.</p>
 */
public class ChatBottomSheetFragment extends BottomSheetDialogFragment
        implements ChatMessageAdapter.ChatActionListener {

    public static final String TAG = "ChatBottomSheetFragment";

    private static final String ENCRYPTED_PREFS_NAME = "astro_secure_prefs";
    private static final String KEY_OPENAI_API_KEY = "openai_api_key";

    // Bundle argument keys (set by SkyMapActivity)
    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";
    private static final String ARG_TIME_MILLIS = "timeMillis";
    private static final String ARG_POINTING_RA = "pointingRA";
    private static final String ARG_POINTING_DEC = "pointingDec";
    private static final String ARG_SELECTED_OBJECT = "selectedObjectName";

    private ChatViewModel viewModel;
    private ChatMessageAdapter adapter;

    private RecyclerView rvChatMessages;
    private TextInputEditText etChatInput;
    private ImageButton btnSend;
    private ImageButton btnCloseChat;
    private ImageButton btnClearChat;
    private View chipScrollView;
    private ChipGroup chipGroupSuggestions;

    private String apiKey;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        apiKey = readApiKey();

        // Pass observer context from SkyMapActivity to ViewModel
        Bundle args = getArguments();
        if (args != null) {
            double lat = args.getDouble(ARG_LATITUDE, Double.NaN);
            double lon = args.getDouble(ARG_LONGITUDE, Double.NaN);
            long timeMillis = args.getLong(ARG_TIME_MILLIS, 0);
            float pointingRA = args.getFloat(ARG_POINTING_RA, Float.NaN);
            float pointingDec = args.getFloat(ARG_POINTING_DEC, Float.NaN);
            String selectedObject = args.getString(ARG_SELECTED_OBJECT);
            viewModel.setContext(lat, lon, timeMillis, pointingRA, pointingDec);
            viewModel.setSelectedObject(selectedObject);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views
        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        etChatInput = view.findViewById(R.id.etChatInput);
        btnSend = view.findViewById(R.id.btnSend);
        btnCloseChat = view.findViewById(R.id.btnCloseChat);
        btnClearChat = view.findViewById(R.id.btnClearChat);
        chipScrollView = view.findViewById(R.id.chipScrollView);
        chipGroupSuggestions = view.findViewById(R.id.chipGroupSuggestions);

        // Setup RecyclerView
        adapter = new ChatMessageAdapter();
        adapter.setActionListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(adapter);

        // Send button
        btnSend.setOnClickListener(v -> sendCurrentInput());

        // Close button
        btnCloseChat.setOnClickListener(v -> dismiss());

        // Clear chat button
        btnClearChat.setOnClickListener(v -> viewModel.clearMessages());

        // Handle Enter key in input
        etChatInput.setOnEditorActionListener((v, actionId, event) -> {
            sendCurrentInput();
            return true;
        });

        // Build and display dynamic suggestion chips
        setupDynamicChips();

        // Observe messages — show/hide chips and clear button
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.setMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                rvChatMessages.scrollToPosition(messages.size() - 1);
                chipScrollView.setVisibility(View.GONE);
                btnClearChat.setVisibility(View.VISIBLE);
            } else {
                chipScrollView.setVisibility(View.VISIBLE);
                btnClearChat.setVisibility(View.GONE);
            }
        });

        // Observe loading state — disable send button while waiting
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            btnSend.setEnabled(!isLoading);
            btnSend.setAlpha(isLoading ? 0.5f : 1.0f);
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            View bottomSheet = bottomSheetDialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);

                // Set peek height to 75% of screen
                DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();
                int peekHeight = (int) (displayMetrics.heightPixels * 0.75);
                behavior.setPeekHeight(peekHeight);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(false);

                // Set max height to full screen
                ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.setLayoutParams(layoutParams);
            }
        });

        return dialog;
    }

    // ---- ChatActionListener (follow-ups and retry from adapter) ----

    @Override
    public void onFollowupClicked(String text) {
        sendMessage(text);
    }

    @Override
    public void onRetryClicked(String retryQuery) {
        sendMessage(retryQuery);
    }

    // ---- Private helpers ----

    private void sendCurrentInput() {
        if (etChatInput == null) return;
        String text = etChatInput.getText() != null ? etChatInput.getText().toString() : "";
        if (text.trim().isEmpty()) return;
        sendMessage(text.trim());
        etChatInput.setText("");
    }

    private void sendMessage(String text) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.chat_no_api_key, Toast.LENGTH_LONG).show();
            return;
        }
        viewModel.sendMessage(text, apiKey);
    }

    /**
     * Builds dynamic suggestion chips based on which planets are currently above the
     * horizon, plus the selected sky object (if any) and static astronomy fallbacks.
     *
     * <p>Chip priority:
     * <ol>
     *   <li>Selected object (injected by SkyMapActivity when user tapped an object)</li>
     *   <li>Planets/Moon visible above 10° altitude right now</li>
     *   <li>Static astronomy fallback questions</li>
     * </ol>
     * </p>
     */
    private void setupDynamicChips() {
        List<String> suggestions = buildSuggestions();

        chipGroupSuggestions.removeAllViews();
        for (String text : suggestions) {
            Chip chip = new Chip(requireContext());
            chip.setText(text);
            chip.setCheckable(false);
            // Use the app's existing chip style attributes
            chip.setChipBackgroundColorResource(R.color.surface_elevated);
            chip.setTextColor(requireContext().getColor(R.color.text_primary));
            chip.setChipStrokeColorResource(R.color.outline_variant);
            chip.setChipStrokeWidth(1f);
            chip.setOnClickListener(v -> sendMessage(text));
            chipGroupSuggestions.addView(chip);
        }
    }

    /**
     * Returns up to 6 chip labels: selected object first, then visible planets, then
     * static fallbacks to fill remaining slots.
     */
    private List<String> buildSuggestions() {
        List<String> chips = new ArrayList<>();

        Bundle args = getArguments();
        double lat = (args != null) ? args.getDouble(ARG_LATITUDE, Double.NaN) : Double.NaN;
        double lon = (args != null) ? args.getDouble(ARG_LONGITUDE, Double.NaN) : Double.NaN;
        long timeMillis = (args != null) ? args.getLong(ARG_TIME_MILLIS, 0) : 0;
        String selectedObject = (args != null) ? args.getString(ARG_SELECTED_OBJECT) : null;

        // 1. Selected object chip — first, most contextually relevant
        if (selectedObject != null && !selectedObject.trim().isEmpty()) {
            chips.add("Tell me about " + selectedObject);
        }

        // 2. Visible planets — computed from live ephemeris
        if (!Double.isNaN(lat) && !Double.isNaN(lon) && timeMillis > 0) {
            appendVisiblePlanetChips(chips, lat, lon, timeMillis);
        }

        // 3. Static fallbacks to fill remaining slots
        String[] fallbacks = {
                "What's visible tonight?",
                "Tips for astrophotography",
                "What is the Bortle scale?",
                "How does plate solving work?",
                "How do I read RA and Dec?",
                "What's the best telescope for beginners?"
        };
        for (String fallback : fallbacks) {
            if (chips.size() >= 6) break;
            chips.add(fallback);
        }

        return chips;
    }

    /**
     * Adds chips for solar system bodies currently above 10° altitude.
     * Uses the app's existing {@link Universe} ephemeris — same source as PlanetsLayer.
     */
    private void appendVisiblePlanetChips(List<String> chips, double lat, double lon, long timeMillis) {
        // Bodies to check (skip Earth and Pluto for observer relevance)
        SolarSystemBody[] bodies = {
                SolarSystemBody.Jupiter,
                SolarSystemBody.Saturn,
                SolarSystemBody.Mars,
                SolarSystemBody.Venus,
                SolarSystemBody.Mercury,
                SolarSystemBody.Moon,
                SolarSystemBody.Uranus,
                SolarSystemBody.Neptune
        };
        String[] names = {"Jupiter", "Saturn", "Mars", "Venus", "Mercury", "Moon", "Uranus", "Neptune"};

        try {
            Universe universe = new Universe();
            Date date = new Date(timeMillis);

            for (int i = 0; i < bodies.length; i++) {
                if (chips.size() >= 5) break; // Leave room for at least one static fallback
                try {
                    RaDec raDec = universe.getRaDec(bodies[i], date);
                    double altDeg = computeAltitudeDeg(raDec.getRa(), raDec.getDec(), lat, lon, timeMillis);
                    if (altDeg > 10.0) {
                        String name = names[i];
                        if ("Moon".equals(name)) {
                            chips.add("How will the Moon affect tonight\u2019s observing?");
                        } else {
                            chips.add("Tell me about " + name + " tonight");
                        }
                    }
                } catch (Exception ignored) {
                    // Skip individual body if calculation fails
                }
            }
        } catch (Exception ignored) {
            // If Universe init fails, just use static chips
        }
    }

    /**
     * Computes the altitude in degrees of a celestial object from the observer's location.
     *
     * @param raDeg  Right ascension in degrees
     * @param decDeg Declination in degrees
     * @param lat    Observer latitude in degrees
     * @param lon    Observer longitude in degrees (positive = East)
     * @param timeMs Unix timestamp in milliseconds
     * @return Altitude above horizon in degrees
     */
    private static double computeAltitudeDeg(float raDeg, float decDeg,
                                              double lat, double lon, long timeMs) {
        // Julian date from Unix epoch
        double jd = timeMs / 86400000.0 + 2440587.5;

        // Greenwich Mean Sidereal Time in degrees (simplified formula)
        double jd0 = Math.floor(jd - 0.5) + 0.5; // JD at prior midnight UT
        double T = (jd0 - 2451545.0) / 36525.0;
        double theta0 = 100.4606184 + 36000.770053354 * T + 0.000387933 * T * T;
        double fracDay = (jd - jd0) * 360.985647;
        double lst = ((theta0 + fracDay + lon) % 360.0 + 360.0) % 360.0;

        // Hour angle
        double ha = ((lst - raDeg) % 360.0 + 360.0) % 360.0;

        // Altitude from spherical trig
        double latRad = Math.toRadians(lat);
        double decRad = Math.toRadians(decDeg);
        double haRad = Math.toRadians(ha);
        double sinAlt = Math.sin(decRad) * Math.sin(latRad)
                + Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad);
        return Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, sinAlt))));
    }

    /**
     * Reads the OpenAI API key from EncryptedSharedPreferences.
     * Returns null if no key is stored or if encryption setup fails.
     */
    @Nullable
    private String readApiKey() {
        Context context = getContext();
        if (context == null) return null;

        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            return securePrefs.getString(KEY_OPENAI_API_KEY, null);
        } catch (GeneralSecurityException | IOException e) {
            // Do not fall back to unencrypted storage — return null to protect the key
            return null;
        }
    }
}
