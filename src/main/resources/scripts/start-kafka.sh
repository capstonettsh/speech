#!/bin/bash

# Step 1: Start Kafka (which includes Zookeeper) using Docker Compose
echo "Starting Kafka (and Zookeeper inside fast-data-dev) using Docker Compose..."
docker-compose -f kafka.yml up -d

echo "Waiting for Kafka (fast-data-dev) to start..."
sleep 5

# Step 2: Check if fast-data-dev is running
echo "Checking running containers..."
running_containers=$(docker ps --format "{{.Names}}")

if [[ $running_containers == *"fast-data-dev"* ]]; then
    echo "Confirmed that Landoop fast-data-dev container is running."
    echo "Containers running:"
    docker ps --filter "name=fast-data-dev" --format " - {{.Names}} ({{.Status}})"
else
    echo "Error: Landoop fast-data-dev container is not running."
    echo "Please check the Docker logs for more details:"
    echo " - Run 'docker logs fast-data-dev' for specific container logs."
    exit 1
fi

# Optional: Display logs for the fast-data-dev container
echo -e "\nLogs for fast-data-dev (last 5 lines):"
docker logs fast-data-dev --tail 5

echo -e "\nKafka (via fast-data-dev) setup completed successfully."
