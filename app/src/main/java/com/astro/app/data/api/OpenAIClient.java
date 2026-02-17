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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
    private static final int READ_TIMEOUT_MS = 60_000;
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

    // Observer context for dynamic system prompt
    private double observerLatitude = Double.NaN;
    private double observerLongitude = Double.NaN;
    private long observerTimeMillis = 0;
    private float pointingRA = Float.NaN;
    private float pointingDec = Float.NaN;

    public interface StreamCallback {
        void onToken(String token);
        void onComplete(String fullResponse);
        void onError(String error);
    }

    public OpenAIClient(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Sets the observer context for inclusion in the system prompt.
     */
    public void setObserverContext(double latitude, double longitude, long timeMillis,
                                   float pointingRA, float pointingDec) {
        this.observerLatitude = latitude;
        this.observerLongitude = longitude;
        this.observerTimeMillis = timeMillis;
        this.pointingRA = pointingRA;
        this.pointingDec = pointingDec;
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
            JSONObject requestBody = buildRequestBody(messages, false);

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

    /**
     * Sends the conversation to OpenAI with SSE streaming enabled.
     * Tokens are delivered incrementally via the callback.
     * Falls back to non-streaming if streaming yields no content.
     */
    public void sendMessageStreaming(List<ChatMessage> messages, StreamCallback callback) throws IOException {
        HttpURLConnection connection = null;
        try {
            JSONObject requestBody = buildRequestBody(messages, true);

            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Accept", "text/event-stream");
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
                callback.onError(parseErrorMessage(responseCode, errorBody));
                return;
            }

            // Read SSE stream
            StringBuilder fullResponse = new StringBuilder();
            String rawBody = null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder rawCapture = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    rawCapture.append(line).append('\n');

                    // Handle "data: ..." or "data:..." (with or without space)
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) {
                        if ("[DONE]".equals(data)) break;
                        continue;
                    }
                    try {
                        JSONObject chunk = new JSONObject(data);
                        JSONArray choices = chunk.optJSONArray("choices");
                        if (choices != null && choices.length() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            JSONObject delta = choice.optJSONObject("delta");
                            if (delta != null) {
                                // Try standard content field first, then output_text (reasoning models)
                                String content = delta.optString("content", null);
                                if (content == null) {
                                    content = delta.optString("output_text", null);
                                }
                                if (content != null) {
                                    fullResponse.append(content);
                                    callback.onToken(content);
                                }
                            }
                        }
                    } catch (JSONException ignored) {
                        // Skip malformed SSE chunks
                    }
                }
                rawBody = rawCapture.toString();
            }

            String result = fullResponse.toString().trim();
            if (result.isEmpty()) {
                // Streaming yielded no tokens — try to parse as a non-streaming response
                // (model may not support streaming and returned a regular JSON body)
                if (rawBody != null) {
                    String fallback = tryParseNonStreamingResponse(rawBody.trim());
                    if (fallback != null && !fallback.isEmpty()) {
                        callback.onToken(fallback);
                        callback.onComplete(fallback);
                        return;
                    }
                }
                // Last resort: fall back to non-streaming API call
                connection.disconnect();
                connection = null;
                String fallbackResponse = sendMessage(messages);
                callback.onToken(fallbackResponse);
                callback.onComplete(fallbackResponse);
            } else {
                callback.onComplete(result);
            }

        } catch (JSONException e) {
            callback.onError("Failed to build request: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Attempts to parse a raw response body as a non-streaming chat completion.
     * Returns the content string, or null if parsing fails.
     */
    private String tryParseNonStreamingResponse(String rawBody) {
        try {
            // Strip any SSE framing if present
            String json = rawBody;
            if (json.startsWith("data:")) {
                json = json.substring(5).trim();
            }
            // Try to find a JSON object in the body
            int braceStart = json.indexOf('{');
            if (braceStart < 0) return null;
            json = json.substring(braceStart);

            JSONObject obj = new JSONObject(json);
            JSONArray choices = obj.optJSONArray("choices");
            if (choices == null || choices.length() == 0) return null;

            JSONObject first = choices.getJSONObject(0);

            // Non-streaming format: choices[0].message.content
            JSONObject message = first.optJSONObject("message");
            if (message != null) {
                String content = message.optString("content", null);
                if (content != null && !content.trim().isEmpty()) {
                    return content.trim();
                }
                // Reasoning models may use output field
                content = message.optString("output_text", null);
                if (content != null && !content.trim().isEmpty()) {
                    return content.trim();
                }
            }

            // Responses API format: choices[0].text or output[0].content
            String text = first.optString("text", null);
            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }

            // Try output array (newer API format)
            JSONArray output = obj.optJSONArray("output");
            if (output != null) {
                for (int i = 0; i < output.length(); i++) {
                    JSONObject item = output.getJSONObject(i);
                    if ("message".equals(item.optString("type"))) {
                        JSONArray contentArr = item.optJSONArray("content");
                        if (contentArr != null) {
                            for (int j = 0; j < contentArr.length(); j++) {
                                JSONObject cObj = contentArr.getJSONObject(j);
                                if ("output_text".equals(cObj.optString("type"))) {
                                    String t = cObj.optString("text", null);
                                    if (t != null && !t.trim().isEmpty()) return t.trim();
                                }
                            }
                        }
                    }
                }
            }
        } catch (JSONException ignored) {
        }
        return null;
    }

    private JSONObject buildRequestBody(List<ChatMessage> messages, boolean stream) throws JSONException {
        JSONArray messagesArray = new JSONArray();

        // Developer prompt (GPT-5 series uses "developer" role)
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "developer");
        systemMsg.put("content", buildSystemPrompt());
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
        // GPT-5 Nano is a reasoning model — reasoning tokens consume part of this budget.
        // 1024 was too low: reasoning used all tokens, leaving nothing for output.
        body.put("max_completion_tokens", 16384);
        // Reasoning models forbid sampling params (temperature, top_p).
        // Use reasoning_effort instead to control reasoning depth.
        body.put("reasoning_effort", "low");
        if (stream) {
            body.put("stream", true);
        }
        return body;
    }

    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);

        boolean hasContext = false;
        if (!Double.isNaN(observerLatitude) && !Double.isNaN(observerLongitude)) {
            hasContext = true;
        }
        if (observerTimeMillis > 0) {
            hasContext = true;
        }

        if (hasContext) {
            prompt.append("\n\nCurrent context:");
            if (!Double.isNaN(observerLatitude) && !Double.isNaN(observerLongitude)) {
                prompt.append(String.format(Locale.US,
                        "\n- Observer location: %.4f, %.4f", observerLatitude, observerLongitude));
            }
            if (observerTimeMillis > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                prompt.append("\n- Current time: ").append(sdf.format(new Date(observerTimeMillis)));
            }
            if (!Float.isNaN(pointingRA) && !Float.isNaN(pointingDec)) {
                prompt.append(String.format(Locale.US,
                        "\n- Currently viewing: RA=%.2f\u00b0, Dec=%.2f\u00b0", pointingRA, pointingDec));
            }
        }

        return prompt.toString();
    }

    private String parseResponse(String responseBody) throws JSONException {
        JSONObject json = new JSONObject(responseBody);

        // Standard Chat Completions format: choices[0].message.content
        JSONArray choices = json.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            if (message != null) {
                String content = message.optString("content", null);
                if (content != null && !content.trim().isEmpty()) {
                    return content.trim();
                }
            }
        }

        // Responses API format: output[].content[].text
        JSONArray output = json.optJSONArray("output");
        if (output != null) {
            for (int i = 0; i < output.length(); i++) {
                JSONObject item = output.getJSONObject(i);
                if ("message".equals(item.optString("type"))) {
                    JSONArray contentArr = item.optJSONArray("content");
                    if (contentArr != null) {
                        for (int j = 0; j < contentArr.length(); j++) {
                            JSONObject cObj = contentArr.getJSONObject(j);
                            if ("output_text".equals(cObj.optString("type"))) {
                                String text = cObj.optString("text", null);
                                if (text != null && !text.trim().isEmpty()) {
                                    return text.trim();
                                }
                            }
                        }
                    }
                }
            }
        }

        return "I'm sorry, I couldn't generate a response. Please try again.";
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
