package com.communication.communication_backend.kafka;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PipelineController {

    private final ExchangeProducer exchangeProducer;
    private final ChatGPTProducer chatGPTProducer;

    public PipelineController(ExchangeProducer exchangeProducer, ChatGPTProducer chatGPTProducer) {
        this.exchangeProducer = exchangeProducer;
        this.chatGPTProducer = chatGPTProducer;
    }

    @GetMapping("/api/processedExchanges")
    public List<String> getProcessedExchanges() {
        return exchangeProducer.getProcessedExchanges();
    }

    @GetMapping("/api/empathyRatedExchanges")
    public List<String> getEmpathyRatedExchanges() {
        return chatGPTProducer.getEmpathyRatedExchanges();
    }
}
