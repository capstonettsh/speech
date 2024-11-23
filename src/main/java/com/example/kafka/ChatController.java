package com.example.kafka;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatProducer chatProducer;
    private final ChatConsumer chatConsumer; 
    private final ExchangeProducer exchangeProducer;
    private final ChatGPTProducer chatGPTProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatController(ChatProducer chatProducer, ChatConsumer chatConsumer, ExchangeProducer exchangeProducer, ChatGPTProducer chatGPTProducer) {
        this.chatProducer = chatProducer;
        this.chatConsumer = chatConsumer;
        this.exchangeProducer = exchangeProducer;
        this.chatGPTProducer = chatGPTProducer;
    }

    @GetMapping("/sendMessagesFromFile")
    public void sendMessagesFromFile() {
        try (InputStream inputStream = getClass().getResourceAsStream("/messages.json")) {
            if (inputStream == null) {
                System.out.println("ERROR: messages.json not found in the resources folder!");
                return;
            }

            List<JsonNode> messages = objectMapper.readValue(inputStream, new TypeReference<List<JsonNode>>() {});
            System.out.println("Loaded " + messages.size() + " messages from messages.json");

            for (JsonNode messageNode : messages) {
                String messageAsString = messageNode.toString();
                chatProducer.sendMessage(messageAsString);
                Thread.sleep(100); // Optional delay for simulation
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/consumerLogs")
    public List<Map<String, String>> getConsumerLogs() {
        return chatConsumer.getExchanges();
    }

    @GetMapping("/processedExchanges")
    public List<String> getProcessedExchanges() {
        return exchangeProducer.getProcessedExchanges();
    }

    @GetMapping("/empathyRatedExchanges")
    public List<String> getEmpathyRatedExchanges() {
        return chatGPTProducer.getEmpathyRatedExchanges();
    }
}
