# 🚀 Hướng dẫn Setup Webhook Auto-Deploy

## Bước 1: Cấu hình trên Remote Machine (Server)

### 1.1. SSH vào server

```powershell
# Thay đổi theo thông tin server của bạn
ssh -i "path/to/your-key.pem" ubuntu@YOUR_SERVER_IP
```

### 1.2. Cài đặt Docker & Docker Compose (nếu chưa có)

```bash
# Update system
sudo apt update
sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add user to docker group
sudo usermod -aG docker $USER

# Install Docker Compose
sudo apt install docker-compose -y

# Verify installation
docker --version
docker-compose --version
```

### 1.3. Clone repository hoặc copy webhook folder

**Option 1: Clone từ GitHub (Khuyến nghị)**
```bash
# Clone repository
git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git
cd YOUR_REPO/AirLabs/webhook
```

**Option 2: Copy từ máy local**
```powershell
# Trên Windows PowerShell
scp -i "path/to/your-key.pem" -r AirLabs/webhook ubuntu@YOUR_SERVER_IP:/home/ubuntu/
```

### 1.4. Tạo Webhook Secret

```bash
# Tạo secret ngẫu nhiên (copy kết quả này)
openssl rand -base64 32
```

### 1.5. Cập nhật hooks.json

```bash
cd /home/ubuntu/webhook  # hoặc đường dẫn tương ứng
nano hooks.json
```

Thay `YOUR_WEBHOOK_SECRET_HERE` bằng secret vừa tạo:

```json
{
  "secret": "PASTE_YOUR_SECRET_HERE"
}
```

### 1.6. Cấp quyền thực thi cho deploy script

```bash
chmod +x scripts/deploy.sh
```

### 1.7. Start Webhook Service

```bash
# Start webhook listener
docker-compose up -d

# Kiểm tra logs
docker-compose logs -f

# Kiểm tra webhook đang chạy
curl http://localhost:9000/hooks/deploy-airlabs
```

### 1.8. Cấu hình Firewall

```bash
# Cho phép port 9000 (webhook) và 8080 (app)
sudo ufw allow 9000/tcp
sudo ufw allow 8080/tcp
sudo ufw enable
sudo ufw status
```

## Bước 2: Xác thực Remote Machine với GitHub

### 2.1. Tạo SSH Key trên server (nếu cần pull private repo)

```bash
# Tạo SSH key
ssh-keygen -t ed25519 -C "your_email@example.com"

# Hiển thị public key
cat ~/.ssh/id_ed25519.pub
```

### 2.2. Thêm SSH Key vào GitHub

1. Copy nội dung public key
2. Vào GitHub: **Settings** → **SSH and GPG keys** → **New SSH key**
3. Paste public key và save

### 2.3. Test SSH connection

```bash
ssh -T git@github.com
# Kết quả: "Hi username! You've successfully authenticated..."
```

### 2.4. Cấu hình Git (nếu cần)

```bash
git config --global user.name "Your Name"
git config --global user.email "your_email@example.com"
```

## Bước 3: Cấu hình GitHub Webhook

### 3.1. Vào GitHub Repository Settings

1. Mở repository trên GitHub
2. **Settings** → **Webhooks** → **Add webhook**

### 3.2. Điền thông tin Webhook

```
Payload URL: http://YOUR_SERVER_IP:9000/hooks/deploy-airlabs
Content type: application/json
Secret: [Paste secret đã tạo ở Bước 1.4]
SSL verification: Enable (nếu có HTTPS) hoặc Disable
Which events: Just the push event
Active: ✓ (checked)
```

### 3.3. Save webhook

Click **Add webhook** → GitHub sẽ gửi 1 ping test

## Bước 4: Cấu hình GitHub Secrets (cho GitHub Actions)

### 4.1. Vào Repository Settings → Secrets

**Settings** → **Secrets and variables** → **Actions** → **New repository secret**

### 4.2. Thêm các secrets sau:

```
DOCKERHUB_USERNAME: your_dockerhub_username
DOCKERHUB_ACCESS_TOKEN: your_dockerhub_token
```

### 4.3. Tạo Docker Hub Access Token

1. Đăng nhập [Docker Hub](https://hub.docker.com/)
2. **Account Settings** → **Security** → **New Access Token**
3. Đặt tên: `github-actions`
4. Permissions: **Read & Write**
5. Copy token (chỉ hiển thị 1 lần)
6. Paste vào GitHub Secret `DOCKERHUB_ACCESS_TOKEN`

## Bước 5: Test Workflow

### 5.1. Push code để trigger workflow

```bash
# Trên máy local
git add .
git commit -m "Test webhook deployment"
git push origin main
```

### 5.2. Theo dõi quá trình

**Trên GitHub:**
- **Actions** tab → Xem workflow đang chạy
- **Settings** → **Webhooks** → Xem webhook delivery

**Trên Server:**
```bash
# Xem logs webhook
docker-compose logs -f

# Xem logs app deployment
docker logs airlabs-app -f

# Kiểm tra container đang chạy
docker ps
```

### 5.3. Test application

```bash
# Trên server
curl http://localhost:8080/actuator/health

# Từ bên ngoài
curl http://YOUR_SERVER_IP:8080/actuator/health
```

## 🔧 Troubleshooting

### Webhook không trigger

```bash
# Kiểm tra webhook service
docker-compose ps
docker-compose logs webhook

# Test endpoint
curl -X POST http://localhost:9000/hooks/deploy-airlabs
```

### Container không start

```bash
# Xem logs chi tiết
docker logs airlabs-app

# Kiểm tra image đã pull chưa
docker images | grep airlabs

# Pull manual
docker pull hienminh1332004/airlabs-realtime-flight:latest
```

### Port đã bị sử dụng

```bash
# Tìm process đang dùng port 8080
sudo lsof -i :8080
sudo netstat -tulpn | grep 8080

# Kill process
sudo kill -9 PID
```

### GitHub Actions fail

- Kiểm tra Docker Hub credentials trong GitHub Secrets
- Xem logs trong Actions tab
- Đảm bảo Dockerfile đúng format

## 📊 Monitoring

### Xem status real-time

```bash
# Container status
watch docker ps

# Application logs
docker logs -f airlabs-app

# Webhook logs
docker-compose -f /home/ubuntu/webhook/docker-compose.yml logs -f
```

### Restart services

```bash
# Restart webhook
cd /home/ubuntu/webhook
docker-compose restart

# Restart application
docker restart airlabs-app
```

## 🔒 Security Best Practices

1. **Luôn dùng HTTPS** cho webhook URL (setup nginx reverse proxy)
2. **Giữ secret an toàn**, không commit vào git
3. **Limit IP access** cho webhook port nếu có thể
4. **Update Docker images** thường xuyên
5. **Backup** cấu hình và data

## 📝 Notes

- Workflow hoàn chỉnh: GitHub Push → Actions build → Docker Hub → Webhook trigger → Auto deploy
- Mỗi lần push code lên main branch sẽ tự động deploy
- Kiểm tra logs thường xuyên để phát hiện lỗi sớm
