package com.communication.communication_backend.kafka;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class FinalSpeechController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // We'll store just one "latest record"
    private Map<String, Object> latestRecord = Collections.synchronizedMap(new HashMap<>());

    @KafkaListener(topics = "speech-final", groupId = "speech-final-web-group")
    public void consumeSpeechFinal(String payload) {
        try {
            // We expect payload to be something like:
            // {
            //   "trailingSummary": "...some text...",
            //   "top3Mistakes": [
            //       { "mistakeText": "...", "exchangeRef": "..." },
            //       ...
            //   ]
            // }
            JsonNode json = objectMapper.readTree(payload);
            synchronized (latestRecord) {
                latestRecord.clear();
                latestRecord.put("trailingSummary", json.has("trailingSummary") ? json.get("trailingSummary").asText() : "");
                // We can store the entire "top3Mistakes" array as a raw string or something
                // We'll parse it more carefully if we want:
                if (json.has("top3Mistakes")) {
                    latestRecord.put("top3Mistakes", json.get("top3Mistakes"));
                } else {
                    latestRecord.put("top3Mistakes", "[]");
                }
            }
            System.out.println("Received final speech summary: " + payload);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/api/speechFinal")
    public Map<String, Object> getSpeechFinalData() {
        // Return the latest record
        synchronized (latestRecord) {
            return new HashMap<>(latestRecord);
        }
    }
}
