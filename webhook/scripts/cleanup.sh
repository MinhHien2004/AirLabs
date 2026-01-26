#!/bin/bash

echo "🧹 Cleaning up all airlabs containers..."

# Stop all airlabs containers
docker ps | grep airlabs | awk '{print $1}' | xargs -r docker stop

# Remove all airlabs containers
docker ps -a | grep airlabs | awk '{print $1}' | xargs -r docker rm

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "📊 Remaining containers:"
docker ps -a
