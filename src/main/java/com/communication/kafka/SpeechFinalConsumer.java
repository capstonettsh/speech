package com.communication.communication_backend.kafka;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This consumer listens to "gpt-responses", keeps a trailing summary + top mistakes,
 * calls OpenAiClient.getSpeechSummary(...) each time, then produces final summary to "speech-final".
 */
@Service
public class SpeechFinalConsumer {

    private final OpenAiClient openAiClient;
    private final ChatProducer chatProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${speech.final.topic:speech-final}")
    private String speechFinalTopic;

    // Running trailing summary
    private String trailingSummary = "";

    // top 3 mistakes
    private final List<Map<String, String>> top3Mistakes = new ArrayList<>();

    public SpeechFinalConsumer(OpenAiClient openAiClient, ChatProducer chatProducer) {
        this.openAiClient = openAiClient;
        this.chatProducer = chatProducer;
    }

    @KafkaListener(topics = "gpt-responses", groupId = "speech-final-group")
    public void consume(String gptResponseJson) {
        try {
            // 1) Parse JSON from GPT response
            JsonNode json = objectMapper.readTree(gptResponseJson);

            // 2) Convert entire JSON to string for "newGptResponse"
            String newGptResponse = json.toString();

            // 3) Attempt to detect some "exchangeId"
            String exchangeId = (json.has("exchangeId")) 
                    ? json.get("exchangeId").asText() 
                    : "unknown_exchange";

            // 4) Update top mistakes logic
            updateTopMistakes(newGptResponse, exchangeId);

            // 5) Call the new method in OpenAiClient
            String updatedSummary = openAiClient.getSpeechSummary(
                    newGptResponse,
                    trailingSummary,
                    formatTop3MistakesForPrompt()
            );

            // 6) Save the updated summary
            trailingSummary = updatedSummary;

            // 7) Build JSON to produce
            Map<String, Object> outputPayload = new HashMap<>();
            outputPayload.put("trailingSummary", trailingSummary);
            outputPayload.put("top3Mistakes", top3Mistakes);

            String outputJson = objectMapper.writeValueAsString(outputPayload);

            // 8) Produce to "speech-final"
            chatProducer.sendMessage(outputJson, speechFinalTopic);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Looks for lines that start with "Mistake:" or "Exchange:".
     * We store them as { mistakeText, exchangeRef } in top3Mistakes
     */
    private void updateTopMistakes(String text, String exchangeId) {
        // DEBUG: Print the entire text so we can see what's coming in
        System.out.println("=== updateTopMistakes RAW TEXT ===\n" + text + "\n===================================");

        // split by lines
        String[] lines = text.split("\n");

        // We'll iterate line-by-line, looking for "Mistake:" and then next line for "Exchange:"
        for (int i = 0; i < lines.length; i++) {

            // remove invisible control chars, then trim
            String cleanedLine = lines[i].replaceAll("\\p{C}+", "").trim();

            // debug each line
            System.out.println("Line " + i + ": [" + cleanedLine + "]");

            // compare in lowercase
            String lc = cleanedLine.toLowerCase();
            if (lc.startsWith("mistake:")) {
                // The part after "Mistake:"
                String mistakeText = cleanedLine.substring("Mistake:".length()).trim();

                // next line might be "Exchange:"
                String exchangeSnippet = exchangeId;
                if (i + 1 < lines.length) {
                    String nextLine = lines[i + 1].replaceAll("\\p{C}+", "").trim();
                    System.out.println("Potential next line for Exchange: [" + nextLine + "]");
                    String nextLc = nextLine.toLowerCase();

                    if (nextLc.startsWith("exchange:")) {
                        exchangeSnippet = nextLine.substring("Exchange:".length()).trim();
                    }
                }

                // debug
                System.out.println("Found MISTAKE => '" + mistakeText + "' ; EXCHANGE => '" + exchangeSnippet + "'");

                // now check if we already have it
                boolean alreadyExists = false;
                for (Map<String, String> existing : top3Mistakes) {
                    String existMistake = existing.get("mistakeText");
                    String existExchange = existing.get("exchangeRef");
                    if (equalsIgnoreCaseAndTrim(existMistake, mistakeText)
                            && equalsIgnoreCaseAndTrim(existExchange, exchangeSnippet)) {
                        alreadyExists = true;
                        break;
                    }
                }

                if (!alreadyExists) {
                    // if we have < 3 mistakes, add it
                    if (top3Mistakes.size() < 3) {
                        Map<String, String> entry = new HashMap<>();
                        entry.put("mistakeText", mistakeText);
                        entry.put("exchangeRef", exchangeSnippet);
                        top3Mistakes.add(entry);
                    }
                }
            }
        }
    }

    private boolean equalsIgnoreCaseAndTrim(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.trim().equalsIgnoreCase(b.trim());
    }

    /**
     * Convert top3Mistakes into a small bullet list.
     */
    private String formatTop3MistakesForPrompt() {
        if (top3Mistakes.isEmpty()) {
            return "No known mistakes so far.";
        }

        StringBuilder sb = new StringBuilder();
        int idx = 1;
        for (Map<String, String> mistakeMap : top3Mistakes) {
            String mistakeText = mistakeMap.get("mistakeText");
            String exchRef = mistakeMap.get("exchangeRef");

            sb.append(idx).append(") ")
              .append(mistakeText)
              .append(" [found in exchange: ")
              .append(exchRef)
              .append("]\n");

            idx++;
        }
        return sb.toString();
    }
}
