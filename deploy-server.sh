#!/bin/bash

# Deploy script for AirLabs App on Ubuntu Server

echo "🚀 Starting deployment..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="hienminh1332004/airlabs-app:latest"
CONTAINER_NAME="airlabs-app"
PORT="8080"

# Step 1: Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "${YELLOW}Docker not found. Installing Docker...${NC}"
    sudo apt update
    sudo apt install docker.io -y
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
    echo "${GREEN}✓ Docker installed${NC}"
else
    echo "${GREEN}✓ Docker is already installed${NC}"
fi

# Step 2: Stop and remove old container
echo "${YELLOW}Stopping old container...${NC}"
docker stop $CONTAINER_NAME 2>/dev/null || true
docker rm $CONTAINER_NAME 2>/dev/null || true
echo "${GREEN}✓ Old container removed${NC}"

# Step 3: Pull latest image
echo "${YELLOW}Pulling latest image from Docker Hub...${NC}"
docker pull $IMAGE_NAME
echo "${GREEN}✓ Image pulled successfully${NC}"

# Step 4: Run new container
echo "${YELLOW}Starting new container...${NC}"
docker run -d \
  --name $CONTAINER_NAME \
  -p $PORT:$PORT \
  --restart unless-stopped \
  $IMAGE_NAME

echo "${GREEN}✓ Container started${NC}"

# Step 5: Show status
echo ""
echo "📊 Container Status:"
docker ps | grep $CONTAINER_NAME

echo ""
echo "📝 Recent Logs:"
docker logs --tail 20 $CONTAINER_NAME

echo ""
echo "${GREEN}✅ Deployment completed!${NC}"
echo "🌐 Application is running at: http://$(curl -s ifconfig.me):$PORT"
echo ""
echo "💡 Useful commands:"
echo "  - View logs: docker logs -f $CONTAINER_NAME"
echo "  - Stop app:  docker stop $CONTAINER_NAME"
echo "  - Restart:   docker restart $CONTAINER_NAME"
