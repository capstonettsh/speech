package com.communication.communication_backend.kafka;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OpenAiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key:YOUR_OPENAI_API_KEY}")
    private String apiKey;

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    public OpenAiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String getEmpathyRating(String conversation) throws Exception {
        String prompt = "Evaluate the empathy level in the following conversation:\n\n" +
                        conversation +
                        "\n\nProvide a rating between 1 and 10, with 10 being highly empathetic. You are to provide exactly one rating and exactly one line that justifies your rating as concisely as possible.";

        Map<String, Object> requestBody = Map.of(
            "model", "gpt-4o-mini",
            "messages", List.of(
                Map.of("role", "system", "content", "You are an empathy evaluator."),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 50
        );

        String requestBodyJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        } else {
            throw new RuntimeException("Error: " + response.statusCode() + " - " + response.body());
        }

        return "No rating";
    }

    // for speech-only final review
    public String getSpeechSummary(
            String newGptResponse,
            String trailingSummary,
            String top3Mistakes
    ) throws Exception {

        // Build the prompt
        String prompt =
            "You are analyzing a conversation focusing on the user's speech performance.\n\n" +
            "Trailing summary of the conversation so far:\n" +
            trailingSummary + "\n\n" +
            "New exchange to consider:\n" +
            newGptResponse + "\n\n" +
            "Here are the current top 3 mistakes from the user so far:\n" +
            top3Mistakes + "\n\n" +
            "Using all the info:\n" +
            "1) Summarize how the user's empathy and speech is going so far.\n" +
            "2) Provide EXACTLY 3 sentences.\n" +
            "3) In those 3 sentences, highlight how to improve communications if not already in the summary.\n" +
            "4) If the new mistakes are not worse than existing, do not remove the old ones from the top3.\n" +
            "Keep it concise." +
            "Then, if there are any new speech mistakes, each line MUST begin exactly with 'Mistake:' " +
            "followed by a single-line description as well as the exchange where the mistake occurred, returned in the format such as:" +
            "first line -> Mistake: {one-liner describing the mistake, especially why it is a}\n" +
            "second line -> Exchange: {user: ...} {assistant: ...} \n" +
            "If no new mistakes, do not output 'Mistake:' or 'Exchange' lines.\n" +
            "End your response with a blank line.\n";

        Map<String, Object> requestBody = Map.of(
            "model", "gpt-4o-mini",
            "messages", List.of(
                Map.of("role", "system", "content", "You are a speech and empathy summary evaluator."),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 400
        );

        String requestBodyJson = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (!choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        } else {
            throw new RuntimeException("Error: " + response.statusCode() + " - " + response.body());
        }

        return "No speech summary generated.";
    }
}
