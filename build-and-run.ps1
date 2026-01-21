#!/usr/bin/env pwsh
# Build React app và chạy Spring Boot

Write-Host "🔨 Building React frontend..." -ForegroundColor Cyan
npm run build

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Frontend build successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🚀 Starting Spring Boot..." -ForegroundColor Cyan
    ./mvnw spring-boot:run
} else {
    Write-Host "❌ Frontend build failed!" -ForegroundColor Red
    exit 1
}
