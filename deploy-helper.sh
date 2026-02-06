#!/bin/bash

# ==============================================
# Helper script để list và deploy Docker images
# Usage: ./deploy-helper.sh [qa|production] [tag]
# ==============================================

DOCKER_REPO="hienminh1332004/airlabs-realtime-flight"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================="
echo "AirLabs Deployment Helper"
echo -e "==========================================${NC}"
echo ""

# Function to list available tags
list_tags() {
    local pattern=$1
    echo -e "${YELLOW}Fetching available tags from Docker Hub...${NC}"
    echo ""
    
    TAGS=$(curl -s "https://hub.docker.com/v2/repositories/$DOCKER_REPO/tags/?page_size=50" \
        | jq -r '.results[] | "\(.name)\t\(.last_updated)"' \
        | grep "$pattern" \
        | sort -Vr \
        | head -20)
    
    if [ -z "$TAGS" ]; then
        echo -e "${RED}No tags found matching pattern: $pattern${NC}"
        return 1
    fi
    
    echo -e "${GREEN}Available tags (newest first):${NC}"
    echo "----------------------------------------"
    printf "%-20s %s\n" "TAG" "LAST UPDATED"
    echo "----------------------------------------"
    echo "$TAGS" | while IFS=$'\t' read -r tag date; do
        # Format date
        formatted_date=$(echo $date | cut -d'T' -f1)
        printf "%-20s %s\n" "$tag" "$formatted_date"
    done
    echo ""
}

# Function to deploy
deploy() {
    local env=$1
    local tag=$2
    local script=""
    
    if [ "$env" = "qa" ]; then
        script="./deploy-qa.sh"
    elif [ "$env" = "production" ]; then
        script="./deploy-production.sh"
    else
        echo -e "${RED}Invalid environment: $env${NC}"
        echo "Usage: $0 [qa|production] [tag]"
        exit 1
    fi
    
    if [ ! -f "$script" ]; then
        echo -e "${RED}Script not found: $script${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Deploying $env with tag: $tag${NC}"
    echo ""
    
    # Load environment variables
    if [ -f "/opt/airlabs/.env" ]; then
        source /opt/airlabs/.env
    fi
    
    # Run deployment
    bash "$script" "$tag"
}

# Main logic
if [ $# -eq 0 ]; then
    echo "Usage: $0 [list-qa|list-prod|qa TAG|production TAG]"
    echo ""
    echo "Examples:"
    echo "  $0 list-qa                    # List available QA tags"
    echo "  $0 list-prod                  # List available Production tags"
    echo "  $0 qa v0.0.123                # Deploy QA with specific tag"
    echo "  $0 production prod-v0.0.123   # Deploy Production with specific tag"
    echo ""
    exit 1
fi

case "$1" in
    list-qa)
        list_tags "^v0\.0\."
        ;;
    list-prod)
        list_tags "^prod-"
        ;;
    qa)
        if [ -z "$2" ]; then
            echo -e "${RED}Error: Tag required${NC}"
            echo "Usage: $0 qa TAG"
            echo ""
            list_tags "^v0\.0\."
            exit 1
        fi
        deploy "qa" "$2"
        ;;
    production)
        if [ -z "$2" ]; then
            echo -e "${RED}Error: Tag required${NC}"
            echo "Usage: $0 production TAG"
            echo ""
            list_tags "^prod-"
            exit 1
        fi
        deploy "production" "$2"
        ;;
    *)
        echo -e "${RED}Invalid command: $1${NC}"
        echo "Usage: $0 [list-qa|list-prod|qa TAG|production TAG]"
        exit 1
        ;;
esac
