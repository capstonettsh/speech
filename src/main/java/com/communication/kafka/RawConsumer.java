package com.communication.communication_backend.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RawConsumer {

    // Consumes raw messages from Hume AI (with full tone data)
    // Extracts role, content, determines top 3 emotions, produces a shortened record to "shortened".
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatProducer chatProducer; // We'll reuse a ChatProducer-like class to send to shortened

    @Value("${shortened.topic:shortened}")
    private String shortenedTopic;

    public RawConsumer(ChatProducer chatProducer) {
        this.chatProducer = chatProducer;
    }

    @KafkaListener(topics = "raw", groupId = "raw-group")
    public void consume(String record) {
        try {
            JsonNode jsonNode = objectMapper.readTree(record);

            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "";
            if(!type.equals("assistant_message") && !type.equals("user_message")) {
                // We only process messages that have a user or assistant role
                // Adjust conditions as per your real schema
                return;
            }

            JsonNode messageNode = jsonNode.get("message");
            if (messageNode == null) return;

            String role = messageNode.has("role") ? messageNode.get("role").asText() : null;
            String content = messageNode.has("content") ? messageNode.get("content").asText() : null;
            if (role == null || content == null) return;

            // Extract scores (tone data)
            JsonNode scoresNode = jsonNode.at("/models/prosody/scores");
            Map<String, Double> emotionMap = new HashMap<>();
            if (scoresNode != null && scoresNode.isObject()) {
                Iterator<Map.Entry<String,JsonNode>> fields = scoresNode.fields();
                while(fields.hasNext()) {
                    Map.Entry<String,JsonNode> entry = fields.next();
                    emotionMap.put(entry.getKey(), entry.getValue().asDouble());
                }
            }

            // Get top 3 emotions
            LinkedHashMap<String,String> top3 = emotionMap.entrySet().stream()
                    .sorted((e1,e2)->Double.compare(e2.getValue(), e1.getValue()))
                    .limit(3)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> String.format("%.2f", e.getValue()),
                            (a,b)->a, LinkedHashMap::new
                    ));

            // Create shortened JSON
            Map<String,Object> shortened = new HashMap<>();
            shortened.put("role", role);
            shortened.put("content", content);
            shortened.put("top_emotions", top3);

            String shortenedMessage = objectMapper.writeValueAsString(shortened);
            chatProducer.sendMessage(shortenedMessage, shortenedTopic);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
