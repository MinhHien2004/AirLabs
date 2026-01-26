#!/bin/bash

IMAGE=$1
ENVIRONMENT=$2
PORT=$3
CONTAINER_NAME="airlabs-app-${ENVIRONMENT}"

echo "======================================"
echo "Starting deployment..."
echo "Image: $IMAGE"
echo "Environment: $ENVIRONMENT"
echo "Port: $PORT"
echo "Container: $CONTAINER_NAME"
echo "======================================"

# Pull latest image
echo ""
echo "Pulling Docker image..."
docker pull $IMAGE

if [ $? -ne 0 ]; then
    echo "Failed to pull image"
    exit 1
fi

# Stop and remove old container
echo ""
echo "Stopping old container..."
docker stop $CONTAINER_NAME 2>/dev/null || true
docker rm $CONTAINER_NAME 2>/dev/null || true

# Run new container
echo ""
echo "Starting new container on port $PORT..."
docker run -d \
  --name $CONTAINER_NAME \
  --restart unless-stopped \
  -p $PORT:8080 \
  -e SPRING_PROFILES_ACTIVE=${ENVIRONMENT} \
  -e REDIS_HOST=${REDIS_HOST} \
  -e REDIS_PORT=${REDIS_PORT} \
  -e REDIS_PASSWORD=${REDIS_PASSWORD} \
  -e AIRLABS_API_KEY=${AIRLABS_API_KEY} \
  $IMAGE

# Check if container is running
if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
    echo ""
    echo "Deployment successful!"
    echo ""
    echo "Container status:"
    docker ps | grep $CONTAINER_NAME
    echo ""
    echo "Last 20 logs:"
    docker logs --tail 20 $CONTAINER_NAME
    
    # Clean up old images (keep last 3)
    echo ""
    echo "Cleaning up old images..."
    docker images --format "{{.Repository}}:{{.Tag}}" | grep "airlabs-realtime-flight" | grep "${ENVIRONMENT%-}" | tail -n +4 | xargs -r docker rmi 2>/dev/null || true
    
    exit 0
else
    echo ""
    echo "Deployment failed!"
    echo "Container logs:"
    docker logs $CONTAINER_NAME 2>&1
    exit 1
fi
