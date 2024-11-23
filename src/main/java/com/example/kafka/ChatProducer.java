package com.example.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
public class ChatProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic}")
    private String topic;

    public ChatProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String message) {
        System.out.println("Sending message to Kafka: " + message);
        kafkaTemplate.send(topic, message).whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.println("Message sent successfully: " + message);
            } else {
                System.err.println("Failed to send message: " + message + ", error: " + ex.getMessage());
            }
        });
    }
    
}
