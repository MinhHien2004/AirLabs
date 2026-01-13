# Flights API CRUD Test Script
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TEST FLIGHTS API - CRUD OPERATIONS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Kiem tra server
Write-Host "[1] Kiem tra server..." -ForegroundColor Yellow
try {
    $healthCheck = Test-NetConnection -ComputerName localhost -Port 8080 -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($healthCheck) {
        Write-Host "    Server dang chay tren port 8080" -ForegroundColor Green
    } else {
        Write-Host "    Server KHONG chay!" -ForegroundColor Red
        exit
    }
} catch {
    Write-Host "    Loi khi kiem tra server!" -ForegroundColor Red
    exit
}

Write-Host ""

# Test 2: GET all flights
Write-Host "[2] GET /api/flights (Lay danh sach)" -ForegroundColor Yellow
try {
    $flights = Invoke-RestMethod -Uri "http://localhost:8080/api/flights" -Method GET
    Write-Host "    SUCCESS! So luong: $($flights.Count)" -ForegroundColor Green
    if ($flights.Count -gt 0) {
        Write-Host "    Mau: $($flights[0].airlineIata) - $($flights[0].depIata) -> $($flights[0].arrIata)" -ForegroundColor Cyan
    }
} catch {
    Write-Host "    FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 3: POST - Them flight moi
Write-Host "[3] POST /api/flights (Them moi)" -ForegroundColor Yellow
$newFlight = @{
    airline_iata = "TA"
    airline_icao = "TEST"
    flight_iata = "TA123"
    flight_number = "123"
    dep_iata = "HAN"
    dep_time = "2026-01-10T10:00:00"
    arr_iata = "SGN"
    arr_time = "2026-01-10T12:15:00"
    status = "scheduled"
    duration = 135
} | ConvertTo-Json

try {
    $created = Invoke-RestMethod -Uri "http://localhost:8080/api/flights" `
        -Method POST -ContentType "application/json" -Body $newFlight
    
    Write-Host "    SUCCESS! ID: $($created.id)" -ForegroundColor Green
    Write-Host "    Airline: $($created.airlineIata), Route: $($created.depIata)->$($created.arrIata)" -ForegroundColor Cyan
    $global:testId = $created.id
} catch {
    Write-Host "    FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 4: GET by ID
if ($global:testId) {
    Write-Host "[4] GET /api/flights/$($global:testId) (Lay theo ID)" -ForegroundColor Yellow
    try {
        $flight = Invoke-RestMethod -Uri "http://localhost:8080/api/flights/$($global:testId)" -Method GET
        Write-Host "    SUCCESS! $($flight.airlineIata) - $($flight.flightIata)" -ForegroundColor Green
    } catch {
        Write-Host "    FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# Test 5: PUT - Cap nhat
if ($global:testId) {
    Write-Host "[5] PUT /api/flights/$($global:testId) (Cap nhat)" -ForegroundColor Yellow
    $updateData = @{
        airline_iata = "UA"
        airline_icao = "UPDT"
        flight_iata = "UA999"
        flight_number = "999"
        dep_iata = "HAN"
        dep_time = "2026-01-10T14:00:00"
        arr_iata = "DAD"
        arr_time = "2026-01-10T15:30:00"
        status = "scheduled"
        duration = 90
    } | ConvertTo-Json
    
    try {
        $updated = Invoke-RestMethod -Uri "http://localhost:8080/api/flights/$($global:testId)" `
            -Method PUT -ContentType "application/json" -Body $updateData
        Write-Host "    SUCCESS! New airline: $($updated.airlineIata), Route: $($updated.depIata)->$($updated.arrIata)" -ForegroundColor Green
    } catch {
        Write-Host "    FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# Test 6: DELETE
if ($global:testId) {
    Write-Host "[6] DELETE /api/flights/$($global:testId) (Xoa)" -ForegroundColor Yellow
    try {
        Invoke-RestMethod -Uri "http://localhost:8080/api/flights/$($global:testId)" -Method DELETE
        Write-Host "    SUCCESS! Flight da xoa" -ForegroundColor Green
        
        # Verify
        try {
            Invoke-RestMethod -Uri "http://localhost:8080/api/flights/$($global:testId)" -Method GET
            Write-Host "    WARNING: Flight van ton tai!" -ForegroundColor Yellow
        } catch {
            Write-Host "    Verified: 404 - Flight khong ton tai" -ForegroundColor Green
        }
    } catch {
        Write-Host "    FAILED: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  HOAN THANH TEST CRUD" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
