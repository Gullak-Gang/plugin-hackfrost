package io.kestra.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringEscapeUtils;
import lombok.Getter;

public class SentimentAnalysisApiClient {
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String BASE_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Response DTO for sentiment analysis
    @Getter
    public static class SentimentResult {
        @JsonProperty("sentiment")
        private Sentiment sentiment;

        @JsonProperty("score")
        private double score;

        @JsonProperty("positive_word_count")
        private int positiveWordCount;

        @JsonProperty("negative_word_count")
        private int negativeWordCount;

        // Enum definition remains the same as in the original code
        public enum Sentiment {
            POSITIVE("POSITIVE"),
            NEGATIVE("NEGATIVE"),
            NEUTRAL("NEUTRAL");

            private final String value;

            Sentiment(String value) {
                this.value = value;
            }

            @JsonValue
            public String getValue() {
                return value;
            }

            @JsonCreator
            public static Sentiment fromValue(String value) {
                for (Sentiment sentiment : Sentiment.values()) {
                    if (sentiment.value.equalsIgnoreCase(value)) {
                        return sentiment;
                    }
                }
                throw new IllegalArgumentException("Unknown value: " + value);
            }
        }
    }

    // Error Response DTO
    @Getter
    public static class ErrorResponse {
        @JsonProperty("message")
        private String message;
    }

    // Comprehensive Chat Completion Response DTO
    @Getter
    private static class ChatCompletionResponse {
        @JsonProperty("id")
        private String id;

        @JsonProperty("object")
        private String object;

        @JsonProperty("created")
        private long created;

        @JsonProperty("model")
        private String model;

        @JsonProperty("choices")
        private List<Choice> choices;

        @JsonProperty("usage")
        private Usage usage;

        @Getter
        private static class Choice {
            @JsonProperty("index")
            private int index;

            @JsonProperty("message")
            private Message message;

            @JsonProperty("finish_reason")
            private String finishReason;
        }

        @Getter
        private static class Message {
            @JsonProperty("role")
            private String role;

            @JsonProperty("content")
            private String content;
        }

        @Getter
        private static class Usage {
            @JsonProperty("prompt_tokens")
            private int promptTokens;

            @JsonProperty("completion_tokens")
            private int completionTokens;

            @JsonProperty("total_tokens")
            private int totalTokens;
        }
    }

    // Request DTO remains the same as in the original code
    @Getter
    private static class ChatRequest {
        private final String model = "llama3-8b-8192";
        private List<Message> messages;

        private record Message(String role, String content) {
        }
    }

    public SentimentAnalysisApiClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();

        // Configure ObjectMapper
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Perform sentiment analysis on a list of posts
     *
     * @param posts List of posts to analyze
     * @return List of sentiment results
     * @throws IOException          If there's a network or request error
     * @throws InterruptedException If the request is interrupted
     */
    public List<SentimentResult> analyzeSentiment(List<String> posts) throws IOException, InterruptedException {
        // Prepare request payload
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.messages = List.of(
                new ChatRequest.Message("user",
                        "Please provide a sentiment analysis for the posts in the following format:\r\n\r\n[  \r\n  {\r\n    \"sentiment\": \"POSITIVE\",\r\n    \"score\": 0.7,\r\n    \"positive_word_count\": 4,\r\n    \"negative_word_count\": 1 \r\n  },\r\n  {\r\n    \"sentiment\": \"NEGATIVE\",\r\n    \"score\": 0.3,\r\n    \"positive_word_count\": 2,\r\n    \"negative_word_count\": 6\r\n  },\r\n  {\r\n    \"sentiment\": \"NEUTRAL\",\r\n    \"score\": 0.4,\r\n    \"positive_word_count\": 2,\r\n    \"negative_word_count\": 3\r\n  }\r\n]\r\n\r\nNOTE: Ensure there is an **equal distribution** of **POSITIVE**, **NEGATIVE** and **NEUTRAL** responses across the posts. Do not skew results to either positive or negative excessively. Just return the raw JSON response in the exact format as shown above without any additional explanation or plain text."),
                new ChatRequest.Message("user",
                        "Here is the data for analysis: " + objectMapper.writeValueAsString(posts)));

        // Convert request to JSON
        String requestBody = objectMapper.writeValueAsString(chatRequest);

        // Prepare HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Send request and get response
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Check response status
        if (response.statusCode() != 200) {
            // Parse error response
            ErrorResponse errorResponse = objectMapper.readValue(response.body(), ErrorResponse.class);
            throw new IOException("API Error: " + errorResponse.getMessage());
        }

        // Parse the chat completion response
        ChatCompletionResponse completionResponse = objectMapper.readValue(response.body(),
                ChatCompletionResponse.class);

        // Extract sentiment JSON from the first choice
        String sentimentJson = completionResponse.getChoices().get(0).getMessage().getContent();

        // Extract JSON array and parse into SentimentResult
        String extractedJson = extractJSONArray(sentimentJson);

        // Parse and return sentiment results
        return Arrays.asList(objectMapper.readValue(extractedJson, SentimentResult[].class));
    }

    private String extractJSONArray(String text) {
        if (text == null)
            return null;

        // Find the first JSON array in the text
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[.*?\\]", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        return StringEscapeUtils.unescapeJson(matcher.group(0));
    }
}