package com.communication.communication_backend.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

@Configuration
public class WebSocketClientConfig {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public WebSocketClientConfig(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Bean
    @Scope("prototype")
    public HumeAIWebSocketClient humeAIWebSocketClient(URI serverUri, WebSocketSession frontendSession, String topicName) {
        return new HumeAIWebSocketClient(serverUri, frontendSession, kafkaTemplate, topicName);
    }
}