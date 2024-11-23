package com.example.kafka;

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

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    public OpenAiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String getEmpathyRating(String conversation) throws Exception {
        String prompt = "Evaluate the empathy level in the following conversation:\n\n" +
                        conversation +
                        "\n\nProvide a rating between 1 and 10, with 10 being highly empathetic.";

        Map<String, Object> requestBody = Map.of(
            "model", "gpt-4o-mini",
            "messages", List.of(
                Map.of("role", "system", "content", "You are an empathy evaluator."),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 50
        );

        // max tokens set at 50; can make prompt more detailed, etc. if needed

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

        return null;
    }
}
