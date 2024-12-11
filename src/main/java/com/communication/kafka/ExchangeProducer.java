package com.communication.communication_backend.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExchangeProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final List<String> processedExchanges = new ArrayList<>();

    public ExchangeProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendProcessedExchange(String exchange, String topic) {
        kafkaTemplate.send(topic, exchange);
        synchronized (processedExchanges) {
            processedExchanges.add(exchange);
        }
        System.out.println("Sent processed exchange to topic: " + topic + " - " + exchange);
    }

    public List<String> getProcessedExchanges() {
        synchronized (processedExchanges) {
            return new ArrayList<>(processedExchanges);
        }
    }
}
