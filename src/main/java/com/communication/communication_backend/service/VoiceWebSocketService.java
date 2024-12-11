package com.communication.communication_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class VoiceWebSocketService extends BinaryWebSocketHandler {

    private ByteArrayOutputStream audioData;
    private HumeAIWebSocketClient humeAIClient;
    private String userId = "1";

    @Autowired
    private ApplicationContext context;

    @Value("${humeai.api.key}")
    private String humeAiApiKey;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Initialize ByteArrayOutputStream to collect audio data
        audioData = new ByteArrayOutputStream();
        System.out.println(humeAiApiKey);

        URI humeUri = new URI("wss://api.hume.ai/v0/evi/chat?api_key=" + humeAiApiKey + "&config_id=68c878da-16b6-4174-8895-4aea6a3f4879");

        humeAIClient = context.getBean(HumeAIWebSocketClient.class, humeUri, session, generateTopicName());
        humeAIClient.connectBlocking();
    }

    private String generateTopicName() {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        // Replace colons in time to avoid issues in topic naming
        currentTime = currentTime.replace(":", "-");
        return currentTime + "_" + "humeai-data-testingasdfkjasdlkfj" + "_" + userId;
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        audioData.write(message.getPayload().array());

        if (humeAIClient != null && humeAIClient.isOpen()) {
            humeAIClient.sendAudioData(message.getPayload().array());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (humeAIClient != null && humeAIClient.isOpen()) {
            humeAIClient.closeStream();
        }

        audioData.close();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        exception.printStackTrace();
    }
}
