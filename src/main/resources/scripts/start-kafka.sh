#!/bin/bash

echo "Starting Kafka and Zookeeper using Docker Compose (Landoop/LensesIO image)..."
docker-compose -f kafka.yml up -d

echo "Waiting for Kafka and Zookeeper to start..."
sleep 10

echo "Checking running containers..."
running_containers=$(docker ps --format "{{.Names}}")

if [[ $running_containers == *"fast-data-dev"* ]]; then
    echo "Confirmed that the Landoop Kafka (fast-data-dev) container is running."
    echo "Containers running:"
    docker ps --filter "name=fast-data-dev" --format " - {{.Names}} ({{.Status}})"
else
    echo "Error: Landoop Kafka container is not running."
    echo "Please check the Docker logs for more details:"
    docker logs fast-data-dev
    exit 1
fi

echo -e "\nLogs for the fast-data-dev container:"
docker logs fast-data-dev --tail 5

echo -e "\nKafka and Zookeeper setup completed successfully."
