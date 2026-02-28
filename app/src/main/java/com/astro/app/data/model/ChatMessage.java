package com.astro.app.data.model;

import java.util.Collections;
import java.util.List;

public class ChatMessage {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private String role;
    private String content;
    private long timestamp;
    private boolean thinking;
    private boolean error;
    private String retryQuery;
    private List<String> followups;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.thinking = false;
        this.error = false;
        this.followups = Collections.emptyList();
    }

    private ChatMessage(boolean thinking) {
        this.role = ROLE_ASSISTANT;
        this.content = "";
        this.timestamp = System.currentTimeMillis();
        this.thinking = true;
        this.error = false;
        this.followups = Collections.emptyList();
    }

    /** Creates a thinking/typing indicator message. */
    public static ChatMessage thinking() {
        return new ChatMessage(true);
    }

    // Getters
    public String getRole() { return role; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public boolean isUser() { return ROLE_USER.equals(role); }
    public boolean isThinking() { return thinking; }
    public boolean isError() { return error; }
    public String getRetryQuery() { return retryQuery; }
    public List<String> getFollowups() { return followups; }

    /** Updates content on a successful response, with optional follow-up suggestions. */
    public void setResponse(String content, List<String> followups) {
        this.content = content;
        this.thinking = false;
        this.error = false;
        this.followups = followups != null ? followups : Collections.emptyList();
    }

    /** Updates content for a network/API error, storing the original query for retry. */
    public void setError(String content, String retryQuery) {
        this.content = content;
        this.thinking = false;
        this.error = true;
        this.retryQuery = retryQuery;
        this.followups = Collections.emptyList();
    }

    /** Updates the content (used for streaming token updates). */
    public void setContent(String content) {
        this.content = content;
        this.thinking = false;
    }
}
