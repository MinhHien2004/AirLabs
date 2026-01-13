# Hướng dẫn Deploy lên Render

## 1. Chuẩn bị

### 1.1. Tạo tài khoản Render
- Truy cập: https://render.com
- Đăng ký/Đăng nhập bằng GitHub

### 1.2. Push code lên GitHub (Đã xong ✅)
```bash
git add .
git commit -m "Add Docker configuration"
git push origin main
```

## 2. Tạo PostgreSQL Database trên Render

1. Vào Render Dashboard → New → PostgreSQL
2. Điền thông tin:
   - **Name**: `airlabs-db` (hoặc tên bạn muốn)
   - **Database**: `postgres`
   - **User**: `postgres`
   - **Region**: Singapore (gần VN nhất)
   - **Plan**: Free
3. Click **Create Database**
4. Sau khi tạo xong, copy **Internal Database URL** (dạng: `postgresql://...`)

## 3. Tạo Redis Database trên Render

1. Vào Render Dashboard → New → Redis
2. Điền thông tin:
   - **Name**: `airlabs-redis`
   - **Region**: Singapore
   - **Plan**: Free (nếu có)
3. Click **Create Redis**
4. Copy **Internal Redis URL**

## 4. Deploy Spring Boot App

### 4.1. Tạo Web Service
1. Vào Render Dashboard → New → Web Service
2. Chọn repository: `MinhHien2004/AirLabs`
3. Điền thông tin:
   - **Name**: `airlabs-api` (hoặc tên bạn muốn)
   - **Region**: Singapore
   - **Branch**: `main`
   - **Root Directory**: (để trống)
   - **Runtime**: Docker
   - **Plan**: Free

### 4.2. Cấu hình Environment Variables
Trong phần **Environment**, thêm các biến sau:

```bash
# Database Configuration
SPRING_DATASOURCE_URL=<Internal Database URL từ bước 2>
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<password từ Render PostgreSQL>

# Redis Configuration  
SPRING_REDIS_HOST=<Internal Redis Host từ bước 3>
SPRING_REDIS_PORT=6379

# JPA Configuration (optional)
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_JPA_SHOW_SQL=false

# API Key (nếu cần)
AIRLABS_API_BASE_URL=https://airlabs.co/api/v9
AIRLABS_API_API_KEY=1ffd3d4c-1ea9-4b6c-8f0e-2250ad010506
```

### 4.3. Deploy
1. Click **Create Web Service**
2. Đợi Render build và deploy (5-10 phút)
3. Sau khi deploy xong, bạn sẽ có URL: `https://airlabs-api.onrender.com`

## 5. Test API

Sau khi deploy xong, test API:

```bash
# Health check (nếu có endpoint)
curl https://airlabs-api.onrender.com/actuator/health

# Test API của bạn
curl https://airlabs-api.onrender.com/api/flights
```

## 6. Lưu ý quan trọng

### 6.1. Free Plan Limitations
- App sẽ **sleep** sau 15 phút không hoạt động
- Request đầu tiên sau khi sleep sẽ mất 30-60s để wake up
- Database free có giới hạn 1GB

### 6.2. Tránh sleep (Optional)
Dùng dịch vụ ping như **UptimeRobot** hoặc **Cron-job.org** để ping app mỗi 10-14 phút.

### 6.3. Logs
Xem logs tại: Dashboard → Your Service → Logs

## 7. Test local với Docker

Trước khi deploy, test local:

```bash
# Build và run tất cả services
docker-compose up -d

# Xem logs
docker-compose logs -f app

# Test API
curl http://localhost:8080/api/flights

# Stop services
docker-compose down
```

## 8. Cập nhật code

Mỗi khi có thay đổi:

```bash
git add .
git commit -m "Update code"
git push origin main
```

Render sẽ tự động detect và deploy lại.

## 9. Troubleshooting

### App không start
- Check logs tại Render Dashboard
- Verify environment variables
- Check database connection

### Database connection failed
- Dùng **Internal URL** thay vì External URL
- Check username/password

### Redis connection failed
- Check Redis host và port
- Ensure Redis instance đang running

## 10. Nâng cấp (Paid Plan)

Nếu cần:
- **Starter Plan** ($7/month): No sleep, more resources
- **Standard Plan** ($25/month): Auto-scaling, better performance
