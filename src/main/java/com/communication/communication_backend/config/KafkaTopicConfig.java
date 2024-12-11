package com.communication.communication_backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Raw topic for unprocessed messages
    @Bean
    public NewTopic rawTopic() {
        return TopicBuilder.name("raw").partitions(1).replicas(1).build();
    }

    // Shortened topic for compressed messages (just speech and top 3 emotions)
    @Bean
    public NewTopic shortenedTopic() {
        return TopicBuilder.name("shortened").partitions(1).replicas(1).build();
    }

    // Exchanges topic for grouped assistant/user exchanges
    @Bean
    public NewTopic exchangesTopic() {
        return TopicBuilder.name("exchanges").partitions(1).replicas(1).build();
    }

    // GPT responses topic for ChatGPT-rated exchanges
    @Bean
    public NewTopic gptResponsesTopic() {
        return TopicBuilder.name("gpt-responses").partitions(1).replicas(1).build();
    }
}
