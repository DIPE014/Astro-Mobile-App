package com.astro.app.ui.chat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
     * Sends a user message and requests a response from OpenAI.
     *
     * <p>If the API key is null or empty, a friendly error message is added
     * instead of making a network call.</p>
     *
     * @param userMessage the text the user typed
     * @param apiKey      the OpenAI API key (may be null)
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
                    "No API key configured. Please add your OpenAI API key in Settings to use AstroBot."
            );
            messages.add(errorMsg);
            messagesLiveData.postValue(new ArrayList<>(messages));
            return;
        }

        // Call OpenAI on background thread
        loadingLiveData.postValue(true);
        executor.execute(() -> {
            try {
                OpenAIClient client = new OpenAIClient(apiKey.trim());

                // Send only the last MAX_CONTEXT_MESSAGES for context
                List<ChatMessage> contextMessages;
                if (messages.size() > MAX_CONTEXT_MESSAGES) {
                    contextMessages = new ArrayList<>(
                            messages.subList(messages.size() - MAX_CONTEXT_MESSAGES, messages.size()));
                } else {
                    contextMessages = new ArrayList<>(messages);
                }

                String response = client.sendMessage(contextMessages);
                ChatMessage botMsg = new ChatMessage(ChatMessage.ROLE_ASSISTANT, response);
                messages.add(botMsg);
                messagesLiveData.postValue(new ArrayList<>(messages));

            } catch (IOException e) {
                String errorText = "Sorry, I couldn't connect to the server. Please check your internet connection and try again.";
                ChatMessage errorMsg = new ChatMessage(ChatMessage.ROLE_ASSISTANT, errorText);
                messages.add(errorMsg);
                messagesLiveData.postValue(new ArrayList<>(messages));
            } finally {
                loadingLiveData.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }
}
