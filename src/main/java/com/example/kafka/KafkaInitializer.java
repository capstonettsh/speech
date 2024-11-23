package com.example.kafka;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class KafkaInitializer {

    @PostConstruct
    public void startKafka() {
        try {
            System.out.println("Starting Kafka and Zookeeper using start-kafka.sh script...");
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "src/main/resources/scripts/start-kafka.sh");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Capture and print the output from the script
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Kafka and Zookeeper started successfully.");
            } else {
                System.out.println("Failed to start Kafka and Zookeeper. Check script logs for details.");
            }
        } catch (Exception e) {
            System.err.println("Error running the start-kafka.sh script: " + e.getMessage());
            e.printStackTrace();
        }
    }
}