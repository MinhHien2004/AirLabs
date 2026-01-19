#!/bin/sh

set -e

echo "================================================"
echo "Starting deployment process..."
echo "Commit message: $1"
echo "================================================"

# Pull latest image from Docker Hub
echo "Pulling latest image..."
docker pull hienminh1332004/airlabs-realtime-flight:latest

# Stop and remove old container
echo "Stopping old container..."
docker stop airlabs-app 2>/dev/null || true
docker rm airlabs-app 2>/dev/null || true

# Run new container
echo "Starting new container..."
docker run -d \
  --name airlabs-app \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  hienminh1332004/airlabs-realtime-flight:latest

# Wait for container to start
sleep 5

# Check if container is running
if [ "$(docker ps -q -f name=airlabs-app)" ]; then
    echo "✅ Container started successfully!"
    docker logs airlabs-app --tail 20
else
    echo "❌ Container failed to start!"
    docker logs airlabs-app --tail 50
    exit 1
fi

# Clean up old images
echo "Cleaning up old images..."
docker image prune -f

echo "================================================"
echo "✅ Deployment completed successfully!"
echo "================================================"
