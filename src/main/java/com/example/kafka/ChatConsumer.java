package com.example.kafka;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ChatConsumer {

    private String lastRole = null;
    private String lastContent = null;
    private Map<String, String> lastEmotions = null;
    private final List<Map<String, String>> exchanges = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExchangeProducer exchangeProducer;

    @Value("${kafka.exchanges.topic}")
    private String processedExchangesTopic;

    public ChatConsumer(ExchangeProducer exchangeProducer) {
        this.exchangeProducer = exchangeProducer;
    }

    @KafkaListener(topics = "${kafka.topic}", groupId = "chat-group")
    public void consume(ConsumerRecord<String, String> record) {
        String message = record.value();

        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String type = jsonNode.has("type") ? jsonNode.get("type").asText() : null;

            if (type == null || (!type.equals("assistant_message") && !type.equals("user_message"))) {
                return;
            }

            JsonNode messageNode = jsonNode.get("message");
            JsonNode scoresNode = jsonNode.at("/models/prosody/scores");

            if (messageNode == null) {
                return;
            }

            String role = messageNode.has("role") ? messageNode.get("role").asText() : null;
            String content = messageNode.has("content") ? messageNode.get("content").asText() : null;

            if (role == null || content == null) {
                return;
            }

            Map<String, String> topEmotions = getTopEmotions(scoresNode);
            processMessage(role, content.trim(), topEmotions);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> getTopEmotions(JsonNode scoresNode) {
        if (scoresNode == null || !scoresNode.isObject()) {
            return Collections.emptyMap();
        }

        Map<String, Double> emotions = new HashMap<>();
        scoresNode.fields().forEachRemaining(entry -> emotions.put(entry.getKey(), entry.getValue().asDouble()));

        return emotions.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(3)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.format("%.2f", e.getValue()),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private void processMessage(String role, String content, Map<String, String> emotions) {
        if (lastRole == null) {
            lastRole = role;
            lastContent = content;
            lastEmotions = emotions;
        } else if (!lastRole.equals(role)) {
            saveExchange(lastRole, lastContent, lastEmotions, role, content, emotions);
            lastRole = null;
            lastContent = null;
            lastEmotions = null;
        } else {
            lastContent += " " + content;
        }
    }

    private void saveExchange(String firstRole, String firstContent, Map<String, String> firstEmotions,
                              String secondRole, String secondContent, Map<String, String> secondEmotions) {
        LinkedHashMap<String, String> exchange = new LinkedHashMap<>();
        exchange.put(firstRole, appendEmotionsToContent(firstContent, firstEmotions));
        exchange.put(secondRole, appendEmotionsToContent(secondContent, secondEmotions));

        exchanges.add(exchange);
        String exchangeAsString = serializeExchange(exchange); // Serialize exchange properly
        exchangeProducer.sendProcessedExchange(exchangeAsString, processedExchangesTopic);
    }

    private String appendEmotionsToContent(String content, Map<String, String> emotions) {
        if (emotions.isEmpty()) {
            return content;
        }

        StringBuilder emotionString = new StringBuilder(" (");
        emotions.forEach((emotion, score) -> emotionString.append(emotion).append(": ").append(score).append(", "));
        emotionString.setLength(emotionString.length() - 2);
        emotionString.append(")");

        return content + emotionString;
    }

    private String serializeExchange(Map<String, String> exchange) {
        try {
            return objectMapper.writeValueAsString(exchange);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}"; // Return empty JSON if serialization fails
        }
    }

    public List<Map<String, String>> getExchanges() {
        flushBuffer();
        return exchanges;
    }

    private void flushBuffer() {
        if (lastRole != null && lastContent != null) {
            Map<String, String> incompleteExchange = new LinkedHashMap<>();
            incompleteExchange.put(lastRole, appendEmotionsToContent(lastContent, lastEmotions));
            exchanges.add(incompleteExchange);

            String exchangeAsString = serializeExchange(incompleteExchange);
            exchangeProducer.sendProcessedExchange(exchangeAsString, processedExchangesTopic);

            lastRole = null;
            lastContent = null;
            lastEmotions = null;
        }
    }
}
