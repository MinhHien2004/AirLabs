# Flight Info Application - React + Spring Boot

Ứng dụng hiển thị thông tin chuyến bay sử dụng **React + Vite** cho frontend và **Spring Boot** cho backend.

## 🏗️ Cấu trúc Project

```
demo/
├── frontend/              # React source code
│   ├── App.tsx           # Main React component
│   ├── index.tsx         # Entry point
│   ├── Scheduled.tsx     # Flight schedule component
│   └── FlightsInfo.tsx   # Flight info component
├── src/main/
│   ├── java/             # Spring Boot backend
│   │   └── Task/demo/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── entity/
│   │       └── config/
│   └── resources/
│       ├── application.yaml
│       └── static/       # Build output của React (auto-generated)
├── index.html            # HTML template cho Vite
├── vite.config.ts        # Vite configuration
├── package.json          # Node dependencies
└── pom.xml              # Maven configuration
```

## 🚀 Development

### Yêu cầu

- Node.js 18+
- Java 17+
- PostgreSQL
- Redis (optional, có thể dùng Redis Cloud)

### Cài đặt

1. **Cài đặt frontend dependencies:**
```bash
npm install
```

2. **Build React app:**
```bash
npm run build
```

3. **Chạy Spring Boot:**
```bash
./mvnw spring-boot:run
```

### Development Mode

**Chạy frontend (dev mode với hot reload):**
```bash
npm run dev
```
Frontend sẽ chạy tại: http://localhost:3000

**Chạy backend:**
```bash
./mvnw spring-boot:run
```
Backend API tại: http://localhost:8080

### Production Build

**Build tất cả và chạy:**
```bash
.\build-and-run.ps1
```

Hoặc thủ công:
```bash
npm run build
./mvnw spring-boot:run
```

## 🐳 Docker

### Build Docker image:
```bash
docker build -t airlabs-app .
```

### Chạy container:
```bash
docker run -d -p 8080:8080 airlabs-app
```

### Push lên Docker Hub:
```bash
docker tag airlabs-app <username>/airlabs-app:latest
docker push <username>/airlabs-app:latest
```

## 📝 API Endpoints

- `GET /api/flights` - Lấy danh sách chuyến bay
- `GET /api/scheduled` - Lấy lịch bay theo schedule
- `GET /health` - Health check

## ⚙️ Configuration

Cấu hình trong `src/main/resources/application.yaml`:
- Database connection (PostgreSQL)
- Redis configuration
- Server port và các settings khác

## 🔧 Troubleshooting

### Giao diện không hiển thị

1. Build lại frontend: `npm run build`
2. Kiểm tra file đã được tạo trong `src/main/resources/static/`
3. Restart Spring Boot

### Hot reload không hoạt động

- Chạy `npm run dev` để development mode với Vite
- API calls sẽ được proxy tới `localhost:8080`

## 📦 Build Output

Sau khi chạy `npm run build`, Vite sẽ tạo:
- `src/main/resources/static/index.html` - HTML file
- `src/main/resources/static/assets/` - JS và CSS bundles

Spring Boot sẽ tự động serve các file này từ classpath.
