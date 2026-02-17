package com.astro.app.data.api;

import com.astro.app.data.model.ChatMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * HTTP client for the OpenAI Chat Completions API.
 *
 * <p>Sends conversation history to the GPT-5 Nano model and returns the
 * assistant's response. Includes an astronomy-focused system prompt so the
 * model behaves as "AstroBot".</p>
 */
public class OpenAIClient {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-5-nano-2025-08-07";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int MAX_CONTEXT_MESSAGES = 20;

    private static final String SYSTEM_PROMPT =
            "You are AstroBot, an AI assistant for the Astro astronomy app. You help users with:\n" +
            "- Identifying stars, constellations, and planets visible tonight\n" +
            "- Explaining astronomical concepts (magnitude, RA/Dec, celestial coordinates)\n" +
            "- Tips for astrophotography and observation\n" +
            "- Understanding plate solving and star detection results\n" +
            "- Interpreting sky brightness/Bortle scale readings\n" +
            "- General astronomy questions\n" +
            "Keep answers concise and beginner-friendly. Use the user's location and time context when relevant.";

    private final String apiKey;

    public OpenAIClient(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Sends the conversation to OpenAI and returns the assistant's reply.
     *
     * @param messages the conversation history (last {@value MAX_CONTEXT_MESSAGES} messages are sent)
     * @return the assistant's response text
     * @throws IOException on network or API errors
     */
    public String sendMessage(List<ChatMessage> messages) throws IOException {
        HttpURLConnection connection = null;
        try {
            JSONObject requestBody = buildRequestBody(messages);

            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setDoOutput(true);

            byte[] body = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorBody = readStream(connection.getErrorStream() != null
                        ? new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))
                        : null);
                return parseErrorMessage(responseCode, errorBody);
            }

            String responseBody = readStream(
                    new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)));
            return parseResponse(responseBody);

        } catch (JSONException e) {
            throw new IOException("Failed to build or parse JSON", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private JSONObject buildRequestBody(List<ChatMessage> messages) throws JSONException {
        JSONArray messagesArray = new JSONArray();

        // System prompt
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        messagesArray.put(systemMsg);

        // Conversation history (last MAX_CONTEXT_MESSAGES)
        int startIndex = Math.max(0, messages.size() - MAX_CONTEXT_MESSAGES);
        for (int i = startIndex; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            JSONObject msgObj = new JSONObject();
            msgObj.put("role", msg.getRole());
            msgObj.put("content", msg.getContent());
            messagesArray.put(msgObj);
        }

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("messages", messagesArray);
        body.put("max_completion_tokens", 4096);
        body.put("reasoning_effort", "low");
        return body;
    }

    private String parseResponse(String responseBody) throws JSONException {
        JSONObject json = new JSONObject(responseBody);
        JSONArray choices = json.getJSONArray("choices");
        if (choices.length() == 0) {
            return "No response received from AstroBot.";
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String content = message.optString("content", null);
        if (content == null || content.trim().isEmpty()) {
            return "AstroBot is thinking but couldn't produce a response. Please try again.";
        }
        return content.trim();
    }

    private String parseErrorMessage(int responseCode, String errorBody) {
        String detail = "";
        if (errorBody != null && !errorBody.isEmpty()) {
            try {
                JSONObject errorJson = new JSONObject(errorBody);
                if (errorJson.has("error")) {
                    JSONObject errorObj = errorJson.getJSONObject("error");
                    detail = errorObj.optString("message", "");
                }
            } catch (JSONException ignored) {
                detail = errorBody;
            }
        }

        switch (responseCode) {
            case 401:
                return "Invalid API key. Please check your OpenAI API key in Settings.";
            case 429:
                return "Rate limit exceeded. Please wait a moment and try again.";
            case 500:
            case 502:
            case 503:
                return "OpenAI service is temporarily unavailable. Please try again later.";
            default:
                if (!detail.isEmpty()) {
                    return "API error (" + responseCode + "): " + detail;
                }
                return "Unexpected error (HTTP " + responseCode + "). Please try again.";
        }
    }

    private String readStream(BufferedReader reader) throws IOException {
        if (reader == null) return "";
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }
}
