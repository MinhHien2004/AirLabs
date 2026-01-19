# Webhook Auto-Deploy Setup

Tự động deploy ứng dụng khi push code lên GitHub main branch.

## 📋 Yêu cầu

- Docker & Docker Compose đã cài trên remote machine
- GitHub repository đã setup GitHub Actions
- Docker Hub account

## 🚀 Cài đặt trên Remote Machine

### 1. Copy folder webhook lên server

```bash
# Trên máy local (Windows PowerShell)
scp -i "C:\Users\LENOVO\Downloads\FPT-Key-1.pem" -r webhook/ ubuntu@184.73.67.179:/home/ubuntu/

# SSH vào server
ssh -i "C:\Users\LENOVO\Downloads\FPT-Key-1.pem" ubuntu@184.73.67.179
cd /home/ubuntu/webhook
```

**Hoặc clone trực tiếp từ GitHub (Đơn giản hơn):**

```bash
# SSH vào server
ssh -i "C:\Users\LENOVO\Downloads\FPT-Key-1.pem" ubuntu@184.73.67.179

# Clone repository
git clone https://github.com/MinhHien2004/AirLabs.git
cd AirLabs/webhook
```

### 2. Cấu hình Webhook Secret

```bash
# Tạo secret ngẫu nhiên
openssl rand -base64 32

# Cập nhật trong hooks.json
nano hooks.json
# Thay YOUR_WEBHOOK_SECRET_HERE bằng secret vừa tạo
```

### 3. Chạy Webhook Service

```bash
# Build và start webhook listener
docker-compose up -d

# Kiểm tra logs
docker-compose logs -f
```

### 4. Kiểm tra webhook đang chạy

```bash
# Test endpoint
curl http://localhost:9000/hooks/deploy-airlabs

# Xem logs
docker logs webhook-listener -f
```

## ⚙️ Cấu hình GitHub Webhook

1. Vào GitHub Repository → **Settings** → **Webhooks** → **Add webhook**

2. Điền thông tin:
   - **Payload URL**: `http://YOUR_SERVER_IP:9000/hooks/deploy-airlabs`
   - **Content type**: `application/json`
   - **Secret**: [Secret đã tạo ở bước 2]
   - **Events**: Chọn "Just the push event"
   - **Active**: ✓

3. Click **Add webhook**

## 🔧 Cấu hình Firewall (nếu cần)

```bash
# Cho phép port 9000
sudo ufw allow 9000/tcp

# Hoặc chỉ cho phép từ GitHub IPs
# https://api.github.com/meta
```

## 🔒 Sử dụng HTTPS (Khuyến nghị)

Nếu muốn dùng HTTPS, setup nginx reverse proxy:

```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location /hooks/ {
        proxy_pass http://localhost:9000/hooks/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 📊 Workflow

1. Developer push code lên `main` branch
2. GitHub Actions build Docker image → push lên Docker Hub
3. GitHub gửi webhook notification đến server
4. Webhook listener trigger script `deploy.sh`
5. Script pull image mới và restart container
6. Application tự động cập nhật!

## 🛠️ Troubleshooting

```bash
# Kiểm tra webhook container
docker ps | grep webhook

# Xem logs
docker logs webhook-listener -f

# Restart webhook service
docker-compose restart

# Test deploy script manually
docker exec webhook-listener /scripts/deploy.sh "manual test"

# Xem logs của app container
docker logs airlabs-app -f
```

## 🔄 Cập nhật Webhook Configuration

```bash
# Sau khi sửa hooks.json hoặc scripts
docker-compose restart
```

## 📝 Notes

- Webhook secret phải giống nhau giữa GitHub và `hooks.json`
- Port 9000 phải accessible từ internet (hoặc GitHub IPs)
- Script `deploy.sh` cần quyền truy cập Docker socket
- Logs được lưu tự động bởi Docker
