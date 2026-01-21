#!/bin/bash

# Script để chạy trên server sau khi pull code về
# Sử dụng: bash webhook/server-setup.sh

set -e

echo "🚀 Starting webhook setup on server..."

# Kiểm tra Docker đã cài chưa
if ! command -v docker &> /dev/null; then
    echo "❌ Docker chưa được cài đặt. Đang cài đặt..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $USER
    rm get-docker.sh
    echo "✅ Docker đã được cài đặt"
else
    echo "✅ Docker đã có sẵn"
fi

# Kiểm tra Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose chưa được cài đặt. Đang cài đặt..."
    sudo apt update
    sudo apt install docker-compose -y
    echo "✅ Docker Compose đã được cài đặt"
else
    echo "✅ Docker Compose đã có sẵn"
fi

# Di chuyển vào thư mục webhook
cd "$(dirname "$0")"
WEBHOOK_DIR=$(pwd)
echo "📂 Working directory: $WEBHOOK_DIR"

# Kiểm tra file hooks.json
if [ ! -f "hooks.json" ]; then
    echo "❌ File hooks.json không tồn tại!"
    exit 1
fi

# Kiểm tra webhook secret
if grep -q "YOUR_WEBHOOK_SECRET_HERE" hooks.json; then
    echo "⚠️  CẢNH BÁO: Webhook secret chưa được cấu hình!"
    echo ""
    echo "Tạo secret ngẫu nhiên:"
    SECRET=$(openssl rand -base64 32)
    echo "Secret: $SECRET"
    echo ""
    echo "Bạn cần:"
    echo "1. Copy secret này"
    echo "2. Thay thế 'YOUR_WEBHOOK_SECRET_HERE' trong hooks.json"
    echo "3. Thêm secret vào GitHub Webhook settings"
    echo ""
    read -p "Bạn có muốn tự động thay thế không? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        sed -i "s/YOUR_WEBHOOK_SECRET_HERE/$SECRET/g" hooks.json
        echo "✅ Secret đã được cập nhật trong hooks.json"
        echo "⚠️  Nhớ thêm secret này vào GitHub Webhook!"
    fi
fi

# Cấp quyền thực thi cho scripts
echo "🔧 Setting execute permissions..."
chmod +x scripts/*.sh

# Dừng webhook cũ (nếu có)
echo "🛑 Stopping old webhook containers..."
docker-compose down 2>/dev/null || true

# Build và start webhook
echo "🔨 Building and starting webhook service..."
docker-compose up -d --build

# Đợi webhook khởi động
echo "⏳ Waiting for webhook to start..."
sleep 5

# Kiểm tra status
if docker ps | grep -q webhook-listener; then
    echo "✅ Webhook service is running!"
    docker-compose logs --tail 20
else
    echo "❌ Webhook failed to start!"
    docker-compose logs
    exit 1
fi

# Cấu hình firewall
echo ""
echo "🔒 Configuring firewall..."
if command -v ufw &> /dev/null; then
    sudo ufw allow 9000/tcp
    sudo ufw allow 8080/tcp
    echo "✅ Firewall rules added (ports 9000, 8080)"
else
    echo "⚠️  UFW not found, skip firewall configuration"
fi

# Lấy IP của server
SERVER_IP=$(curl -s ifconfig.me 2>/dev/null || echo "YOUR_SERVER_IP")

echo ""
echo "================================================"
echo "✅ Webhook setup completed!"
echo "================================================"
echo ""
echo "📋 Next steps:"
echo ""
echo "1. Cấu hình GitHub Webhook:"
echo "   URL: http://$SERVER_IP:9000/hooks/deploy-airlabs"
echo "   Content type: application/json"
echo "   Secret: [Secret từ hooks.json]"
echo "   Events: Just the push event"
echo ""
echo "2. Test webhook:"
echo "   curl -X POST http://localhost:9000/hooks/deploy-airlabs"
echo ""
echo "3. Xem logs:"
echo "   docker-compose logs -f"
echo ""
echo "4. Kiểm tra status:"
echo "   docker ps"
echo ""
echo "================================================"
