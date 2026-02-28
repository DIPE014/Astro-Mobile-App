package com.astro.app.ui.chat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.astro.app.R;
import com.astro.app.data.model.ChatMessage;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for chat messages. Uses two view types to render
 * user messages (right-aligned purple bubble) and bot messages (left-aligned
 * surface-colored bubble with an "AstroBot" label).
 *
 * <p>Bot messages with follow-up suggestions show tappable chips below the bubble.
 * Error messages show a retry button.</p>
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_BOT = 1;

    /** Callbacks from in-message interactions to the host fragment. */
    public interface ChatActionListener {
        /** Called when the user taps a follow-up suggestion chip. */
        void onFollowupClicked(String text);
        /** Called when the user taps the Retry button on an error message. */
        void onRetryClicked(String retryQuery);
    }

    private final List<ChatMessage> messages = new ArrayList<>();
    @Nullable
    private ChatActionListener actionListener;

    public void setActionListener(@Nullable ChatActionListener listener) {
        this.actionListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_message_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_message_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(message);
        } else if (holder instanceof BotViewHolder) {
            ((BotViewHolder) holder).bind(message, actionListener);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /**
     * Replaces the entire message list and refreshes the view.
     */
    public void setMessages(List<ChatMessage> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    // ---- ViewHolders ----

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUserMessage;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
        }

        void bind(ChatMessage message) {
            tvUserMessage.setText(message.getContent());
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvBotMessage;
        private final TextView tvRetry;
        private final ChipGroup chipGroupFollowups;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable animationRunnable;
        private int dotCount = 0;

        BotViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBotMessage = itemView.findViewById(R.id.tvBotMessage);
            tvRetry = itemView.findViewById(R.id.tvRetry);
            chipGroupFollowups = itemView.findViewById(R.id.chipGroupFollowups);
        }

        void bind(ChatMessage message, @Nullable ChatActionListener listener) {
            stopAnimation();

            if (message.isThinking()) {
                tvRetry.setVisibility(View.GONE);
                chipGroupFollowups.setVisibility(View.GONE);
                startThinkingAnimation();
                return;
            }

            tvBotMessage.setText(message.getContent());

            // Retry button for error messages
            if (message.isError() && message.getRetryQuery() != null) {
                tvRetry.setVisibility(View.VISIBLE);
                tvRetry.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRetryClicked(message.getRetryQuery());
                    }
                });
            } else {
                tvRetry.setVisibility(View.GONE);
            }

            // Follow-up suggestion chips
            List<String> followups = message.getFollowups();
            if (!message.isError() && followups != null && !followups.isEmpty()) {
                chipGroupFollowups.setVisibility(View.VISIBLE);
                chipGroupFollowups.removeAllViews();
                Context context = itemView.getContext();
                for (String suggestion : followups) {
                    Chip chip = new Chip(context);
                    chip.setText(suggestion);
                    chip.setCheckable(false);
                    chip.setChipBackgroundColorResource(R.color.surface_elevated);
                    chip.setTextColor(context.getColor(R.color.text_primary));
                    chip.setTextSize(12f);
                    chip.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onFollowupClicked(suggestion);
                        }
                    });
                    chipGroupFollowups.addView(chip);
                }
            } else {
                chipGroupFollowups.setVisibility(View.GONE);
                chipGroupFollowups.removeAllViews();
            }
        }

        private void startThinkingAnimation() {
            dotCount = 0;
            animationRunnable = new Runnable() {
                @Override
                public void run() {
                    dotCount = (dotCount % 3) + 1;
                    StringBuilder dots = new StringBuilder("Thinking");
                    for (int i = 0; i < dotCount; i++) dots.append('.');
                    tvBotMessage.setText(dots.toString());
                    handler.postDelayed(this, 500);
                }
            };
            tvBotMessage.setText("Thinking.");
            handler.postDelayed(animationRunnable, 500);
        }

        private void stopAnimation() {
            if (animationRunnable != null) {
                handler.removeCallbacks(animationRunnable);
                animationRunnable = null;
            }
        }
    }
}
