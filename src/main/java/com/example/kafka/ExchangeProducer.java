package com.example.kafka;

import java.util.ArrayList;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ExchangeProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final List<String> processedExchanges = new ArrayList<>(); // In-memory storage for processed exchanges

    public ExchangeProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendProcessedExchange(String exchange, String topic) {
        try {
            kafkaTemplate.send(topic, exchange);
            synchronized (processedExchanges) {
                processedExchanges.add(exchange); // Save for retrieval via REST
            }
            System.out.println("Sent processed exchange to topic: " + topic + " - " + exchange);
        } catch (Exception e) {
            System.err.println("Error sending processed exchange: " + e.getMessage());
        }
    }

    public List<String> getProcessedExchanges() {
        synchronized (processedExchanges) {
            return new ArrayList<>(processedExchanges); // Return a copy for thread safety
        }
    }
}
