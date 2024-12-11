package com.communication.communication_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HumeAIWebSocketClient extends WebSocketClient {

    private final WebSocketSession frontendSession;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int userId = 1;
    private final String topicName;

    public HumeAIWebSocketClient(URI serverUri, WebSocketSession frontendSession,
                                 KafkaTemplate<String, String> kafkaTemplate, String topicName) {
        super(serverUri);
        this.frontendSession = frontendSession;
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        // Connection to Hume AI opened
        System.out.println("Connected to Hume AI");
    }

    public void sendAudioData(byte[] audioData) {
        send(audioData);
    }

    @Override
    public void onMessage(String message) {
        // Handle text messages from Hume AI
        try {
            // Parse the JSON message
            JsonNode rootNode = objectMapper.readTree(message);
            String type = rootNode.get("type").asText();
            System.out.println(message);

            if ("audio_output".equals(type)) {
                frontendSession.sendMessage(new TextMessage(message));
            }

            kafkaTemplate.send("raw", message);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateTopicName() {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        // Replace colons in time to avoid issues in topic naming
        currentTime = currentTime.replace(":", "-");
        return "test" + "_" + "humeai-data" + "_" + userId;
    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        // Handle binary messages from Hume AI
        try {
            // Convert ByteBuffer to byte array
            byte[] data = new byte[bytes.remaining()];
            bytes.get(data);

            // Convert binary data to string (assuming UTF-8 encoding)
            String message = new String(data, StandardCharsets.UTF_8);

            // Parse the message
            JsonNode rootNode = objectMapper.readTree(message);
            String type = rootNode.get("type").asText();

            if ("audio_output".equals(type)) {
                frontendSession.sendMessage(new TextMessage(message));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeStream() {
        // Send any final messages if required by Hume AI before closing
        try {
            String endMessage = "{\"type\":\"end_stream\"}";
            send(endMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
        close();
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Hume AI WebSocket closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}
