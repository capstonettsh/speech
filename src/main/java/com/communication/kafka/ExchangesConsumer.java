package com.communication.communication_backend.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ExchangesConsumer {

    private final OpenAiClient openAiClient;
    private final ChatGPTProducer chatGPTProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gpt.responses.topic:gpt-responses}")
    private String gptResponsesTopic;

    public ExchangesConsumer(OpenAiClient openAiClient, ChatGPTProducer chatGPTProducer) {
        this.openAiClient = openAiClient;
        this.chatGPTProducer = chatGPTProducer;
    }

    @KafkaListener(topics = "exchanges", groupId = "exchanges-group")
    public void consume(String exchangeJson) {
        try {
            // exchangeJson is a JSON representation of the user-assistant exchange
            System.out.println("Received exchange for ChatGPT processing: " + exchangeJson);

            // Get empathy rating from ChatGPT
            String rating = openAiClient.getEmpathyRating(exchangeJson);

            // Append rating to the exchange JSON
            JsonNode exchangeNode = objectMapper.readTree(exchangeJson);
            // Convert to ObjectNode to add a field
            com.fasterxml.jackson.databind.node.ObjectNode node = (com.fasterxml.jackson.databind.node.ObjectNode) exchangeNode;
            node.put("rating", rating);

            String updatedExchange = objectMapper.writeValueAsString(node);

            // Send updated exchange to GPT responses topic
            chatGPTProducer.sendRatedExchange(updatedExchange, gptResponsesTopic);

        } catch (Exception e) {
            System.err.println("Error processing exchange with ChatGPT: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
