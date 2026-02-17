package com.astro.app.data.model;

public class ChatMessage {
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private String role;
    private String content;
    private long timestamp;
    private boolean thinking;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.thinking = false;
    }

    private ChatMessage(boolean thinking) {
        this.role = ROLE_ASSISTANT;
        this.content = "";
        this.timestamp = System.currentTimeMillis();
        this.thinking = true;
    }

    /** Creates a thinking/typing indicator message. */
    public static ChatMessage thinking() {
        return new ChatMessage(true);
    }

    // getters
    public String getRole() { return role; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public boolean isUser() { return ROLE_USER.equals(role); }
    public boolean isThinking() { return thinking; }

    /** Updates the content (used for streaming token updates). */
    public void setContent(String content) {
        this.content = content;
        this.thinking = false;
    }
}
