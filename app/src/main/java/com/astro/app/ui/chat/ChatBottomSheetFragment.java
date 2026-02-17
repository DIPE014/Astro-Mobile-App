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
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Bottom sheet fragment that hosts the AstroBot chat interface.
 *
 * <p>Displays a RecyclerView of chat messages, suggestion chips for
 * common astronomy questions, and a text input field. Reads the
 * OpenAI API key from EncryptedSharedPreferences.</p>
 */
public class ChatBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "ChatBottomSheetFragment";

    private static final String ENCRYPTED_PREFS_NAME = "astro_secure_prefs";
    private static final String KEY_OPENAI_API_KEY = "openai_api_key";

    private ChatViewModel viewModel;
    private ChatMessageAdapter adapter;

    private RecyclerView rvChatMessages;
    private TextInputEditText etChatInput;
    private ImageButton btnSend;
    private ImageButton btnCloseChat;
    private View chipScrollView;

    private String apiKey;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        apiKey = readApiKey();
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
        chipScrollView = view.findViewById(R.id.chipScrollView);

        // Setup RecyclerView
        adapter = new ChatMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
        rvChatMessages.setAdapter(adapter);

        // Send button
        btnSend.setOnClickListener(v -> sendCurrentInput());

        // Close button
        btnCloseChat.setOnClickListener(v -> dismiss());

        // Suggestion chips
        setupChipListener(view, R.id.chipSuggestion1);
        setupChipListener(view, R.id.chipSuggestion2);
        setupChipListener(view, R.id.chipSuggestion3);
        setupChipListener(view, R.id.chipSuggestion4);
        setupChipListener(view, R.id.chipSuggestion5);
        setupChipListener(view, R.id.chipSuggestion6);

        // Handle Enter key in input
        etChatInput.setOnEditorActionListener((v, actionId, event) -> {
            sendCurrentInput();
            return true;
        });

        // Observe messages
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.setMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                rvChatMessages.scrollToPosition(messages.size() - 1);
                chipScrollView.setVisibility(View.GONE);
            } else {
                chipScrollView.setVisibility(View.VISIBLE);
            }
        });

        // Observe loading state
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

    private void sendCurrentInput() {
        if (etChatInput == null) return;
        String text = etChatInput.getText() != null ? etChatInput.getText().toString() : "";
        if (text.trim().isEmpty()) return;

        if (apiKey == null || apiKey.trim().isEmpty()) {
            Toast.makeText(requireContext(),
                    R.string.chat_no_api_key,
                    Toast.LENGTH_LONG).show();
            return;
        }

        viewModel.sendMessage(text, apiKey);
        etChatInput.setText("");
    }

    private void setupChipListener(@NonNull View root, int chipId) {
        Chip chip = root.findViewById(chipId);
        if (chip != null) {
            chip.setOnClickListener(v -> {
                String chipText = chip.getText().toString();
                viewModel.sendMessage(chipText, apiKey);
            });
        }
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
