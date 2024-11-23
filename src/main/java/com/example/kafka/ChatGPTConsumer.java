package com.example.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ChatGPTConsumer {

    private final OpenAiClient openAiClient;
    private final ChatGPTProducer chatGPTProducer;

    @Value("${kafka.gpt.topic}")
    private String gptTopic;

    public ChatGPTConsumer(OpenAiClient openAiClient, ChatGPTProducer chatGPTProducer) {
        this.openAiClient = openAiClient;
        this.chatGPTProducer = chatGPTProducer;
    }

    @KafkaListener(topics = "${kafka.exchanges.topic}", groupId = "gpt-group")
    public void consume(String exchange) {
        try {
            System.out.println("Received exchange for ChatGPT processing: " + exchange);

            // Get empathy rating from ChatGPT
            String rating = openAiClient.getEmpathyRating(exchange);

            // Append rating to exchange JSON
            String updatedExchange = exchange.substring(0, exchange.length() - 1) +
                                     ", \"rating\": \"" + rating + "\"}";

            // Send updated exchange to the next Kafka topic
            chatGPTProducer.sendRatedExchange(updatedExchange, gptTopic);

        } catch (Exception e) {
            System.err.println("Error processing exchange with ChatGPT: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
