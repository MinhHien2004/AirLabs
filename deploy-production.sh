#!/bin/bash

set -e

echo "=========================================="
echo "Starting Production Deployment"
echo "=========================================="

# Configuration
IMAGE_TAG="${1:-prod-latest}"  # Lấy tag từ parameter, default là prod-latest
IMAGE="hienminh1332004/airlabs-realtime-flight:$IMAGE_TAG"
CONTAINER_NAME="airlabs-app-production"
PORT=8080
ENVIRONMENT="production"

echo "Deploying with image tag: $IMAGE_TAG"

# Login Docker Hub
echo "Logging in to Docker Hub..."
if [ -n "$DOCKERHUB_TOKEN" ] && [ -n "$DOCKERHUB_USERNAME" ]; then
    echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USERNAME" --password-stdin
else
    echo "DOCKERHUB credentials not found. Assuming already logged in or using public image."
fi

# Pull latest image
echo "Pulling latest image from Docker Hub..."
docker pull $IMAGE

# Stop and remove old container
echo "Stopping old container..."
docker stop $CONTAINER_NAME 2>/dev/null || echo "No container to stop"
docker rm $CONTAINER_NAME 2>/dev/null || echo "No container to remove"

# Run new container
echo "Starting new container..."
docker run -d \
    --name $CONTAINER_NAME \
    --restart unless-stopped \
    -p $PORT:8080 \
    -e SPRING_PROFILES_ACTIVE=$ENVIRONMENT \
    -e REDIS_HOST="${REDIS_HOST:-localhost}" \
    -e REDIS_PORT="${REDIS_PORT:-6379}" \
    -e REDIS_PASSWORD="${REDIS_PASSWORD:-}" \
    -e AIRLABS_API_KEY="${AIRLABS_API_KEY:-}" \
    $IMAGE

# Wait for container to start
echo "Waiting for container to start..."
sleep 5

# Verify deployment
if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
    echo "Deployment successful!"
    docker ps | grep $CONTAINER_NAME
    echo ""
    echo "Container logs:"
    docker logs --tail 20 $CONTAINER_NAME
else
    echo "Deployment failed!"
    docker logs $CONTAINER_NAME
    exit 1
fi

# Health check
echo ""
echo "Performing health check..."
MAX_RETRIES=10
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT/actuator/health 2>/dev/null || echo "000")
    
    if [ "$HTTP_CODE" -eq 200 ]; then
        echo "Health check passed!"
        echo ""
        echo "=========================================="
        echo "Production Deployment completed successfully!"
        echo "Application is running on port $PORT"
        echo "=========================================="
        exit 0
    fi
    
    echo "Attempt $((RETRY_COUNT + 1))/$MAX_RETRIES - Status: $HTTP_CODE"
    sleep 3
    RETRY_COUNT=$((RETRY_COUNT + 1))
done

echo "Health check failed after $MAX_RETRIES attempts"
echo "Container is running but health endpoint is not responding"
exit 1

# Cleanup old images (keep last 3)
echo "Cleaning up old images..."
docker images --format "{{.Repository}}:{{.Tag}}" | \
    grep "airlabs-realtime-flight" | \
    tail -n +4 | xargs -r docker rmi 2>/dev/null || echo "No old images to remove"
