package com.astro.app.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.astro.app.R;
import com.astro.app.data.api.OpenAIClient;
import com.astro.app.data.model.ChatMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel for the chat feature. Maintains the conversation history,
 * dispatches API calls on a background thread, and exposes LiveData
 * for the UI to observe.
 */
public class ChatViewModel extends AndroidViewModel {

    private static final int MAX_CONTEXT_MESSAGES = 20;

    private final MutableLiveData<List<ChatMessage>> messagesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    private final List<ChatMessage> messages = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Observer context fields
    private double contextLatitude = Double.NaN;
    private double contextLongitude = Double.NaN;
    private long contextTimeMillis = 0;
    private float contextPointingRA = Float.NaN;
    private float contextPointingDec = Float.NaN;
    private String contextSelectedObject = null;

    public ChatViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messagesLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }

    /**
     * Sets the observer context (location, time, pointing direction) for
     * inclusion in the system prompt.
     */
    public void setContext(double latitude, double longitude, long timeMillis,
                           float pointingRA, float pointingDec) {
        this.contextLatitude = latitude;
        this.contextLongitude = longitude;
        this.contextTimeMillis = timeMillis;
        this.contextPointingRA = pointingRA;
        this.contextPointingDec = pointingDec;
    }

    /**
     * Sets the currently selected sky object name to inject into the system prompt.
     */
    public void setSelectedObject(String name) {
        this.contextSelectedObject = name;
    }

    /**
     * Sends a user message and requests a response from OpenAI.
     *
     * <p>If the API key is null or empty, a friendly error message is added
     * instead of making a network call.</p>
     */
    public void sendMessage(String userMessage, String apiKey) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return;
        }

        // Add user message
        ChatMessage userMsg = new ChatMessage(ChatMessage.ROLE_USER, userMessage.trim());
        messages.add(userMsg);
        messagesLiveData.postValue(new ArrayList<>(messages));

        // Validate API key
        if (apiKey == null || apiKey.trim().isEmpty()) {
            ChatMessage errorMsg = new ChatMessage(
                    ChatMessage.ROLE_ASSISTANT,
                    getApplication().getString(R.string.chat_no_api_key)
            );
            messages.add(errorMsg);
            messagesLiveData.postValue(new ArrayList<>(messages));
            return;
        }

        // Add thinking indicator
        ChatMessage thinkingMsg = ChatMessage.thinking();
        messages.add(thinkingMsg);
        messagesLiveData.postValue(new ArrayList<>(messages));

        loadingLiveData.postValue(true);

        final String query = userMessage.trim();
        // Snapshot the conversation context on the calling thread to avoid a data race
        // between the main thread (add/clear) and the executor thread (iterate).
        final List<ChatMessage> snapshot = new ArrayList<>(messages);
        executor.execute(() -> {
            try {
                OpenAIClient client = new OpenAIClient(apiKey.trim());
                client.setObserverContext(contextLatitude, contextLongitude,
                        contextTimeMillis, contextPointingRA, contextPointingDec);
                client.setSelectedObject(contextSelectedObject);

                // Build context messages from snapshot (exclude thinking messages)
                List<ChatMessage> contextMessages = new ArrayList<>();
                for (ChatMessage msg : snapshot) {
                    if (!msg.isThinking()) {
                        contextMessages.add(msg);
                    }
                }
                if (contextMessages.size() > MAX_CONTEXT_MESSAGES) {
                    contextMessages = new ArrayList<>(
                            contextMessages.subList(contextMessages.size() - MAX_CONTEXT_MESSAGES,
                                    contextMessages.size()));
                }

                // Use non-streaming request (GPT-5 Nano streaming is broken —
                // SSE stream opens but emits no tokens)
                OpenAIClient.BotResponse response = client.sendMessage(contextMessages);
                int idx = messages.indexOf(thinkingMsg);
                if (idx >= 0) {
                    messages.get(idx).setResponse(response.answer, response.followups);
                    messagesLiveData.postValue(new ArrayList<>(messages));
                }

            } catch (IOException e) {
                // Replace thinking message with retryable error
                int idx = messages.indexOf(thinkingMsg);
                if (idx >= 0) {
                    messages.get(idx).setError(
                            getApplication().getString(R.string.chat_error),
                            query);
                    messagesLiveData.postValue(new ArrayList<>(messages));
                }
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    /** Clears all messages from the conversation. */
    public void clearMessages() {
        messages.clear();
        messagesLiveData.postValue(new ArrayList<>(messages));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }
}
