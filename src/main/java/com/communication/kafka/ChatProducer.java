package com.communication.communication_backend.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ChatProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Overloaded sendMessage to allow specifying topic
    public void sendMessage(String message, String topic) {
        kafkaTemplate.send(topic, message).whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.println("Message sent successfully to " + topic + ": " + message);
            } else {
                System.err.println("Failed to send message: " + message + ", error: " + ex.getMessage());
            }
        });
    }
}
