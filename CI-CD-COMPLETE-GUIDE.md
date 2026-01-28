# 📚 Hướng Dẫn Hoàn Chỉnh CI/CD với GitHub Actions

**Tài liệu đầy đủ về CI/CD, Self-hosted Runner, Docker và GitHub Actions**

---

## Mục Lục

- [PHẦN 1: LÝ THUYẾT CƠ BẢN](#phần-1-lý-thuyết-cơ-bản)
- [PHẦN 2: SETUP TỪ ĐẦU](#phần-2-setup-từ-đầu)
- [PHẦN 3: WORKFLOW SYNTAX](#phần-3-workflow-syntax-giải-thích)
- [PHẦN 4: THỰC HÀNH STEP-BY-STEP](#phần-4-thực-hành-step-by-step)
- [PHẦN 5: TROUBLESHOOTING](#phần-5-troubleshooting)
- [PHẦN 6: BEST PRACTICES](#phần-6-best-practices)
- [PHẦN 7: TEMPLATES](#phần-7-templates-tái-sử-dụng)
- [PHẦN 8: CHECKLIST](#phần-8-checklist-triển-khai)
- [PHẦN 9: TÀI LIỆU](#phần-9-tài-liệu-tham-khảo)

---

## PHẦN 1: LÝ THUYẾT CƠ BẢN

### 1.1. CI/CD là gì?

**CI (Continuous Integration):**
- Tự động build & test code mỗi khi có thay đổi
- Phát hiện lỗi sớm
- Đảm bảo code luôn có thể build thành công

**CD (Continuous Deployment/Delivery):**
- Tự động deploy lên server
- Giảm thời gian release
- Deploy nhất quán, ít lỗi

**Workflow:**
```
Developer → Push Code → GitHub
                          ↓
                    GitHub Actions
                          ↓
            Build → Test → Deploy
                          ↓
                       Server
```

---

### 1.2. GitHub Actions là gì?

**Định nghĩa:**
- CI/CD platform của GitHub
- Chạy automated workflows khi có events (push, PR, merge...)
- Miễn phí cho public repos

**Các thành phần:**

```
Workflow (file .yml)
├── Trigger (on: push, pull_request...)
├── Jobs (build, test, deploy...)
│   ├── Runner (máy chạy workflow)
│   └── Steps (các bước thực thi)
│       ├── Action (sử dụng actions có sẵn)
│       └── Run (chạy command)
```

**Ví dụ:**
```yaml
name: My Workflow
on: [push]              # Trigger
jobs:
  build:                # Job
    runs-on: ubuntu-latest  # Runner
    steps:              # Steps
      - uses: actions/checkout@v4  # Action
      - run: echo "Hello"          # Command
```

---

### 1.3. Self-hosted Runner là gì?

**GitHub-hosted runner:**
- Máy do GitHub cung cấp
- Cấu hình cố định (2 CPU, 7GB RAM)
- Mỗi job chạy trên máy mới (clean state)
- ❌ Không truy cập được network nội bộ
- ❌ Giới hạn 2000 phút/tháng (free)

**Self-hosted runner:**
- Máy của bạn (server, laptop, PC...)
- ✅ Tự định cấu hình
- ✅ Truy cập được network nội bộ
- ✅ Không giới hạn thời gian chạy
- ✅ Deploy trực tiếp trên server
- ⚠️ Phải tự maintain

**So sánh:**
```
GitHub-hosted:
Push → GitHub → Runner (cloud) → Build → Push Docker Hub
                                           ↓
Server ← Pull image ← Docker Hub

Self-hosted:
Push → GitHub → Runner (on server) → Build → Deploy local
                                              (no pull needed)
```

---

### 1.4. Docker trong CI/CD

**Docker là gì:**
- Platform để đóng gói app thành containers
- Container = app + dependencies + OS libraries
- Chạy được ở bất kỳ đâu có Docker

**Docker workflow:**
```
Code → Dockerfile → Build image → Push registry → Pull & Run
```

**Docker trong CI/CD:**
```yaml
Build step:
  docker build -t myapp:v1.0 .
  docker push myapp:v1.0

Deploy step:
  docker pull myapp:v1.0
  docker run myapp:v1.0
```

**Docker Hub:**
- Registry lưu trữ images
- Public/Private repositories
- Tương tự GitHub nhưng cho Docker images

---

### 1.5. Workflow Branching Strategy

**Chiến lược 3 nhánh:**

```
main (development)
  ↓ merge
QA (staging/testing)
  ↓ merge
Production (live)
```

**Luồng:**
1. Dev code ở `main`
2. Merge `main → QA` → Deploy QA → Test
3. Nếu OK → Merge `QA → Production` → Deploy Production

**Lợi ích:**
- ✅ Test kỹ trước khi lên Production
- ✅ Rollback dễ dàng
- ✅ Môi trường giống Production

---

## PHẦN 2: SETUP TỪ ĐẦU

### 2.1. Chuẩn bị Server (AWS EC2)

**Yêu cầu:**
- Ubuntu 20.04/22.04
- Tối thiểu 2GB RAM, 2 CPU
- Port 8080, 8081, 9000 mở
- Public IP

**Cài đặt Docker:**
```bash
# SSH vào server
ssh ubuntu@<SERVER_IP>

# Update system
sudo apt update && sudo apt upgrade -y

# Cài Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Add user vào docker group
sudo usermod -aG docker ubuntu

# Logout và login lại để apply
exit
ssh ubuntu@<SERVER_IP>

# Verify
docker --version
docker ps
```

---

### 2.2. Cài Self-hosted Runner trên Server

**Bước 1: Tạo runner trên GitHub**

1. Vào repo → **Settings** → **Actions** → **Runners**
2. Click **New self-hosted runner**
3. Chọn **Linux** và **x64**
4. GitHub sẽ hiện commands

**Bước 2: Download & Configure runner**

```bash
# SSH vào server
ssh ubuntu@<SERVER_IP>

# Tạo thư mục
mkdir actions-runner && cd actions-runner

# Download runner (copy từ GitHub)
curl -o actions-runner-linux-x64-2.311.0.tar.gz -L \
  https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz

# Extract
tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz

# Configure (copy token từ GitHub)
./config.sh --url https://github.com/YOUR_USERNAME/YOUR_REPO \
            --token YOUR_TOKEN \
            --name server-runner \
            --labels self-hosted,linux,x64

# Khi hỏi "Enter name of work folder", nhấn Enter (default: _work)
```

**Bước 3: Cài runner như service (auto-start)**

```bash
# Install service
sudo ./svc.sh install

# Start service
sudo ./svc.sh start

# Check status
sudo ./svc.sh status

# View logs
sudo journalctl -u actions.runner.* -f
```

**Verify:**
- Vào GitHub → Settings → Actions → Runners
- Sẽ thấy runner status: **Idle** (màu xanh)

---

### 2.3. Setup Environment Variables trên Server

**Cách 1: System-wide (khuyến nghị)**

```bash
# Edit /etc/environment
sudo nano /etc/environment

# Thêm các dòng:
REDIS_HOST="your-redis-cloud.com"
REDIS_PORT="6379"
REDIS_PASSWORD="your-redis-password"
AIRLABS_API_KEY="your-airlabs-api-key"

# Save (Ctrl+O, Enter, Ctrl+X)

# Reload
source /etc/environment

# Verify
echo $REDIS_HOST
```

**Cách 2: User-specific**

```bash
# Edit ~/.bashrc
nano ~/.bashrc

# Thêm vào cuối file:
export REDIS_HOST="your-redis-cloud.com"
export REDIS_PORT="6379"
export REDIS_PASSWORD="your-redis-password"
export AIRLABS_API_KEY="your-airlabs-api-key"

# Reload
source ~/.bashrc
```

**Cách 3: GitHub Secrets (bảo mật nhất)**

1. Vào repo → **Settings** → **Secrets and variables** → **Actions**
2. Click **New repository secret**
3. Thêm từng secret:
   - `REDIS_HOST`
   - `REDIS_PORT`
   - `REDIS_PASSWORD`
   - `AIRLABS_API_KEY`
   - `DOCKERHUB_USERNAME`
   - `DOCKERHUB_ACCESS_TOKEN`

---

### 2.4. Tạo Dockerfile

**Mục đích:** Đóng gói app thành Docker image

```dockerfile
# Stage 1: Build frontend
FROM node:18-alpine AS frontend-build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY src/main/java/Task/demo/frontend ./src/main/java/Task/demo/frontend
COPY tsconfig.json vite.config.ts tailwind.config.cjs postcss.config.js ./
RUN npm run build

# Stage 2: Build backend
FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline
COPY src ./src
COPY --from=frontend-build /app/dist ./src/main/resources/static
RUN mvn clean package -DskipTests

# Stage 3: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar

# Environment variables (có thể override khi run)
ENV SPRING_PROFILES_ACTIVE=production
ENV REDIS_HOST=localhost
ENV REDIS_PORT=6379

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Giải thích:**
- **Multi-stage build:** Giảm size image (chỉ giữ runtime, bỏ build tools)
- **Stage 1:** Build frontend (React/Vite)
- **Stage 2:** Build backend (Maven/Spring Boot)
- **Stage 3:** Chỉ giữ JRE + app.jar

---

### 2.5. Tạo Workflow File

**Mục đích:** Định nghĩa CI/CD pipeline

```yaml
# .github/workflows/deploy.yaml

name: Deploy Application

# Trigger: Chỉ khi merge PR vào QA hoặc Production
on: 
  pull_request:
    types: [closed]
    branches:
      - QA
      - Production

jobs:
  deploy:
    # Chỉ chạy khi PR thực sự được merge (không phải close)
    if: github.event.pull_request.merged == true
    
    # Chạy trên self-hosted runner
    runs-on: self-hosted
    
    steps:
      # 1. Checkout code
      - name: Checkout repository
        uses: actions/checkout@v4

      # 2. Xác định environment dựa vào branch
      - name: Set environment variables
        id: env
        run: |
          if [ "${{ github.base_ref }}" == "QA" ]; then
            echo "environment=qa" >> $GITHUB_OUTPUT
            echo "port=8081" >> $GITHUB_OUTPUT
            echo "tag_prefix=qa" >> $GITHUB_OUTPUT
          elif [ "${{ github.base_ref }}" == "Production" ]; then
            echo "environment=production" >> $GITHUB_OUTPUT
            echo "port=8080" >> $GITHUB_OUTPUT
            echo "tag_prefix=prod" >> $GITHUB_OUTPUT
          fi

      # 3. Login Docker Hub
      - name: Login Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_ACCESS_TOKEN }}

      # 4-7. Build steps (chỉ cho QA)
      - name: Setup Docker Buildx
        if: github.base_ref == 'QA'
        uses: docker/setup-buildx-action@v3
        id: buildx

      - name: Cache Docker layers
        if: github.base_ref == 'QA'
        uses: actions/cache@v4
        with:
          path: /tmp/.buildx-cache
          key: ${{ runner.os }}-buildx-${{ github.sha }}
          restore-keys: |
            ${{ runner.os }}-buildx-

      - name: Build and Push Docker Image
        if: github.base_ref == 'QA'
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          load: true
          tags: |
            yourname/app:${{ steps.env.outputs.tag_prefix }}-v0.0.${{ github.run_number }}
            yourname/app:${{ steps.env.outputs.tag_prefix }}-latest
          cache-from: type=local,src=/tmp/.buildx-cache
          cache-to: type=local,dest=/tmp/.buildx-cache

      # 8. Pull image (QA pull image vừa build, Prod pull qa-latest)
      - name: Pull Docker image
        run: |
          if [ "${{ github.base_ref }}" == "QA" ]; then
            docker pull yourname/app:qa-v0.0.${{ github.run_number }}
          else
            docker pull yourname/app:qa-latest
          fi

      # 9. Deploy container
      - name: Deploy container
        env:
          IMAGE: ${{ github.base_ref == 'QA' && format('yourname/app:qa-v0.0.{0}', github.run_number) || 'yourname/app:qa-latest' }}
          ENVIRONMENT: ${{ steps.env.outputs.environment }}
          PORT: ${{ steps.env.outputs.port }}
          CONTAINER_NAME: app-${{ steps.env.outputs.environment }}
        run: |
          # Stop old container
          docker stop $CONTAINER_NAME 2>/dev/null || true
          docker rm $CONTAINER_NAME 2>/dev/null || true
          
          # Run new container
          docker run -d \
            --name $CONTAINER_NAME \
            --restart unless-stopped \
            -p $PORT:8080 \
            -e SPRING_PROFILES_ACTIVE=$ENVIRONMENT \
            -e REDIS_HOST=${{ secrets.REDIS_HOST }} \
            -e REDIS_PORT=${{ secrets.REDIS_PORT }} \
            -e REDIS_PASSWORD=${{ secrets.REDIS_PASSWORD }} \
            -e AIRLABS_API_KEY=${{ secrets.AIRLABS_API_KEY }} \
            $IMAGE
          
          sleep 5
          
          # Verify
          if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
            echo "✅ Deploy successful"
            docker ps | grep $CONTAINER_NAME
          else
            echo "❌ Deploy failed"
            docker logs $CONTAINER_NAME
            exit 1
          fi

      # 10. Health check
      - name: Health check
        run: |
          MAX_RETRIES=10
          for i in $(seq 1 $MAX_RETRIES); do
            HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
              http://localhost:${{ steps.env.outputs.port }}/actuator/health || echo "000")
            
            if [ "$HTTP_CODE" -eq 200 ]; then
              echo "✅ Health check passed"
              exit 0
            fi
            
            echo "⏳ Retry $i/$MAX_RETRIES - Status: $HTTP_CODE"
            sleep 3
          done
          
          echo "❌ Health check failed"
          exit 1
```

---

## PHẦN 3: WORKFLOW SYNTAX GIẢI THÍCH

### 3.1. Trigger Events

```yaml
on:
  push:
    branches: [main]          # Khi push lên main
  
  pull_request:
    types: [opened, closed]   # Khi mở hoặc đóng PR
    branches: [QA]            # PR merge VÀO nhánh QA
  
  workflow_dispatch:          # Manual trigger (button trên GitHub)
  
  schedule:
    - cron: '0 0 * * *'       # Chạy hàng ngày lúc 00:00
```

**GitHub context variables:**
```yaml
${{ github.ref }}           # refs/heads/main
${{ github.base_ref }}      # QA (nhánh đích của PR)
${{ github.head_ref }}      # main (nhánh nguồn của PR)
${{ github.sha }}           # commit hash
${{ github.run_number }}    # số thứ tự workflow run
${{ github.event.pull_request.merged }}  # true/false
```

---

### 3.2. Jobs & Steps

```yaml
jobs:
  build:
    runs-on: ubuntu-latest    # Hoặc self-hosted
    timeout-minutes: 30       # Timeout
    
    steps:
      - name: Step name
        uses: actions/checkout@v4    # Sử dụng action có sẵn
      
      - name: Run command
        run: echo "Hello"            # Chạy command
      
      - name: Multi-line
        run: |
          echo "Line 1"
          echo "Line 2"
      
      - name: With environment
        env:
          MY_VAR: value
        run: echo $MY_VAR
      
      - name: Conditional step
        if: github.ref == 'refs/heads/main'
        run: echo "Only on main"
```

---

### 3.3. Outputs & Inputs

```yaml
jobs:
  job1:
    steps:
      - name: Set output
        id: step1
        run: echo "myvar=hello" >> $GITHUB_OUTPUT
      
      - name: Use output
        run: echo ${{ steps.step1.outputs.myvar }}
  
  job2:
    needs: job1    # Chạy sau job1
```

---

### 3.4. Secrets & Variables

```yaml
steps:
  - name: Use secret
    env:
      API_KEY: ${{ secrets.API_KEY }}
    run: curl -H "Authorization: Bearer $API_KEY" ...
```

**Repository secrets:**
- Settings → Secrets and variables → Actions
- Encrypted, không hiện trong logs
- Dùng cho: API keys, passwords, tokens

---

### 3.5. Caching

```yaml
- name: Cache dependencies
  uses: actions/cache@v4
  with:
    path: ~/.m2/repository    # Thư mục cache
    key: maven-${{ hashFiles('**/pom.xml') }}  # Key dựa trên file
    restore-keys: |
      maven-
```

**Lợi ích:**
- Giảm thời gian build (không download dependencies lại)
- Tiết kiệm bandwidth

---

## PHẦN 4: THỰC HÀNH STEP-BY-STEP

### 4.1. Setup Project từ đầu

**Bước 1: Clone repo**
```powershell
git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git
cd YOUR_REPO
```

**Bước 2: Tạo cấu trúc nhánh**
```powershell
# Tạo nhánh QA
git checkout -b QA
git push origin QA

# Tạo nhánh Production
git checkout -b Production
git push origin Production

# Về main
git checkout main
```

**Bước 3: Tạo Dockerfile**
```powershell
# Tạo file Dockerfile ở root project
notepad Dockerfile
# Paste nội dung ở phần 2.4
```

**Bước 4: Tạo workflow**
```powershell
# Tạo thư mục
mkdir -p .github/workflows

# Tạo file workflow
notepad .github/workflows/deploy.yaml
# Paste nội dung ở phần 2.5
```

**Bước 5: Commit & push**
```powershell
git add .
git commit -m "Add CI/CD pipeline"
git push origin main
```

---

### 4.2. Deploy lần đầu

**Bước 1: Tạo PR main → QA**
1. Vào GitHub repo
2. Click **Pull requests** → **New pull request**
3. Base: `QA` ← Compare: `main`
4. Click **Create pull request**
5. Review → Click **Merge pull request**

**Bước 2: Xem workflow chạy**
1. Vào tab **Actions**
2. Click vào workflow run
3. Xem logs real-time

**Bước 3: Verify deployment**
```bash
# SSH vào server
ssh ubuntu@<SERVER_IP>

# Check container
docker ps | grep app-qa

# Check logs
docker logs -f app-qa

# Test
curl http://localhost:8081/actuator/health
```

**Bước 4: Test trên browser**
```
http://<SERVER_IP>:8081
```

---

### 4.3. Deploy lên Production

**Sau khi test QA OK:**

1. Tạo PR: QA → Production
2. Merge PR
3. Workflow tự động:
   - Pull image `qa-latest`
   - Deploy lên port 8080
   - Health check

4. Verify:
```
http://<SERVER_IP>:8080
```

---

### 4.4. Rollback nếu có lỗi

**Cách 1: Rollback bằng Docker**
```bash
# List images
docker images | grep app

# Run image cũ
docker stop app-production
docker rm app-production
docker run -d --name app-production -p 8080:8080 \
  yourname/app:qa-v0.0.42  # version cũ
```

**Cách 2: Revert commit trên GitHub**
1. Vào **Commits**
2. Click vào commit lỗi → **Revert**
3. Tạo PR revert → Merge
4. Workflow tự động deploy lại

---

## PHẦN 5: TROUBLESHOOTING

### 5.1. Workflow không chạy

**Kiểm tra:**
```yaml
# File phải đặt đúng vị trí
.github/workflows/deploy.yaml

# Syntax phải đúng
# Dùng YAML validator: https://www.yamllint.com/

# Check trigger
on:
  pull_request:
    types: [closed]
    branches: [QA, Production]

# Check condition
if: github.event.pull_request.merged == true
```

**Debug:**
1. Vào Actions → Workflow → Click vào run
2. Xem logs chi tiết
3. Xem "Set up job" để biết runner nào chạy

---

### 5.2. Runner offline

```bash
# SSH vào server
ssh ubuntu@<SERVER_IP>

# Check service
sudo systemctl status actions.runner.*

# Restart
sudo systemctl restart actions.runner.*

# View logs
sudo journalctl -u actions.runner.* -f

# Nếu lỗi, remove và register lại
cd ~/actions-runner
sudo ./svc.sh stop
sudo ./svc.sh uninstall
./config.sh remove
./config.sh --url ... --token ...
sudo ./svc.sh install
sudo ./svc.sh start
```

---

### 5.3. Docker build fail

```bash
# Test build local
docker build -t test .

# Check logs
docker build -t test . --progress=plain --no-cache

# Check Dockerfile syntax
# Check có file .dockerignore chưa
```

---

### 5.4. Container không start

```bash
# Check logs
docker logs app-qa

# Check ports
netstat -tlnp | grep 8081

# Check environment variables
docker inspect app-qa | grep -A 20 Env

# Test run manually
docker run -it --rm \
  -p 8081:8080 \
  -e REDIS_HOST=... \
  yourname/app:qa-latest
```

---

### 5.5. Health check fail

```bash
# Check app logs
docker logs app-qa

# Test endpoint manually
curl http://localhost:8081/actuator/health

# Check Spring Boot application.yaml
# Check Redis connection
# Check API keys
```

---

## PHẦN 6: BEST PRACTICES

### 6.1. Security

✅ **DO:**
- Dùng GitHub Secrets cho sensitive data
- Không commit passwords/API keys vào code
- Dùng `.env` file cho local development (add vào `.gitignore`)
- Scan images trước khi deploy: `docker scan yourname/app:latest`

❌ **DON'T:**
- Hard-code credentials trong Dockerfile
- Expose sensitive ports publicly
- Run containers as root user

---

### 6.2. Performance

✅ **DO:**
- Dùng multi-stage build để giảm size image
- Cache dependencies (Maven, npm)
- Dùng `.dockerignore` để exclude files không cần
- Clean up old images/containers định kỳ

```dockerfile
# .dockerignore
node_modules
target
.git
*.log
```

---

### 6.3. Monitoring & Logging

```bash
# Setup log rotation
docker run -d \
  --log-driver json-file \
  --log-opt max-size=10m \
  --log-opt max-file=3 \
  ...

# Monitor resources
docker stats app-qa app-production

# Setup alerts (ví dụ với cron)
*/5 * * * * curl http://localhost:8080/actuator/health || echo "App down!" | mail -s "Alert" admin@example.com
```

---

### 6.4. Versioning

**Semantic Versioning:**
```
v1.2.3
│ │ │
│ │ └─ Patch (bug fixes)
│ └─── Minor (new features, backward compatible)
└───── Major (breaking changes)
```

**Trong workflow:**
```yaml
# Option 1: Dùng git tags
- name: Get version
  run: echo "VERSION=$(git describe --tags)" >> $GITHUB_ENV

# Option 2: Dùng run number
tags: |
  app:v1.0.${{ github.run_number }}

# Option 3: Dùng package.json/pom.xml
- name: Get version
  run: echo "VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)" >> $GITHUB_ENV
```

---

## PHẦN 7: TEMPLATES TÁI SỬ DỤNG

### 7.1. Template cho Spring Boot App

Sử dụng template ở phần 2.5

### 7.2. Template cho Node.js App

```yaml
name: Deploy Node.js App

on:
  pull_request:
    types: [closed]
    branches: [QA, Production]

jobs:
  deploy:
    if: github.event.pull_request.merged == true
    runs-on: self-hosted
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Build
        run: npm run build
      
      - name: Deploy
        run: |
          pm2 stop app || true
          pm2 start dist/index.js --name app
          pm2 save
```

---

### 7.3. Template cho Docker Compose

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    image: yourname/app:${TAG:-latest}
    ports:
      - "${PORT:-8080}:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=${ENV:-production}
      - REDIS_HOST=${REDIS_HOST}
    restart: unless-stopped
    
  redis:
    image: redis:7-alpine
    restart: unless-stopped
```

**Workflow:**
```yaml
- name: Deploy with docker-compose
  env:
    TAG: qa-v0.0.${{ github.run_number }}
    PORT: 8081
    ENV: qa
  run: |
    docker-compose down
    docker-compose up -d
```

---

## PHẦN 8: CHECKLIST TRIỂN KHAI

### 8.1. Trước khi bắt đầu

- [ ] Server đã cài Docker
- [ ] Server có public IP và ports mở
- [ ] GitHub repo đã tạo
- [ ] Có Dockerfile
- [ ] Có workflow file
- [ ] Đã tạo GitHub Secrets

### 8.2. Setup lần đầu

- [ ] Cài self-hosted runner trên server
- [ ] Runner status: **Idle** trên GitHub
- [ ] Test runner: push code → workflow chạy
- [ ] Tạo nhánh QA và Production
- [ ] Set environment variables trên server

### 8.3. Mỗi lần deploy

- [ ] Code đã commit và push lên main
- [ ] Tạo PR main → QA
- [ ] Review code
- [ ] Merge PR
- [ ] Workflow chạy thành công
- [ ] Test trên QA
- [ ] Tạo PR QA → Production
- [ ] Merge PR
- [ ] Verify Production

### 8.4. Sau khi deploy

- [ ] Check container đang chạy
- [ ] Check logs không có error
- [ ] Test các endpoints chính
- [ ] Monitor resources (CPU, RAM, disk)
- [ ] Cleanup images/containers cũ

---

## PHẦN 9: TÀI LIỆU THAM KHẢO

### 9.1. Official Docs

- **GitHub Actions:** https://docs.github.com/en/actions
- **Docker:** https://docs.docker.com/
- **Self-hosted runners:** https://docs.github.com/en/actions/hosting-your-own-runners

### 9.2. Useful Actions

- `actions/checkout@v4` - Clone repo
- `docker/login-action@v3` - Login Docker Hub
- `docker/build-push-action@v5` - Build & push image
- `actions/cache@v4` - Cache dependencies
- `actions/upload-artifact@v4` - Upload build artifacts

### 9.3. Commands Cheatsheet

```bash
# Docker
docker ps                          # List running containers
docker ps -a                       # List all containers
docker images                      # List images
docker logs <container>            # View logs
docker exec -it <container> bash   # Enter container
docker system prune -a             # Clean up everything

# Git
git branch                         # List branches
git checkout <branch>              # Switch branch
git merge <branch>                 # Merge branch
git log --oneline                  # View commits
git revert <commit>                # Revert commit

# Runner
sudo systemctl status actions.runner.*
sudo journalctl -u actions.runner.* -f
cd ~/actions-runner && ./run.sh    # Test runner manually
```

---

## TÓM TẮT TOÀN BỘ QUY TRÌNH

```
1. SETUP (làm 1 lần):
   ├── Chuẩn bị server (Docker + Runner)
   ├── Tạo Dockerfile
   ├── Tạo workflow file
   ├── Tạo branches (QA, Production)
   └── Setup secrets

2. DEVELOPMENT:
   ├── Code ở local
   ├── Commit & push lên main
   └── Push không trigger workflow ✅

3. DEPLOY QA:
   ├── Tạo PR: main → QA
   ├── Merge PR
   ├── Workflow tự động:
   │   ├── Build image
   │   ├── Push Docker Hub
   │   ├── Deploy QA (port 8081)
   │   └── Health check
   └── Test trên QA

4. DEPLOY PRODUCTION:
   ├── Tạo PR: QA → Production
   ├── Merge PR
   ├── Workflow tự động:
   │   ├── Pull image từ QA
   │   ├── Deploy Production (port 8080)
   │   └── Health check
   └── Verify Production

5. ROLLBACK (nếu cần):
   ├── Option 1: Deploy image cũ
   ├── Option 2: Revert commit
   └── Option 3: Manual rollback
```

---

## DIAGRAM TỔNG QUAN

```
┌─────────────────────────────────────────────────────────────┐
│                      DEVELOPER                               │
│                                                              │
│  Code → Commit → Push to main                               │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                      GITHUB                                  │
│                                                              │
│  Create PR: main → QA → Merge                               │
│           │                                                  │
│           └─→ Trigger GitHub Actions Workflow               │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              SELF-HOSTED RUNNER (on Server)                 │
│                                                              │
│  ┌────────────┐   ┌────────────┐   ┌────────────┐         │
│  │   Build    │ → │    Push    │ → │   Deploy   │         │
│  │   Docker   │   │   Docker   │   │  Container │         │
│  │   Image    │   │    Hub     │   │   QA:8081  │         │
│  └────────────┘   └────────────┘   └────────────┘         │
│                                                              │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    TEST ON QA                                │
│                                                              │
│  http://server-ip:8081                                       │
│  Test OK? → Create PR: QA → Production → Merge              │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              SELF-HOSTED RUNNER (on Server)                 │
│                                                              │
│  ┌────────────┐   ┌────────────┐                           │
│  │    Pull    │ → │   Deploy   │                           │
│  │  qa-latest │   │  Container │                           │
│  │   Image    │   │  Prod:8080 │                           │
│  └────────────┘   └────────────┘                           │
│                                                              │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   PRODUCTION LIVE                            │
│                                                              │
│  http://server-ip:8080                                       │
│  ✅ App running successfully!                               │
└─────────────────────────────────────────────────────────────┘
```

---

**🎉 Chúc bạn thành công với CI/CD workflow!**

*Tài liệu này được tạo bởi GitHub Copilot - Version 1.0 - January 2026*
