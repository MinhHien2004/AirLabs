# Server Deployment Guide

## Tổng quan
File `build.yaml` giờ chỉ build và push image lên DockerHub. **Deployment được thực hiện trên server EC2 của bạn** để bảo mật thông tin.

## Workflow

### Khi merge PR vào QA:
1. GitHub Actions build image mới
2. Push lên DockerHub với tag `qa-latest`
3. **Bạn SSH vào server và chạy script `deploy-qa.sh`**

### Khi merge PR vào Production:
1. GitHub Actions lấy image `qa-latest`
2. Re-tag thành `prod-latest` và push lên DockerHub
3. **Bạn SSH vào server và chạy script `deploy-production.sh`**

## Setup trên Server EC2

### 1. Tạo file environment variables
```bash
# SSH vào server
ssh -i "Hien-Key.pem" ubuntu@3.88.65.140

# Tạo file .env
sudo nano /opt/airlabs/.env
```

Nội dung file `.env`:
```bash
# Docker Hub
export DOCKERHUB_USERNAME="hienminh1332004"
export DOCKERHUB_TOKEN="your-dockerhub-token"

# Application Secrets
export REDIS_HOST="your-redis-host"
export REDIS_PORT="6379"
export REDIS_PASSWORD="your-redis-password"
export AIRLABS_API_KEY="your-airlabs-api-key"
```

### 2. Upload các script lên server
```bash
# Từ máy local
scp -i "C:\Users\LENOVO\Downloads\Hien-Key.pem" deploy-qa.sh ubuntu@3.88.65.140:/opt/airlabs/
scp -i "C:\Users\LENOVO\Downloads\Hien-Key.pem" deploy-production.sh ubuntu@3.88.65.140:/opt/airlabs/
```

### 3. Set permissions cho scripts
```bash
# Trên server
chmod +x /opt/airlabs/deploy-qa.sh
chmod +x /opt/airlabs/deploy-production.sh
chmod 600 /opt/airlabs/.env
```

## Cách deploy

### Deploy QA
```bash
# SSH vào server
ssh -i "Hien-Key.pem" ubuntu@3.88.65.140

# Load environment variables
source /opt/airlabs/.env

# Chạy deploy script
cd /opt/airlabs
./deploy-qa.sh
```

### Deploy Production
```bash
# SSH vào server
ssh -i "Hien-Key.pem" ubuntu@3.88.65.140

# Load environment variables
source /opt/airlabs/.env

# Chạy deploy script
cd /opt/airlabs
./deploy-production.sh
```

## Tự động hóa (Optional)

### Option 1: Cron job - Tự động pull image mới mỗi 5 phút
```bash
# Edit crontab
crontab -e

# Thêm dòng này
*/5 * * * * source /opt/airlabs/.env && /opt/airlabs/deploy-qa.sh >> /var/log/airlabs-deploy-qa.log 2>&1
```

### Option 2: Webhook - Deploy khi có push lên DockerHub
Sử dụng webhook từ DockerHub để trigger deploy script khi có image mới.

### Option 3: Manual - Chạy script sau mỗi lần merge PR
Cách an toàn nhất, bạn kiểm soát khi nào deploy.

## Kiểm tra logs

```bash
# Xem logs container QA
docker logs -f airlabs-app-qa

# Xem logs container Production
docker logs -f airlabs-app-production

# Kiểm tra container đang chạy
docker ps

# Health check
curl http://localhost:8081/actuator/health  # QA
curl http://localhost:8080/actuator/health  # Production
```

## Rollback

### Rollback về version trước
```bash
# List all images
docker images | grep airlabs-realtime-flight

# Stop current container
docker stop airlabs-app-qa
docker rm airlabs-app-qa

# Run old version
docker run -d \
    --name airlabs-app-qa \
    --restart unless-stopped \
    -p 8081:8080 \
    -e SPRING_PROFILES_ACTIVE=qa \
    -e REDIS_HOST="${REDIS_HOST}" \
    -e REDIS_PORT="${REDIS_PORT}" \
    -e REDIS_PASSWORD="${REDIS_PASSWORD}" \
    -e AIRLABS_API_KEY="${AIRLABS_API_KEY}" \
    hienminh1332004/airlabs-realtime-flight:v0.0.XXX
```

## Lợi ích của phương pháp này

✅ **Bảo mật**: Không để credentials trong GitHub  
✅ **Linh hoạt**: Kiểm soát được thời điểm deploy  
✅ **Đơn giản**: Chỉ cần chạy 1 script  
✅ **Rollback dễ dàng**: Có thể quay lại version cũ nhanh chóng  
✅ **Logging**: Có thể xem log deploy trên server
