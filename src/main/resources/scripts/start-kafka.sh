#!/bin/bash

# Step 1: Start Kafka and Zookeeper
echo "Starting Kafka and Zookeeper using Docker Compose..."
docker-compose -f kafka.yml up -d

# Wait for a few seconds to ensure Kafka and Zookeeper start up
echo "Waiting for Kafka and Zookeeper to start..."
sleep 5

# Step 2: Check if Kafka and Zookeeper are running
echo "Running command: docker ps"
running_containers=$(docker ps --format "{{.Names}}")

if [[ $running_containers == *"zookeeper"* && $running_containers == *"kafka"* ]]; then
    echo "Confirmed that Kafka and Zookeeper are running."
    echo "Containers running:"
    docker ps --filter "name=kafka" --filter "name=zookeeper" --format " - {{.Names}} ({{.Status}})"
else
    echo "Error: Kafka and/or Zookeeper are not running."
    echo "Please check the Docker logs for more details:"
    echo " - Run 'docker logs zookeeper' or 'docker logs kafka' for specific container logs."
    exit 1
fi

# Optional: Display logs for Kafka and Zookeeper for further confirmation
echo -e "\nLogs for Kafka and Zookeeper:"
echo "Displaying the last 5 lines of each container's log for quick verification:"
echo -e "\nKafka Logs:"
docker logs kafka --tail 5
echo -e "\nZookeeper Logs:"
docker logs zookeeper --tail 5

echo -e "\nKafka and Zookeeper setup completed successfully."
