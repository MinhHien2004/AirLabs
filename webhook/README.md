# 🚀 Webhook Auto-Deploy Setup Guide

Hướng dẫn thiết lập webhook để tự động deploy khi push code lên GitHub.

## 📋 Quy trình hoàn chỉnh

```
Local (Push code) → GitHub → GitHub Actions (Build & Push to Docker Hub) → Webhook (Trigger) → Server (Pull & Deploy)
```

## 🔧 Bước 1: Setup trên Local (Đã hoàn thành)

### 1.1. Push code lên GitHub

```powershell
# Thêm tất cả files
git add .

# Commit
git commit -m "Add webhook configuration"

# Push lên GitHub
git push origin main
```

### 1.2. Cấu hình GitHub Secrets

Vào repository trên GitHub:
- **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

Thêm 2 secrets:
```
DOCKERHUB_USERNAME: hienminh1332004
DOCKERHUB_ACCESS_TOKEN: [Token từ Docker Hub]
```

Cách tạo Docker Hub token:
1. Đăng nhập https://hub.docker.com/
2. Account Settings → Security → New Access Token
3. Tên: `github-actions`, Permission: Read & Write
4. Copy token và paste vào GitHub secret

## 🖥️ Bước 2: Setup trên Server

### 2.1. SSH vào server

```bash
ssh -i "path/to/your-key.pem" ubuntu@YOUR_SERVER_IP
```

### 2.2. Clone repository

```bash
# Clone repository về server
git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git
cd YOUR_REPO
```

### 2.3. Chạy script tự động setup

```bash
# Chạy script setup
cd webhook
bash server-setup.sh
```

Script này sẽ tự động:
- ✅ Kiểm tra và cài Docker/Docker Compose
- ✅ Tạo webhook secret
- ✅ Cập nhật hooks.json
- ✅ Build và start webhook service
- ✅ Cấu hình firewall
- ✅ Hiển thị hướng dẫn tiếp theo

### 2.4. (Tùy chọn) Setup thủ công nếu không dùng script

```bash
cd webhook

# Tạo webhook secret
openssl rand -base64 32

# Cập nhật hooks.json
nano hooks.json
# Thay YOUR_WEBHOOK_SECRET_HERE bằng secret vừa tạo

# Cấp quyền
chmod +x scripts/*.sh

# Start webhook
docker-compose up -d

# Kiểm tra logs
docker-compose logs -f

# Cấu hình firewall
sudo ufw allow 9000/tcp
sudo ufw allow 8080/tcp
```

## 🌐 Bước 3: Cấu hình GitHub Webhook

### 3.1. Vào GitHub Repository

- **Settings** → **Webhooks** → **Add webhook**

### 3.2. Điền thông tin

```
Payload URL: http://YOUR_SERVER_IP:9000/hooks/deploy-airlabs
Content type: application/json
Secret: [Secret từ hooks.json - đã tạo ở bước 2]
SSL verification: Disable (hoặc Enable nếu có HTTPS)
Events: Just the push event
Active: ✓
```

### 3.3. Test webhook

Click **Add webhook** → GitHub sẽ gửi ping test

Kiểm tra:
- ✅ Recent Deliveries có status 200
- ✅ Response: "Deploying application..."

## 🧪 Bước 4: Test toàn bộ quy trình

### 4.1. Push code từ local

```powershell
# Sửa code bất kỳ
echo "test" >> README.md

# Commit và push
git add .
git commit -m "Test webhook deployment"
git push origin main
```

### 4.2. Theo dõi quá trình

**Trên GitHub:**
- Tab **Actions**: Xem workflow đang build
- Tab **Settings** → **Webhooks**: Xem webhook delivery

**Trên Server:**
```bash
# Xem logs webhook
docker-compose logs -f webhook

# Xem logs deployment
docker logs -f airlabs-app
```

### 4.3. Kiểm tra kết quả

```bash
# Kiểm tra container đang chạy
docker ps

# Test application
curl http://localhost:8080/actuator/health
```

## 📊 Quản lý và Monitoring

### Xem logs

```bash
# Logs webhook service
docker-compose -f ~/YOUR_REPO/webhook/docker-compose.yml logs -f

# Logs application
docker logs -f airlabs-app

# Logs realtime
docker logs -f --tail 100 airlabs-app
```

### Restart services

```bash
# Restart webhook
cd ~/YOUR_REPO/webhook
docker-compose restart

# Restart application
docker restart airlabs-app

# Restart tất cả
docker-compose down && docker-compose up -d
```

### Kiểm tra status

```bash
# Xem containers đang chạy
docker ps

# Xem webhook endpoint
curl http://localhost:9000/hooks/deploy-airlabs

# Test application health
curl http://localhost:8080/actuator/health
```

## 🔄 Cập nhật webhook config

### Khi có thay đổi webhook config

```bash
# Pull code mới từ GitHub
cd ~/YOUR_REPO
git pull origin main

# Rebuild webhook
cd webhook
docker-compose down
docker-compose up -d --build

# Xem logs
docker-compose logs -f
```

## 🔒 Security Checklist

- ✅ Webhook secret đã được cấu hình
- ✅ GitHub secrets đã được thêm
- ✅ Firewall đã mở ports cần thiết
- ✅ Docker images từ trusted sources
- ✅ Environment variables được bảo mật

## 🛠️ Troubleshooting

### Webhook không trigger

```bash
# Kiểm tra webhook đang chạy
docker ps | grep webhook

# Test endpoint
curl -X POST http://localhost:9000/hooks/deploy-airlabs

# Xem logs
docker-compose logs webhook
```

### GitHub Actions fail

- Kiểm tra Docker Hub credentials
- Xem logs trong Actions tab
- Verify Dockerfile syntax

### Application không start

```bash
# Xem logs chi tiết
docker logs airlabs-app

# Kiểm tra environment variables
docker inspect airlabs-app | grep -A 20 Env

# Pull image manual
docker pull hienminh1332004/airlabs-realtime-flight:latest
```

### Port conflicts

```bash
# Tìm process đang dùng port
sudo lsof -i :8080
sudo lsof -i :9000

# Kill process
sudo kill -9 PID
```

## 📝 Files Structure

```
webhook/
├── hooks.json              # Webhook configuration
├── Dockerfile              # Webhook container
├── docker-compose.yml      # Docker Compose config
├── server-setup.sh         # Auto setup script
├── README.md               # This file
└── scripts/
    └── deploy.sh          # Deploy script
```

## 🎯 Workflow Summary

1. **Developer pushes code** → GitHub
2. **GitHub Actions** → Build Docker image → Push to Docker Hub
3. **GitHub Webhook** → Trigger server endpoint
4. **Webhook service** → Execute deploy.sh
5. **Deploy script** → Pull new image → Restart container
6. **Application** → Running with latest code

## 💡 Tips

- Luôn kiểm tra logs sau mỗi deployment
- Backup database trước khi deploy
- Test webhook với ping trước
- Sử dụng environment variables cho sensitive data
- Monitor resource usage (CPU, Memory, Disk)

## 📞 Support

Nếu gặp vấn đề, kiểm tra:
1. Logs của webhook service
2. Logs của application
3. GitHub Actions logs
4. Webhook delivery status trên GitHub
