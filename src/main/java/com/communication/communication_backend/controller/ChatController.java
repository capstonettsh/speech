package com.communication.communication_backend.controller;

import com.communication.communication_backend.service.VoiceWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class ChatController implements WebSocketConfigurer {
    @Autowired
    private VoiceWebSocketService voiceWebSocketService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(voiceWebSocketService, "/v0/chat")
                .setAllowedOrigins("*"); // Adjust allowed origins as needed
    }
}
