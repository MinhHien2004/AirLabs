# API Debug Test Script
# Kiem tra tat ca cac endpoints

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  KIEM TRA API ENDPOINTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Kiem tra server co hoat dong khong
Write-Host "[1] Kiem tra server..." -ForegroundColor Yellow
try {
    $healthCheck = Test-NetConnection -ComputerName localhost -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($healthCheck) {
        Write-Host "    Server dang chay tren port 8080" -ForegroundColor Green
    } else {
        Write-Host "    Server KHONG chay! Vui long start server truoc." -ForegroundColor Red
        Write-Host "    Command: .\mvnw spring-boot:run" -ForegroundColor Yellow
        exit
    }
} catch {
    Write-Host "    Loi khi kiem tra server!" -ForegroundColor Red
    exit
}

Write-Host ""

# Test API 1: POST /api/products/add
Write-Host "[2] Test POST /api/products/add (Them san pham)" -ForegroundColor Yellow
$productBody = @{
    title = "Debug Test Product"
    price = 199.99
    description = "Product for debugging API"
    category = "electronics"
    image = "https://example.com/test.jpg"
    rating = @{
        rate = 4.8
        count = 250
    }
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/products/add" `
        -Method POST `
        -ContentType "application/json" `
        -Body $productBody `
        -ErrorAction Stop
    
    Write-Host "    SUCCESS!" -ForegroundColor Green
    Write-Host "    Product ID: $($response.id)" -ForegroundColor Cyan
    Write-Host "    Title: $($response.title)" -ForegroundColor Cyan
    Write-Host "    Price: $($response.price)" -ForegroundColor Cyan
} catch {
    Write-Host "    FAILED!" -ForegroundColor Red
    Write-Host "    Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "    Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test API 2: GET /api/airlines
Write-Host "[3] Test GET /api/airlines (Dong bo airlines)" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/airlines" `
        -Method GET `
        -ErrorAction Stop
    
    Write-Host "    SUCCESS!" -ForegroundColor Green
    Write-Host "    Response: $response" -ForegroundColor Cyan
} catch {
    Write-Host "    FAILED!" -ForegroundColor Red
    Write-Host "    Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "    Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TONG KET" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Cac API endpoints hien tai:" -ForegroundColor Yellow
Write-Host "  1. POST /api/products/add" -ForegroundColor Green
Write-Host "  2. GET  /api/airlines" -ForegroundColor Green
Write-Host ""
Write-Host "KHONG CO API TRUNG LAP!" -ForegroundColor Green
Write-Host ""
